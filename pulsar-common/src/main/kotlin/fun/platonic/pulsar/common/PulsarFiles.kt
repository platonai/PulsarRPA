package `fun`.platonic.pulsar.common

import `fun`.platonic.pulsar.common.config.CapabilityTypes.*
import `fun`.platonic.pulsar.common.config.PulsarConstants.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.SPACE
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Created by vincent on 18-3-23.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
object PulsarPaths {
    val homeDir = SParser(System.getProperty(PARAM_HOME_DIR)).getPath(PULSAR_DEFAULT_TMP_DIR)
    val tmpDir = SParser(System.getProperty(PARAM_TMP_DIR)).getPath(PULSAR_DEFAULT_TMP_DIR)
    val dataDir = SParser(System.getProperty(PARAM_DATA_DIR)).getPath(PULSAR_DEFAULT_DATA_DIR)

    @JvmField val PULSAR_DEFAULT_TEST_DIR = get(tmpDir, "test")
    @JvmField val PATH_LAST_BATCH_ID = get(tmpDir, "last-batch-id")
    @JvmField val PATH_LAST_GENERATED_ROWS = get(tmpDir, "last-generated-rows")
    @JvmField val PATH_LOCAL_COMMAND = get(tmpDir, "pulsar-commands")
    @JvmField val PATH_EMERGENT_SEEDS = get(tmpDir, "emergent-seeds")
    @JvmField val PATH_BANNED_URLS = get(tmpDir, "banned-urls")
    @JvmField val PATH_UNREACHABLE_HOSTS = get(tmpDir, "unreachable-hosts.txt")

    @JvmField val PATH_PULSAR_OUTPUT_DIR = dataDir
    @JvmField val PATH_PULSAR_REPORT_DIR = get(dataDir, "report")
    @JvmField val PATH_PULSAR_CACHE_DIR = get(dataDir, "cache")

    val cacheDir = PATH_PULSAR_CACHE_DIR
    val webCacheDir = get(cacheDir, "web")

    private val homeDirStr get() = homeDir.toString()

    init {
        if (!Files.exists(tmpDir)) Files.createDirectories(tmpDir)
        if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir)
        if (!Files.exists(webCacheDir)) Files.createDirectories(webCacheDir)
    }

    fun get(baseDirectory: Path, vararg more: String): Path {
        return Paths.get(baseDirectory.toString(), *more)
    }

    fun get(first: String, vararg more: String): Path {
        return Paths.get(homeDirStr, first.removePrefix(homeDirStr), *more)
    }

    fun fromUri(uri: String, suffix: String = ""): String {
        val md5 = DigestUtils.md5Hex(uri)
        return if (suffix.isNotEmpty()) md5 + suffix else md5
    }

    fun relative(absolutePath: String): String {
        return StringUtils.substringAfter(absolutePath, homeDir.toString())
    }
}

object PulsarFiles {

    val log = LoggerFactory.getLogger(PulsarFiles::class.java)!!

    fun save(content: String, ident: String, filename: String): Path {
        val path = PulsarPaths.get(ident, filename)
        Files.deleteIfExists(path)
        Files.createDirectories(path.parent)
        Files.write(path, content.toByteArray(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
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

    @JvmOverloads
    fun saveTo(content: ByteArray, relativePath: String, deleteIfExists: Boolean = false): Path {
        return saveTo(content, PulsarPaths.get(relativePath), deleteIfExists)
    }

    fun appendlog(message: String, path: Path) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(
                    path.toFile(), DateTimeUtil.now() + SPACE + message, true)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun appendlog(message: String, file: String) {
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(
                    File(file), DateTimeUtil.now() + SPACE + message, true)
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
        val path = PulsarPaths.PATH_LAST_BATCH_ID
        Files.write(path, (rows.toString() + "\n").toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        return path
    }

    fun readLastGeneratedRows(): Int {
        try {
            val line = Files.readAllLines(PulsarPaths.PATH_LAST_GENERATED_ROWS)[0]
            return NumberUtils.toInt(line, -1)
        } catch (ignored: Throwable) {
        }

        return -1
    }

    @Throws(IOException::class)
    fun writeBatchId(batchId: String?): Path? {
        if (batchId != null && !batchId.isEmpty()) {
            val path = PulsarPaths.PATH_LAST_BATCH_ID
            Files.write(path, (batchId + "\n").toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            return path
        }

        return null
    }

    fun readBatchIdOrDefault(defaultValue: String): String {
        try {
            return Files.readAllLines(PulsarPaths.PATH_LAST_BATCH_ID)[0]
        } catch (ignored: Throwable) {
        }

        return defaultValue
    }

    /**
     * TODO: we need a better name
     */
    fun createSharedFileTask(url: String) {
        try {
            val paths = PulsarPaths
            val path = paths.get(paths.webCacheDir.toString(), paths.fromUri(url, ".task"))
            Files.write(path, url.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            log.error(e.toString())
        }

    }

    fun getCachedWebPage(url: String): String? {
        val paths = PulsarPaths
        val path = paths.get(paths.webCacheDir.toString(), paths.fromUri(url, ".htm"))
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
        val report = unreachableHosts.map { StringUtil.reverse(it) }.sorted()
                .map { StringUtil.reverse(it) }.joinToString { "\n" }

        try {
            Files.write(PulsarPaths.PATH_UNREACHABLE_HOSTS, report.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            log.error(e.toString())
        }
    }
}
