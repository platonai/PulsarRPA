package ai.platon.pulsar.common

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.AppPaths.CONFIG_AVAILABLE_DIR
import ai.platon.pulsar.common.AppPaths.CONFIG_ENABLED_DIR
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
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.notExists
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
        val path = AppPaths.getRandomProcTmpTmpPath(prefix, suffix)
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

    fun logUnreachableHosts(unreachableHosts: Collection<String>) {
        val report = unreachableHosts.map { Strings.reverse(it) }.sorted()
                .map { Strings.reverse(it) }.joinToString { "\n" }

        try {
            Files.write(AppPaths.PATH_UNREACHABLE_HOSTS, report.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            logger.error(e.toString())
        }
    }

    @Throws(IOException::class)
    fun enableConfig(fileName: String) {
        if (CONFIG_AVAILABLE_DIR.resolve(fileName).notExists()) {
            return
        }

        if (CONFIG_ENABLED_DIR.resolve(fileName).exists()) {
            return
        }

        Files.copy(CONFIG_AVAILABLE_DIR.resolve(fileName), CONFIG_ENABLED_DIR.resolve(fileName))
    }

    @Throws(IOException::class)
    fun disableConfig(fileName: String) {
        CONFIG_ENABLED_DIR.resolve(fileName).deleteIfExists()
    }
}
