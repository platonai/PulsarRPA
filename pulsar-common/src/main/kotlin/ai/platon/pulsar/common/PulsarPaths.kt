package ai.platon.pulsar.common

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by vincent on 18-3-23.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
object PulsarPaths {
    val homeDir = ai.platon.pulsar.common.SParser(System.getProperty(ai.platon.pulsar.common.config.CapabilityTypes.PARAM_HOME_DIR)).getPath(ai.platon.pulsar.common.config.PulsarConstants.PULSAR_DEFAULT_TMP_DIR)
    val tmpDir = ai.platon.pulsar.common.SParser(System.getProperty(ai.platon.pulsar.common.config.CapabilityTypes.PARAM_TMP_DIR)).getPath(ai.platon.pulsar.common.config.PulsarConstants.PULSAR_DEFAULT_TMP_DIR)
    val dataDir = ai.platon.pulsar.common.SParser(System.getProperty(ai.platon.pulsar.common.config.CapabilityTypes.PARAM_DATA_DIR)).getPath(ai.platon.pulsar.common.config.PulsarConstants.PULSAR_DEFAULT_DATA_DIR)

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
        val md5 = DigestUtils.md5Hex(uri)
        return if (suffix.isNotEmpty()) md5 + suffix else md5
    }

    fun relative(absolutePath: String): String {
        return StringUtils.substringAfter(absolutePath, homeDir.toString())
    }
}
