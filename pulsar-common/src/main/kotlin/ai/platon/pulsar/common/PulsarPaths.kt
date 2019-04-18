package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.PulsarConstants
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by vincent on 18-3-23.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
object PulsarPaths {
    val homeDir = SParser(System.getProperty(CapabilityTypes.PARAM_HOME_DIR)).getPath(PulsarConstants.PULSAR_DEFAULT_TMP_DIR)
    val tmpDir = SParser(System.getProperty(CapabilityTypes.PARAM_TMP_DIR)).getPath(PulsarConstants.PULSAR_DEFAULT_TMP_DIR)
    val dataDir = SParser(System.getProperty(CapabilityTypes.PARAM_DATA_DIR)).getPath(PulsarConstants.PULSAR_DEFAULT_DATA_DIR)

    val cacheDir = get(tmpDir, "cache")
    val webCacheDir = get(cacheDir, "web")
    val fileCacheDir = get(cacheDir, "files")
    val reportDir = get(tmpDir, "report")
    val testDir = get(tmpDir, "test")

    @JvmField val PATH_LAST_BATCH_ID = get(tmpDir, "last-batch-id")
    @JvmField val PATH_LAST_GENERATED_ROWS = get(tmpDir, "last-generated-rows")
    @JvmField val PATH_LOCAL_COMMAND = get(tmpDir, "pulsar-commands")
    @JvmField val PATH_EMERGENT_SEEDS = get(tmpDir, "emergent-seeds")
    @JvmField val PATH_BANNED_URLS = get(tmpDir, "banned-urls")
    @JvmField val PATH_UNREACHABLE_HOSTS = get(tmpDir, "unreachable-hosts.txt")

    private val rootDirStr get() = homeDir.toString()

    init {
        if (!Files.exists(tmpDir)) Files.createDirectories(tmpDir)
        if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir)
        if (!Files.exists(webCacheDir)) Files.createDirectories(webCacheDir)
    }

    fun get(baseDirectory: Path, vararg more: String): Path {
        return Paths.get(baseDirectory.toString(), *more)
    }

    fun get(first: String, vararg more: String): Path {
        return Paths.get(rootDirStr, first.removePrefix(rootDirStr), *more)
    }

    fun fromUri(uri: String, suffix: String = ""): String {
        val domain = InternetDomainName.from(URL(uri).host).topPrivateDomain().toString()
        val path = domain.replace('.', '-') + "-" + DigestUtils.md5Hex(uri)
        return if (suffix.isNotEmpty()) path + suffix else path
    }

    fun relative(absolutePath: String): String {
        return StringUtils.substringAfter(absolutePath, homeDir.toString())
    }
}
