package arun.com.chromer.search.view

import android.annotation.SuppressLint
import arun.com.chromer.di.scopes.PerView
import arun.com.chromer.di.view.Detaches
import arun.com.chromer.search.provider.SearchProvider
import arun.com.chromer.search.provider.SearchProviders
import arun.com.chromer.search.suggestion.SuggestionsEngine
import arun.com.chromer.search.suggestion.items.SuggestionItem
import arun.com.chromer.search.suggestion.items.SuggestionType
import arun.com.chromer.settings.RxPreferences
import com.jakewharton.rxrelay2.PublishRelay
import dev.arunkumar.android.rxschedulers.SchedulerProvider
import io.reactivex.BackpressureStrategy.LATEST
import io.reactivex.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

@SuppressLint("CheckResult")
@PerView
class SearchPresenter
@Inject
constructor(
        private val suggestionsEngine: SuggestionsEngine,
        private val schedulerProvider: SchedulerProvider,
        @param:Detaches
        private val detaches: Observable<Unit>,
        private val searchProviders: SearchProviders,
        private val rxPreferences: RxPreferences
) {
    private val suggestionsSubject = PublishRelay.create<Pair<SuggestionType, List<SuggestionItem>>>()
    val suggestions: Observable<Pair<SuggestionType, List<SuggestionItem>>> = suggestionsSubject.hide()

    fun registerSearch(queryObservable: Observable<String>) {
        queryObservable
                .toFlowable(LATEST)
                .debounce(200, MILLISECONDS, schedulerProvider.pool)
                .doOnNext { Timber.d(it) }
                .compose(suggestionsEngine.suggestionsTransformer())
                .takeUntil(detaches.toFlowable(LATEST))
                .subscribe(suggestionsSubject::accept)
    }

    fun registerSearchProviderClicks(searchProviderClicks: Observable<SearchProvider>) {
        searchProviderClicks
                .observeOn(schedulerProvider.pool)
                .map { it.name }
                .observeOn(schedulerProvider.ui)
                .takeUntil(detaches)
                .subscribe(rxPreferences.searchEngine)
    }

    val searchEngines: Observable<List<SearchProvider>> = searchProviders
            .availableProviders
            .toObservable()
            .subscribeOn(schedulerProvider.pool)
            .share()

    val selectedSearchProvider: Observable<SearchProvider> = searchProviders.selectedProvider

    fun getSearchUrl(searchUrl: String): Observable<String> {
        return selectedSearchProvider.take(1)
                .map { provider -> provider.getSearchUrl(searchUrl) }
    }
}