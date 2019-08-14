package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.PulsarConstants
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * Created by vincent on 18-3-23.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
object PulsarPaths {
    val HOME_DIR = SParser(System.getProperty(PARAM_HOME_DIR)).getPath(PulsarConstants.PULSAR_DEFAULT_TMP_DIR)
    val TMP_DIR = SParser(System.getProperty(PARAM_TMP_DIR)).getPath(PulsarConstants.PULSAR_DEFAULT_TMP_DIR)
    val DATA_DIR = SParser(System.getProperty(PARAM_DATA_DIR)).getPath(PulsarConstants.PULSAR_DEFAULT_DATA_DIR)

    val CACHE_DIR = get(TMP_DIR, "cache")
    val WEB_CACHE_DIR = get(CACHE_DIR, "web")
    val FILE_CACHE_DIR = get(CACHE_DIR, "files")
    val REPORT_DIR = get(TMP_DIR, "report")
    val TEST_DIR = get(TMP_DIR, "test")

    @JvmField
    val PATH_LOCAL_COMMAND = get(TMP_DIR, "pulsar-commands")
    @JvmField
    val PATH_EMERGENT_SEEDS = get(TMP_DIR, "emergent-seeds")

    @JvmField
    val PATH_LAST_BATCH_ID = get(REPORT_DIR, "last-batch-id")
    @JvmField
    val PATH_LAST_GENERATED_ROWS = get(REPORT_DIR, "last-generated-rows")
    @JvmField
    val PATH_BANNED_URLS = get(REPORT_DIR, "banned-urls")
    @JvmField
    val PATH_UNREACHABLE_HOSTS = get(REPORT_DIR,  "unreachable-hosts.txt")

    private val tmpDirStr get() = TMP_DIR.toString()
    private val homeDirStr get() = HOME_DIR.toString()

    init {
        if (!Files.exists(TMP_DIR)) Files.createDirectories(TMP_DIR)
        if (!Files.exists(CACHE_DIR)) Files.createDirectories(CACHE_DIR)
        if (!Files.exists(WEB_CACHE_DIR)) Files.createDirectories(WEB_CACHE_DIR)
    }

    fun get(baseDirectory: Path, vararg more: String): Path {
        return Paths.get(baseDirectory.toString(), *more)
    }

    fun get(first: String, vararg more: String): Path {
        return Paths.get(homeDirStr, first.removePrefix(homeDirStr), *more)
    }

    fun fromUri(url: String, suffix: String = ""): String {
        val u = Urls.getURLOrNull(url)
        val path = when {
            u == null -> "unknown-" + UUID.randomUUID().toString()
            StringUtil.isIpLike(u.host) -> u.host.replace('.', '-') + "-" + DigestUtils.md5Hex(url)
            else -> {
                val domain = InternetDomainName.from(u.host).topPrivateDomain().toString()
                domain.replace('.', '-') + "-" + DigestUtils.md5Hex(url)
            }
        }

        return if (suffix.isNotEmpty()) path + suffix else path
    }

    fun relative(absolutePath: String): String {
        return StringUtils.substringAfter(absolutePath, HOME_DIR.toString())
    }
}
