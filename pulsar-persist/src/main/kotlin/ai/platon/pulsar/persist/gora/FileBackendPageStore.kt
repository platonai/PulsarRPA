package ai.platon.pulsar.persist.gora

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.avro.AvroRuntimeException
import org.apache.avro.file.DataFileReader
import org.apache.avro.file.DataFileWriter
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DatumWriter
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.gora.memory.store.MemStore
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A very simple file backend storage for webpages
 * */
class FileBackendPageStore(
    private val persistDirectory: Path = AppPaths.LOCAL_STORAGE_DIR
) : MemStore<String, GWebPage>() {

    private val logger = LoggerFactory.getLogger(FileBackendPageStore::class.java)
    private val unsafeConf = VolatileConfig()

    @Synchronized
    override fun get(reversedUrl: String, vararg fields: String): GWebPage? {
        var page = map[reversedUrl] as? GWebPage
        if (page == null) {
            page = readAvro(reversedUrl) ?: readHtml(reversedUrl)
        }
        return page
    }

    @Synchronized
    override fun put(reversedUrl: String, page: GWebPage) {
        super.put(reversedUrl, page)

        UrlUtils.unreverseUrlOrNull(reversedUrl)?.let {
            val p = WebPage.box(it, page, unsafeConf)
            writeAvro(p)
            writeHtml(p)
        }
    }

    @Synchronized
    override fun delete(reversedUrl: String): Boolean {
        var success = super.delete(reversedUrl)
        val url = UrlUtils.unreverseUrlOrNull(reversedUrl)
        if (url != null) {
            var path = getPersistPath(url, ".avro")
            success = Files.deleteIfExists(path)
            path = getPersistPath(url, ".html")
            success = Files.deleteIfExists(path)
        }

        return success
    }

    override fun getSchemaName() = "FileBackendPageStore"

    override fun getFields(): Array<String> = GWebPage._ALL_FIELDS

    @Synchronized
    fun readHtml(reversedUrl: String): GWebPage? {
        val url = UrlUtils.unreverseUrlOrNull(reversedUrl) ?: return null
        val path = getPersistPath(url, ".html")

        logger.takeIf { it.isTraceEnabled }?.trace("Getting $reversedUrl " + Files.exists(path) + " | $path")

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

    @Synchronized
    fun readAvro(reversedUrl: String): GWebPage? {
        val url = UrlUtils.unreverseUrlOrNull(reversedUrl) ?: return null
        val path = getPersistPath(url, ".avro")

        if (!Files.exists(path)) {
            return null
        }

        logger.takeIf { it.isTraceEnabled }?.trace("Getting $reversedUrl " + Files.exists(path) + " | $path")
        return try {
            readAvro(path)
        } catch (e: AvroRuntimeException) {
            logger.warn("Failed to read avro file from $path, the file might be corrupted, delete it", e)
            Files.deleteIfExists(path)
            null
        } catch (e: IOException) {
            // logger.warn(Throwable.brief())
            Files.deleteIfExists(path)
            null
        }
    }

    @Synchronized
    fun readAvro(path: Path): GWebPage? {
        if (!Files.exists(path)) {
            return null
        }

        val datumReader: DatumReader<GWebPage> = SpecificDatumReader(GWebPage::class.java)
        var page: GWebPage? = null
        val dataFileReader: DataFileReader<GWebPage> = DataFileReader(path.toFile(), datumReader)
        dataFileReader.use {
            while (it.hasNext()) {
                page = it.next(page)
            }
        }
        return page
    }

    @Synchronized
    fun writeHtml(page: WebPage) {
        val content = page.content ?: return
        val path = getPersistPath(page.url, ".htm")

        logger.takeIf { it.isTraceEnabled }?.trace("Putting ${page.content?.array()?.size} | $path")
        Files.write(path, content.array())
    }

    @Synchronized
    fun writeAvro(page: WebPage) {
        val path = getPersistPath(page.url, ".avro")

        logger.takeIf { it.isTraceEnabled }?.trace("Putting ${page.content?.array()?.size} | $path")

        Files.deleteIfExists(path)
        try {
            writeAvro0(page.unbox(), path)
        } catch (e: AvroRuntimeException) {
            logger.warn("Failed to write avro file to $path", e)
        } catch (e: IOException) {
            logger.warn(e.brief())
        }
    }

    fun getPersistPath(url: String, suffix: String): Path {
        val directory = getPersistDirectory(url)
        val filename = AppPaths.fromUri(url, "", suffix)
        return directory.resolve(filename)
    }

    private fun getPersistDirectory(url: String): Path {
        val dirForDomain = AppPaths.fromDomain(url)
        val path = persistDirectory.resolve(dirForDomain)
        Files.createDirectories(path)
        return path
    }

    @Throws(IOException::class)
    private fun writeAvro0(page: GWebPage, path: Path) {
        val datumWriter: DatumWriter<GWebPage> = SpecificDatumWriter(GWebPage::class.java)
        val dataFileWriter: DataFileWriter<GWebPage> = DataFileWriter(datumWriter)
        dataFileWriter.use {
            dataFileWriter.create(page.schema, path.toFile())
            dataFileWriter.append(page)
        }
    }

    private fun newSuccessPage(url: String, lastModified: Instant, content: ByteArray): WebPage {
        val page = WebPage.newWebPage(url, VolatileConfig.UNSAFE)
        page.also {
            it.location = url
            it.fetchCount = 1
            it.prevFetchTime = lastModified
            it.fetchInterval = ChronoUnit.DECADES.duration
            it.fetchTime = lastModified + it.fetchInterval
            it.crawlStatus = CrawlStatus.STATUS_FETCHED
            it.protocolStatus = ProtocolStatus.STATUS_SUCCESS
        }

        page.content = ByteBuffer.wrap(content)
        require(page.contentLength == content.size.toLong())
        require(page.persistedContentLength == content.size.toLong())

        return page
    }
}
