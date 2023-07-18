package ai.platon.pulsar.crawl.parse.html

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.select.selectFirstOrNull
import ai.platon.pulsar.persist.WebPage
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by vincent on 16-9-14.
 */
class JsoupParser(
        private val page: WebPage,
        private val conf: ImmutableConfig
) {
    companion object {
        val numJsoupParses = AtomicInteger()
        val numJsoupParsed = AtomicInteger()
    }

    val LOG = LoggerFactory.getLogger(JsoupParser::class.java)

    var document = FeaturedDocument.NIL
        private set

    fun parse(): FeaturedDocument {
        numJsoupParses.incrementAndGet()

        if (page.encoding == null) {
            val primerParser = PrimerParser(conf)
            primerParser.detectEncoding(page)
        }

        try {
            document = FeaturedDocument(Jsoup.parse(page.contentAsInputStream, page.encoding, page.baseUrl))
            updateMetaInfos(page, document)
            return document
        } catch (e: IOException) {
            LOG.warn("Failed to parse page {}", page.url)
            LOG.warn(e.toString())
        }

        numJsoupParsed.incrementAndGet()

        return document
    }

    private fun updateMetaInfos(page: WebPage, document: FeaturedDocument) {
        // TODO: a json variable might be better
        val selector = "#${AppConstants.PULSAR_META_INFORMATION_ID}"
        val metadata = document.document.selectFirstOrNull(selector) ?: return

        page.href?.takeIf { UrlUtils.isStandard(it) }?.let { metadata.attr("href", it) }
        page.referrer.takeIf { UrlUtils.isStandard(it) }?.let { metadata.attr("referrer", it) }

        val options = page.options

        // normUrl is deprecated, use normalizedUrl instead
        metadata.attr("normUrl", page.url)
        metadata.attr("normalizedUrl", page.url)
        metadata.attr("label", options.label)
        metadata.attr("taskId", options.taskId)
        metadata.attr("taskTime", options.taskTime.toString())
    }
}
