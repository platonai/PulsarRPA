package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.HtmlIntegrity
import ai.platon.pulsar.common.HtmlUtils
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.persist.metadata.OpenPageCategory
import ai.platon.pulsar.persist.metadata.PageCategory
import java.util.*

interface PageCategorySniffer {
    operator fun invoke(pageDatum: PageDatum): OpenPageCategory
}

open class DefaultPageCategorySniffer(
    val conf: ImmutableConfig
): PageCategorySniffer {
    override fun invoke(pageDatum: PageDatum): OpenPageCategory {
        return OpenPageCategory(PageCategory.UNKNOWN)
    }
}

class CombinedPageCategorySniffer(
    val conf: ImmutableConfig
): PageCategorySniffer {
    val sniffers = Collections.synchronizedList(ArrayList<PageCategorySniffer>())

    override fun invoke(pageDatum: PageDatum): OpenPageCategory {
        for (sniffer in sniffers) {
            val category = sniffer(pageDatum)
            if (category.toPageCategory() != PageCategory.UNKNOWN) {
                return category
            }
        }

        return OpenPageCategory(PageCategory.UNKNOWN)
    }
}

interface HtmlIntegrityChecker {
    fun isRelevant(url: String): Boolean
    operator fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity
}

abstract class AbstractHtmlIntegrityChecker: HtmlIntegrityChecker {
    override fun isRelevant(url: String): Boolean = true
    override operator fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity = HtmlIntegrity.OK
}

open class DefaultHtmlIntegrityChecker(
    val jsEnabled: Boolean,
    val conf: ImmutableConfig
): AbstractHtmlIntegrityChecker() {
    private val tracer = getLogger(DefaultHtmlIntegrityChecker::class).takeIf { it.isTraceEnabled }

    override operator fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        return checkHtmlIntegrity(pageSource)
    }

    /**
     * Check if the html is integral before field extraction, a further html integrity checking can be
     * applied after field extraction.
     * */
    private fun checkHtmlIntegrity(pageSource: String): HtmlIntegrity {
        val length = pageSource.length.toLong()

        return when {
            length == 0L -> HtmlIntegrity.EMPTY_0B
            length == 39L -> HtmlIntegrity.EMPTY_39B
            HtmlUtils.isBlankBody(pageSource) -> HtmlIntegrity.BLANK_BODY
            else -> checkHtmlIntegrity0(pageSource)
        }
    }

    private fun checkHtmlIntegrity0(pageSource: String): HtmlIntegrity {
        val p0 = pageSource.indexOf("</head>")
        val p1 = pageSource.indexOf("<body", p0)
        if (p1 <= 0) return HtmlIntegrity.OTHER
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return HtmlIntegrity.OTHER
        // no any link, it's broken
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return HtmlIntegrity.NO_ANCHOR

        if (jsEnabled) {
            // TODO: optimize using region match
            val bodyTag = pageSource.substring(p1, p2)
            tracer?.trace("Body tag: $bodyTag")
            // The javascript set data-error flag to indicate if the vision information of all DOM nodes is calculated
            val r = bodyTag.contains("data-error=\"0\"")
            if (!r) {
                return HtmlIntegrity.NO_JS_OK_FLAG
            }
        }

        return HtmlIntegrity.OK
    }
}

open class CombinedHtmlIntegrityChecker(
    val conf: ImmutableConfig
): AbstractHtmlIntegrityChecker() {
    val checkers = Collections.synchronizedList(mutableListOf<HtmlIntegrityChecker>())

    override fun isRelevant(url: String): Boolean = checkers.any { it.isRelevant(url) }

    override fun invoke(pageSource: String, pageDatum: PageDatum): HtmlIntegrity {
        return checkers.asSequence()
            .filter { it.isRelevant(pageDatum.url) }
            .map { it.invoke(pageSource, pageDatum) }
            .firstOrNull { it != HtmlIntegrity.OK }
            ?: HtmlIntegrity.OK
    }
}
