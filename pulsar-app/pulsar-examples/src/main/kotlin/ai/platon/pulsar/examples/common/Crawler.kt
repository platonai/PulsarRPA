package ai.platon.pulsar.examples.common

import ai.platon.pulsar.common.NetUtil
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.urls.Urls
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.crawl.WebPageBatchHandler
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import com.google.common.collect.Iterables
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.zip.Deflater

class BeforeWebPageBatchHandler: WebPageBatchHandler() {
    override fun invoke(pages: Iterable<WebPage>) {
        val size = Iterables.size(pages)
        println("Before fetching - $size pages")
    }
}

class AfterWebPageBatchHandler: WebPageBatchHandler() {
    override fun invoke(pages: Iterable<WebPage>) {
        val size = Iterables.size(pages)
        val length = pages.joinToString { Strings.readableBytes(it.aveContentBytes) }
        val lengthAfterCompress = pages.asSequence()
                .map { it.content?.array()?:"".toByteArray() }
                .joinToString { Strings.readableBytes(compress(it).second) }
        println("After fetching - Fetched $size pages. Length: $length\tCompressed: $lengthAfterCompress")
    }

    fun compress(input: ByteArray): Pair<ByteArray, Int> {
        val output = ByteArray(input.size)
        val compressor = Deflater()
        compressor.setInput(input)
        compressor.finish()
        val compressedDataLength = compressor.deflate(output)
        return output to compressedDataLength
    }
}

@Deprecated("Use VerboseCrawler instead")
open class Crawler(
        val context: PulsarContext,
        private var beforeBatchHandler: WebPageBatchHandler = BeforeWebPageBatchHandler(),
        private var afterBatchHandler: WebPageBatchHandler = AfterWebPageBatchHandler()
) {
    private val logger = LoggerFactory.getLogger(Crawler::class.java)

    val i = context.createSession()

    fun load(url: String, args: String) = load(url, i.options(args))

    fun load(url: String, options: LoadOptions): Pair<WebPage, FeaturedDocument> {
        val page = i.load(url, options)
        val doc = i.parse(page)
        doc.absoluteLinks()
        doc.stripScripts()

        if (options.correctedOutLinkSelector.isNotBlank()) {
            doc.select(options.correctedOutLinkSelector) { it.attr("abs:href") }.asSequence()
                    .filter { Urls.isValidUrl(it) }
                    .mapTo(HashSet()) { it.substringBefore(".com") }
                    .asSequence()
                    .filter { !it.isBlank() }
                    .mapTo(HashSet()) { "$it.com" }
                    .filter { NetUtil.testHttpNetwork(URL(it)) }
                    .take(10)
                    .joinToString("\n") { it }
                    .also { println(it) }
        }

        val path = i.export(doc)
        logger.info("Export to: file://{}", path)

        return page to doc
    }

    fun loadOutPages(portalUrl: String, args: String) = loadOutPages(portalUrl, i.options(args))

    fun loadOutPages(portalUrl: String, options: LoadOptions) {
        val page = i.load(portalUrl, options)

        val document = i.parse(page)
        document.absoluteLinks()
        document.stripScripts()
        val path = i.export(document)
        logger.info("Portal page is exported to: file://$path")

        if (options.correctedOutLinkSelector.isBlank()) {
            return
        }

        val links = document.select(options.correctedOutLinkSelector) { it.attr("abs:href") }
                .mapNotNullTo(mutableSetOf()) { i.normalizeOrNull(it)?.spec }
                .take(options.topLinks)
        logger.info("Total ${links.size} items to load")

        // TODO: use CrawlEventHandler
//        i.sessionConfig.putBean(FETCH_BEFORE_FETCH_BATCH_HANDLER, beforeBatchHandler)
//        i.sessionConfig.putBean(FETCH_AFTER_FETCH_BATCH_HANDLER, afterBatchHandler)

        val pages = i.loadAll(links, options.createItemOptions())
        pages.forEach {
            println(it.url)
        }
    }

    fun loadAllNews(portalUrl: String, options: LoadOptions) {
        val portal = i.load(portalUrl, options)
        val links = portal.simpleLiveLinks.filter { it.contains("jinrong") }
        val pages = i.loadAll(links, i.options("--parse"))
        pages.forEach { println("${it.url} ${it.contentTitle}") }
    }

    fun extractAds() {
        val url = "https://wuhan.baixing.com/xianhualipin/a1100414743.html"
        val doc = i.loadDocument(url)
        doc.select("a[href~=mssp.baidu]").map {  }
    }

    fun scan(baseUri: String) {
        i.context.scan(baseUri).forEachRemaining {
            val size = it.content?.array()?.size?:0
            println(size)
        }
    }
}
