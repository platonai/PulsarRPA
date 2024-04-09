package ai.platon.pulsar.common

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils.SPACE
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.lang3.math.NumberUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.createFile
import kotlin.random.Random

object AppFiles {

    private val logger = getLogger(AppFiles::class.java)
    
    fun supportSymbolicLink(path: Path): Boolean {
        return !SystemUtils.IS_OS_WINDOWS && path.fileSystem.supportedFileAttributeViews().contains("posix")
    }
    
    /*
     * Create a symbolic link to the given target if the target platform
     * supports symbolic link; otherwise, it will create a tiny file
     * to contain the path to the target.
     */
    @Throws(IOException::class)
    fun createSymbolicLink(dstFile: Path, target: Path) {
        Files.createDirectories(dstFile.parent)
        if (supportSymbolicLink(dstFile)) {
            Files.createSymbolicLink(dstFile, target)
        } else {
            Files.newBufferedWriter(dstFile).use { writer -> writer.write(String.format("Please see %s%n", target.toString())) }
        }
    }
    
    /**
     * Create a temporary file with the given prefix and suffix.
     *
     * @param prefix The prefix string to be used in generating the file's name
     * @param suffix The suffix string to be used in generating the file's name
     * @return The temporary file
     */
    @Throws(IOException::class)
    fun createTempFile(prefix: String, suffix: String = ""): Path {
        val rand = RandomStringUtils.randomAlphanumeric(12)
        val path = AppPaths.getProcTmp("tmp", "$prefix$rand$suffix")
        // This method works as if the CREATE, TRUNCATE_EXISTING, and WRITE options are present. In other words,
        // it opens the file for writing, creating the file if it doesn't exist, or initially truncating an existing
        // regular-file to a size of 0.
        Files.createDirectories(path.parent)
        Files.writeString(path, "")
        return path
    }
    
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
            if (!Files.exists(AppPaths.PATH_LAST_GENERATED_ROWS)) {
                return -1
            }

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
            Files.writeString(path, batchId, StandardOpenOption.CREATE)
        }

        return path
    }

    fun readBatchIdOrDefault(defaultValue: String): String {
        try {
            if (!Files.exists(AppPaths.PATH_LAST_BATCH_ID)) {
                return defaultValue
            }

            return Files.readAllLines(AppPaths.PATH_LAST_BATCH_ID)[0]
        } catch (ignored: Throwable) {}

        return defaultValue
    }

    fun createSharedFileTask(url: String) {
        try {
            val path = AppPaths.WEB_CACHE_DIR.resolve(AppPaths.fromUri(url, "", ".task"))
            Files.write(path, url.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            logger.error(e.toString())
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
            logger.error(e.toString())
        }

        return null
    }

    fun logUnreachableHosts(unreachableHosts: Collection<String>) {
        val report = unreachableHosts.map { Strings.reverse(it) }.sorted()
                .map { Strings.reverse(it) }.joinToString { "\n" }

        try {
            Files.write(AppPaths.PATH_UNREACHABLE_HOSTS, report.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            logger.error(e.toString())
        }
    }
}
