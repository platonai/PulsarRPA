package ai.platon.pulsar.persist.gora

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.memory.store.MemStore
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/**
 * A very simple file backend storage for Web pages
 * */
class FileBackendPageStore(
        private val persistDirectory: Path = AppPaths.LOCAL_STORAGE_DIR
) : MemStore<String, GWebPage>() {

    private val log = LoggerFactory.getLogger(FileBackendPageStore::class.java)
    private val unsafeConf = VolatileConfig.UNSAFE

    override fun get(reversedUrl: String, vararg fields: String): GWebPage? {
        val page = map[reversedUrl] as? GWebPage ?: read(reversedUrl)
        // return getPersistent(page, getFieldsToQuery(fields))
        return page
    }

    override fun put(reversedUrl: String, page: GWebPage) {
        super.put(reversedUrl, page)

        Urls.unreverseUrlOrNull(reversedUrl)?.let {
            write(WebPage.box(it, page, unsafeConf))
        }
    }

    override fun getSchemaName() = "FileBackendPageStore"

    override fun getFields(): Array<String> = GWebPage._ALL_FIELDS

    private fun read(reversedUrl: String): GWebPage? {
        val url = Urls.unreverseUrlOrNull(reversedUrl) ?: return null
        val filename = AppPaths.fromUri(url, "", ".htm")
        val path = persistDirectory.resolve(filename)

        log.takeIf { it.isTraceEnabled }?.trace("Getting $reversedUrl $filename " + Files.exists(path))

        if (Files.exists(path)) {
            val content = Files.readAllBytes(path)
            // val lastModified = Files.getLastModifiedTime(path).toInstant()
            // never expire, so it serves as a mock site
            val lastModified = Instant.now()
            val page = newSuccessPage(url, lastModified, content)
            return page.unbox()
        }

        return null
    }

    private fun write(page: WebPage) {
        val filename = AppPaths.fromUri(page.url, "", ".htm")
        val path = persistDirectory.resolve(filename)

        log.takeIf { it.isTraceEnabled }?.trace("Putting $filename ${page.content?.array()?.size}")

        page.content?.let { Files.write(path, it.array()) }
    }

    private fun newSuccessPage(url: String, lastModified: Instant, content: ByteArray): WebPage {
        val page = WebPage.newWebPage(url, VolatileConfig.UNSAFE).apply {
            location = url
            fetchCount = 1
            fetchTime = lastModified
            prevFetchTime = fetchTime
            crawlStatus = CrawlStatus.STATUS_FETCHED
            protocolStatus = ProtocolStatus.STATUS_SUCCESS
        }

        page.content = ByteBuffer.wrap(content)

        return page
    }
}
