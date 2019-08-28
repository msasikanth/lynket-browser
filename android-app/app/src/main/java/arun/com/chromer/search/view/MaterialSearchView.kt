/*
 * Lynket
 *
 * Copyright (C) 2019 Arunkumar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package arun.com.chromer.search.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent.EXTRA_RESULTS
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import arun.com.chromer.R
import arun.com.chromer.di.view.ViewComponent
import arun.com.chromer.search.suggestion.SuggestionAdapter
import arun.com.chromer.search.suggestion.items.SuggestionItem
import arun.com.chromer.shared.Constants.REQUEST_CODE_VOICE
import arun.com.chromer.shared.base.ProvidesActivityComponent
import arun.com.chromer.util.Utils
import arun.com.chromer.util.Utils.getSearchUrl
import arun.com.chromer.util.recyclerview.onChanges
import butterknife.BindColor
import butterknife.ButterKnife
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxbinding3.view.focusChanges
import com.jakewharton.rxbinding3.widget.afterTextChangeEvents
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import dev.arunkumar.android.rxschedulers.SchedulerProvider
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.widget_material_search_view.view.*
import javax.inject.Inject

@SuppressLint("CheckResult")
class MaterialSearchView
@JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @BindColor(R.color.accent_icon_no_focus)
    @JvmField
    var normalColor = 0
    @BindColor(R.color.accent)
    @JvmField
    var focusedColor = 0

    private var viewComponent: ViewComponent? = null

    private val xIcon: IconicsDrawable by lazy {
        IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_close)
                .color(normalColor)
                .sizeDp(16)
    }
    private val voiceIcon: IconicsDrawable  by lazy {
        IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_microphone)
                .color(normalColor)
                .sizeDp(18)
    }
    private val menuIcon: IconicsDrawable  by lazy {
        IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_menu)
                .color(normalColor)
                .sizeDp(18)
    }

    private val suggestionAdapter by lazy { SuggestionAdapter(context) }

    @Inject
    lateinit var searchPresenter: SearchPresenter
    @Inject
    lateinit var schedulerProvider: SchedulerProvider

    private val voiceSearchFailed = PublishSubject.create<Any>()
    private val searchPerforms = PublishSubject.create<String>()
    private val focusChanges = PublishSubject.create<Boolean>()

    val text: String get() = if (msvEditText.text == null) "" else msvEditText?.text.toString()

    val url: String get() = getSearchUrl(text)

    val editText: EditText get() = msvEditText

    init {
        if (context is ProvidesActivityComponent) {
            viewComponent = context
                    .activityComponent
                    .viewComponentFactory().create(this)
                    .also { component -> component.inject(this) }
        }

        addView(LayoutInflater.from(context).inflate(
                R.layout.widget_material_search_view,
                this,
                false
        ))
        ButterKnife.bind(this)

        searchSuggestions.apply {
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
            adapter = suggestionAdapter.apply {
                onChanges {
                    searchSuggestions.isGone = itemCount == 0
                }
            }
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        msvEditText.setOnClickListener { performClick() }
        msvEditText.focusChanges()
                .takeUntil(detaches())
                .subscribe { hasFocus ->
                    if (hasFocus) {
                        gainFocus()
                    } else {
                        loseFocus()
                    }
                }
        msvEditText.editorActionEvents { it.actionId == IME_ACTION_SEARCH }
                .map { url }
                .takeUntil(detaches())
                .subscribe(::searchPerformed)

        msvEditText.afterTextChangeEvents()
                .takeUntil(detaches())
                .subscribe {
                    handleVoiceIconState()
                }

        msvLeftIcon.setImageDrawable(menuIcon)


        msvRightIcon.run {
            setImageDrawable(voiceIcon)
            setOnClickListener {
                if (text.isNotEmpty()) {
                    msvEditText?.setText("")
                    clearFocus()
                } else {
                    if (Utils.isVoiceRecognizerPresent(context)) {
                        (context as Activity).startActivityForResult(
                                Utils.getRecognizerIntent(context),
                                REQUEST_CODE_VOICE
                        )
                    } else {
                        voiceSearchFailed.onNext(Any())
                    }
                }
            }
        }

        setOnClickListener { if (!msvEditText!!.hasFocus()) gainFocus() }

        suggestionAdapter.clicks()
                .doOnNext {
                    searchPerformed(getSearchUrl(if (it is SuggestionItem.HistorySuggestionItem) it.subTitle else it.title))
                }.takeUntil(detaches())
                .subscribe()

        searchPresenter.registerSearch(
                msvEditText.textChanges()
                        .skipInitialValue()
                        .map { it.toString() }
                        .takeUntil(detaches())
        )

        searchPresenter.suggestions.observeOn(schedulerProvider.ui).subscribe(::setSuggestions)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewComponent = null
        searchPresenter.cleanUp()
    }

    override fun clearFocus() {
        clearFocus(null)
    }

    override fun hasFocus(): Boolean {
        return if (msvEditText != null) {
            msvEditText.hasFocus() && super.hasFocus()
        } else super.hasFocus()
    }

    override fun setOnClickListener(onClickListener: OnClickListener?) = Unit

    fun voiceSearchFailed(): Observable<Any> = voiceSearchFailed.hide()

    fun searchPerforms(): Observable<String> = searchPerforms.hide()

    fun focusChanges(): Observable<Boolean> = focusChanges.hide()

    fun menuClicks(): Observable<Unit> = msvLeftIcon.clicks().share()

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_VOICE) {
            when (resultCode) {
                RESULT_OK -> {
                    val resultList = data?.getStringArrayListExtra(EXTRA_RESULTS)
                    if (resultList != null && resultList.isNotEmpty()) {
                        searchPerformed(getSearchUrl(resultList[0]))
                    }
                }
            }
        }
    }

    fun gainFocus() {
        handleVoiceIconState()
        setFocusedColor()
        focusChanges.onNext(true)
    }

    fun loseFocus(endAction: (() -> Unit)? = null) {
        setNormalColor()
        msvEditText.text = null
        hideKeyboard()
        hideSuggestions()
        endAction?.invoke()
        focusChanges.onNext(false)
        handleVoiceIconState()
    }

    private fun clearFocus(endAction: (() -> Unit)?) {
        loseFocus(endAction)
        val view = findFocus()
        view?.clearFocus()
        super.clearFocus()
    }


    private fun hideKeyboard() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(windowToken, 0)
    }

    private fun setFocusedColor() {
        msvLeftIcon.setImageDrawable(menuIcon.color(focusedColor))
        msvRightIcon.setImageDrawable(voiceIcon.color(focusedColor))
    }

    private fun setNormalColor() {
        msvLeftIcon.setImageDrawable(menuIcon.color(normalColor))
        msvRightIcon.setImageDrawable(voiceIcon.color(normalColor))
    }

    private fun handleVoiceIconState() {
        if (text.isNotEmpty()) {
            msvRightIcon.setImageDrawable(xIcon.color(if (msvEditText.hasFocus()) focusedColor else normalColor))
        } else {
            msvRightIcon.setImageDrawable(voiceIcon.color(if (msvEditText.hasFocus()) focusedColor else normalColor))
        }
    }

    private fun searchPerformed(url: String) {
        clearFocus { searchPerforms.onNext(url) }
    }

    private fun hideSuggestions() = suggestionAdapter.clear()

    private fun setSuggestions(suggestionItems: List<SuggestionItem>) {
        suggestionAdapter.submitList(suggestionItems)
    }
}
