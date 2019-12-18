package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import com.google.common.net.InetAddresses
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
object AppPaths {
    @JvmField
    val HOME_DIR = SParser(System.getProperty(PARAM_HOME_DIR)).getPath(AppConstants.PULSAR_DEFAULT_TMP_DIR)
    @JvmField
    val TMP_DIR = SParser(System.getProperty(PARAM_TMP_DIR)).getPath(AppConstants.PULSAR_DEFAULT_TMP_DIR)
    @JvmField
    val DATA_DIR = SParser(System.getProperty(PARAM_DATA_DIR)).getPath(AppConstants.PULSAR_DEFAULT_DATA_DIR)

    @JvmField
    val CACHE_DIR = get(TMP_DIR, "cache")
    @JvmField
    val WEB_CACHE_DIR = get(CACHE_DIR, "web")
    @JvmField
    val FILE_CACHE_DIR = get(CACHE_DIR, "files")
    @JvmField
    val REPORT_DIR = get(TMP_DIR, "report")
    @JvmField
    val SCRIPT_DIR = get(TMP_DIR, "scripts")
    @JvmField
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

    // TODO: distinct tmp dir and home dir
    private val tmpDirStr get() = TMP_DIR.toString()
    private val homeDirStr get() = HOME_DIR.toString()

    init {
        arrayOf(TMP_DIR, CACHE_DIR, WEB_CACHE_DIR, REPORT_DIR, SCRIPT_DIR, TEST_DIR).forEach {
            if (!Files.exists(it)) {
                Files.createDirectories(it)
            }
        }
    }

    fun get(baseDirectory: Path, vararg more: String): Path {
        return Paths.get(baseDirectory.toString(), *more)
    }

    fun get(first: String, vararg more: String): Path {
        return Paths.get(homeDirStr, first.removePrefix(homeDirStr), *more)
    }

    fun getTmp(first: String, vararg more: String): Path {
        return Paths.get(tmpDirStr, first.removePrefix(tmpDirStr), *more)
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

    // use this version when tested
    fun fromUri2(url: String, suffix: String = ""): String {
        val u = Urls.getURLOrNull(url)?:return "unknown-" + UUID.randomUUID().toString()
        var path = when {
            InetAddresses.isInetAddress(u.host) -> u.host
            else -> InternetDomainName.from(u.host).topPrivateDomain()
        }
        path = path.toString().replace('.', '-') + "-" + DigestUtils.md5Hex(url)
        return if (suffix.isNotEmpty()) path + suffix else path
    }

    fun relative(absolutePath: String): String {
        return StringUtils.substringAfter(absolutePath, HOME_DIR.toString())
    }
}
