package arun.com.chromer.home.epoxycontroller.model

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arun.com.chromer.R
import arun.com.chromer.extenstions.gone
import arun.com.chromer.extenstions.show
import arun.com.chromer.tabs.TabsManager
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyAttribute.Option.DoNotHash
import com.airbnb.epoxy.EpoxyModelClass
import dev.arunkumar.android.epoxy.model.KotlinEpoxyModelWithHolder
import dev.arunkumar.android.epoxy.model.KotlinHolder
import kotlinx.android.synthetic.main.layout_tabs_info_card.*

@EpoxyModelClass(layout = R.layout.layout_tabs_info_card)
abstract class TabsInfoModel : KotlinEpoxyModelWithHolder<TabsInfoModel.ViewHolder>() {
    @EpoxyAttribute
    lateinit var tabs: List<TabsManager.Tab>
    @EpoxyAttribute(DoNotHash)
    lateinit var tabsManager: TabsManager

    private var init = false

    override fun bind(holder: ViewHolder) {
        super.bind(holder)
        holder.tabsDescription.text = holder.tabsDescription.context.resources.getQuantityString(
                R.plurals.active_tabs,
                tabs.size,
                tabs.size
        )
        holder.containerView.setOnClickListener {
            tabsManager.showTabsActivity()
        }
        if (!init) {
            holder.tabsPreviewRecyclerView.apply {
                (itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
                layoutManager = LinearLayoutManager(
                        holder.containerView.context,
                        RecyclerView.HORIZONTAL,
                        false
                )
            }
            init = true
        }
        if (tabs.isEmpty()) {
            holder.tabsPreviewRecyclerView.gone()
        } else {
            holder.tabsPreviewRecyclerView.show()
            holder.tabsPreviewRecyclerView.withModels {
                tabs.forEach { tab ->
                    tab {
                        id(tab.hashCode())
                        tab(tab)
                    }
                }
            }
        }
    }

    class ViewHolder : KotlinHolder()
}