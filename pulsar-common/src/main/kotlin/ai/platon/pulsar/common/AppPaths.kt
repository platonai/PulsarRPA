package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.*
import com.google.common.net.InetAddresses
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
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
    val SYS_TMP_DIR = Paths.get(AppConstants.TMP_DIR)
    @JvmField
    val HOME_DIR = SParser(System.getProperty(PARAM_HOME_DIR)).getPath(AppConstants.PULSAR_DEFAULT_TMP_DIR)
    @JvmField
    val TMP_DIR = SParser(System.getProperty(PARAM_TMP_DIR)).getPath(AppConstants.PULSAR_DEFAULT_TMP_DIR)
    @JvmField
    val DATA_DIR = SParser(System.getProperty(PARAM_DATA_DIR)).getPath(AppConstants.PULSAR_DEFAULT_DATA_DIR)

    // directory for symbolic links, this path should be as short as possible
    @JvmField
    val LINKS_DIR = get(SYS_TMP_DIR, "ln")

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
    val PATH_UNREACHABLE_HOSTS = get(REPORT_DIR, "unreachable-hosts.txt")

    // TODO: distinct tmp dir and home dir
    private val tmpDirStr get() = TMP_DIR.toString()
    private val homeDirStr get() = HOME_DIR.toString()

    init {
        arrayOf(TMP_DIR, CACHE_DIR, WEB_CACHE_DIR, FILE_CACHE_DIR, LINKS_DIR, REPORT_DIR, SCRIPT_DIR, TEST_DIR).forEach {
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

    fun random(prefix: String = "", suffix: String = ""): String {
        return "$prefix${UUID.randomUUID()}$suffix"
    }

    fun hex(uri: String, prefix: String = "", suffix: String = ""): String {
        val path = DigestUtils.md5Hex(uri)
        return "$prefix$path$suffix"
    }

    fun fromUri(uri: String, prefix: String = "", suffix: String = ""): String {
        val u = Urls.getURLOrNull(uri) ?: return UUID.randomUUID().toString()
        var path = when {
            InetAddresses.isInetAddress(u.host) -> u.host
            else -> InternetDomainName.from(u.host).topPrivateDomain()
        }
        path = path.toString().replace('.', '-') + "-" + DigestUtils.md5Hex(uri)
        return "$prefix$path$suffix"
    }

    fun symbolicLinkFromUri(uri: String, prefix: String = "", suffix: String = ".htm"): Path {
        return LINKS_DIR.resolve("$prefix${hex(uri)}$suffix")
    }
}
