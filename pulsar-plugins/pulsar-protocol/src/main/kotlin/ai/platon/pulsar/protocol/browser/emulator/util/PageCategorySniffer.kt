package ai.platon.pulsar.protocol.browser.emulator.util

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.metadata.OpenPageCategory
import ai.platon.pulsar.persist.metadata.PageCategory
import java.util.concurrent.CopyOnWriteArrayList


interface PageCategorySniffer {
    operator fun invoke(pageDatum: PageDatum): OpenPageCategory
}

open class DefaultPageCategorySniffer(val conf: ImmutableConfig): PageCategorySniffer {
    override fun invoke(pageDatum: PageDatum): OpenPageCategory {
        return OpenPageCategory(PageCategory.UNKNOWN)
    }
}

class ChainedPageCategorySniffer(val conf: ImmutableConfig): PageCategorySniffer {
    private val sniffers = CopyOnWriteArrayList<PageCategorySniffer>()

    override fun invoke(pageDatum: PageDatum): OpenPageCategory {
        for (sniffer in sniffers) {
            val category = sniffer(pageDatum)
            if (category.toPageCategory() != PageCategory.UNKNOWN) {
                return category
            }
        }

        return OpenPageCategory(PageCategory.UNKNOWN)
    }

    fun addFirst(sniffer: PageCategorySniffer) {
        sniffers.add(0, sniffer)
    }

    fun addLast(sniffer: PageCategorySniffer) {
        sniffers.add(sniffer)
    }

    fun remove(sniffer: PageCategorySniffer) {
        sniffers.remove(sniffer)
    }
}
