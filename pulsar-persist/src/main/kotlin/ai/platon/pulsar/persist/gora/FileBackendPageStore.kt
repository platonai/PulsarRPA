package ai.platon.pulsar.persist.gora

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.impl.GoraBackendWebPage
import org.apache.avro.AvroRuntimeException
import org.apache.avro.file.DataFileReader
import org.apache.avro.file.DataFileWriter
import org.apache.avro.io.DatumReader
import org.apache.avro.io.DatumWriter
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.gora.memory.store.MemStore
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

    private val logger = getLogger(this)
    private val tracer get() = logger.takeIf { it.isTraceEnabled }
    private val unsafeConf = VolatileConfig()

    /**
     * Get a page from the store.
     * */
    @Synchronized
    override fun get(reversedUrl: String, vararg fields: String): GWebPage? {
        var page = map[reversedUrl] as? GWebPage
        if (page == null) {
            page = readAvro(reversedUrl) ?: readHtml(reversedUrl)
        }
        return page
    }

    /**
     * Put a page into the store.
     * */
    @Synchronized
    override fun put(reversedUrl: String, page: GWebPage) {
        super.put(reversedUrl, page)

        UrlUtils.unreverseUrlOrNull(reversedUrl)?.let {
            val p = GoraBackendWebPage.box(it, page, unsafeConf)
            writeAvro(p)
            writeHtml(p)
        }
    }
    /**
     * Delete a page from the store.
     *
     * This function deletes a page identified by its reversed URL from the store. It first attempts to delete the page
     * using the superclass's delete method. If the URL can be successfully unreversed, it also deletes the associated
     * `.avro` and `.html` files from the file system.
     *
     * @param reversedUrl The reversed URL of the page to be deleted.
     * @return `true` if the page and its associated files were successfully deleted, `false` otherwise.
     */
    @Synchronized
    override fun delete(reversedUrl: String): Boolean {
        // Attempt to delete the page using the superclass's delete method.
        var success = super.delete(reversedUrl)

        // Unreverse the URL to get the original URL.
        val url = UrlUtils.unreverseUrlOrNull(reversedUrl)
        if (url != null) {
            // Get the paths for the associated `.avro` and `.html` files.
            val path1 = getPersistPath(url, ".avro")
            val path2 = getPersistPath(url, ".html")

            // Delete the `.avro` and `.html` files if they exist.
            try {
                val filesDeleted = Files.deleteIfExists(path1) && Files.deleteIfExists(path2)
                success = success && filesDeleted
            } catch (e: IOException) {
                // Log the exception or handle it appropriately
                logger.warn("Failed to delete files for $reversedUrl", e)
                success = false
            }
        }

        // Return the overall success status of the deletion operation.
        return success
    }

    override fun getSchemaName() = "FileBackendPageStore"

    override fun getFields(): Array<String> = GWebPage._ALL_FIELDS

    @Synchronized
    fun readHtml(reversedUrl: String): GWebPage? {
        val url = UrlUtils.unreverseUrlOrNull(reversedUrl) ?: return null
        val path = getPersistPath(url, ".html")

        tracer?.trace("Getting {} {} | {}", reversedUrl, Files.exists(path), path)

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

        tracer?.trace("Getting {} {} | {}", reversedUrl, Files.exists(path), path)
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

        logger.takeIf { it.isTraceEnabled }?.trace("Putting {} | {}", page.content?.array()?.size, path)
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
        val dirForDomain = AppPaths.fromHost(url)
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
        val page = GoraBackendWebPage.newWebPage(url, VolatileConfig.UNSAFE)
        page.also {
            it.location = url
            it.fetchCount = 1
            it.prevFetchTime = lastModified
            it.fetchInterval = ChronoUnit.DECADES.duration
            it.fetchTime = lastModified + it.fetchInterval
            it.protocolStatus = ProtocolStatus.STATUS_SUCCESS
        }

        page.content = ByteBuffer.wrap(content)
        require(page.contentLength == content.size.toLong())
        require(page.persistedContentLength == content.size.toLong())

        return page
    }
}
