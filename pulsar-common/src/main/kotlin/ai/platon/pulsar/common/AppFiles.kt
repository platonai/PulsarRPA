package ai.platon.pulsar.common

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils.SPACE
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object AppFiles {

    val log = LoggerFactory.getLogger(AppFiles::class.java)!!

    fun saveTo(any: Any, path: Path, deleteIfExists: Boolean = false): Path {
        return saveTo(any.toString().toByteArray(), path, deleteIfExists)
    }

    fun saveTo(content: String, path: Path, deleteIfExists: Boolean = false): Path {
        return saveTo(content.toByteArray(), path, deleteIfExists)
    }

    fun saveTo(content: ByteArray, path: Path, deleteIfExists: Boolean = false): Path {
        if (deleteIfExists) {
            Files.deleteIfExists(path)
        }

        Files.createDirectories(path.parent)
        Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE)

        return path
    }

    fun appendLog(message: String, path: Path) {
        try {
            // TODO: cached
            FileUtils.writeStringToFile(path.toFile(), DateTimes.now() + SPACE + message, true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun appendLog(message: String, file: String) {
        try {
            // TODO: cached
            FileUtils.writeStringToFile(File(file), DateTimes.now() + SPACE + message, true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * list all files in a jar file
     * TODO : use spring.ResourceUtil instead
     */
    @Throws(IOException::class)
    fun <T> listJarDirectory(clazz: Class<T>): List<String> {
        val entries = mutableListOf<String>()

        val src = clazz.protectionDomain.codeSource
        val zip = ZipInputStream(src.location.openStream())

        var entry = zip.nextEntry
        while (entry != null) {
            entries.add(entry.name)
            entry = zip.nextEntry
        }

        return entries
    }

    @Throws(IOException::class)
    fun <T> listJarDirectory(clazz: Class<T>, baseDirectory: String): List<String> {
        return listJarDirectory(clazz)
    }

    @Throws(IOException::class)
    fun <T> getJarEntries(clazz: Class<T>): List<ZipEntry> {
        val entries = mutableListOf<ZipEntry>()

        val src = clazz.protectionDomain.codeSource
        val zip = ZipInputStream(src.location.openStream())

        var entry = zip.nextEntry
        while (entry != null) {
            entries.add(entry)
            entry = zip.nextEntry
        }

        return entries
    }

    @Throws(IOException::class)
    fun writeLastGeneratedRows(rows: Long): Path {
        val path = AppPaths.PATH_LAST_GENERATED_ROWS
        Files.write(path, (rows.toString() + "\n").toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        return path
    }

    fun readLastGeneratedRows(): Int {
        try {
            val line = Files.readAllLines(AppPaths.PATH_LAST_GENERATED_ROWS)[0]
            return NumberUtils.toInt(line, -1)
        } catch (ignored: Throwable) {
        }

        return -1
    }

    @Throws(IOException::class)
    fun writeLastBatchId(batchId: String): Path {
        val path = AppPaths.PATH_LAST_BATCH_ID

        if (batchId.isNotEmpty()) {
            Files.write(path, (batchId + "\n").toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        }

        return path
    }

    fun readBatchIdOrDefault(defaultValue: String): String {
        try {
            return Files.readAllLines(AppPaths.PATH_LAST_BATCH_ID)[0]
        } catch (ignored: Throwable) {
        }

        return defaultValue
    }

    fun createSharedFileTask(url: String) {
        try {
            val path = AppPaths.WEB_CACHE_DIR.resolve(AppPaths.fromUri(url, "", ".task"))
            Files.write(path, url.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            log.error(e.toString())
        }
    }

    fun getCachedWebPage(url: String): String? {
        val path = AppPaths.WEB_CACHE_DIR.resolve(AppPaths.fromUri(url, "", ".htm"))
        if (Files.notExists(path)) {
            return null
        }

        try {
            return String(Files.readAllBytes(path))
        } catch (e: IOException) {
            log.error(e.toString())
        }

        return null
    }

    fun logUnreachableHosts(unreachableHosts: Collection<String>) {
        val report = unreachableHosts.map { Strings.reverse(it) }.sorted()
                .map { Strings.reverse(it) }.joinToString { "\n" }

        try {
            Files.write(AppPaths.PATH_UNREACHABLE_HOSTS, report.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            log.error(e.toString())
        }
    }
}
