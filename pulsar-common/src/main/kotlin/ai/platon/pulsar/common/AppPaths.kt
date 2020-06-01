package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import com.google.common.net.InetAddresses
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class RequiredFile

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class RequiredDirectory

/**
 * Created by vincent on 18-3-23.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
object AppPaths {

    val SYS_TMP_DIR = Paths.get(AppConstants.TMP_DIR)
    val SYS_USER_DIR = Paths.get(AppConstants.USER_DIR)
    val SYS_USER_HOME = Paths.get(AppConstants.USER_HOME)

    // directory for symbolic links, this path should be as short as possible
    @RequiredDirectory
    val SYS_TMP_LINKS_DIR = SYS_TMP_DIR.resolve("ln")

    @RequiredDirectory
    val HOME_DIR = AppConstants.APP_HOME_DIR
    @RequiredDirectory
    val BROWSER_DATA_DIR = HOME_DIR.resolve( "browser")
    @RequiredDirectory
    val CHROME_DATA_BACKUP_DIR = BROWSER_DATA_DIR.resolve("google-chrome-backup")
    @RequiredDirectory
    val DATA_DIR = HOME_DIR.resolve("data")

    @RequiredDirectory
    val TMP_DIR = AppConstants.APP_TMP_DIR
    @RequiredDirectory
    val CACHE_DIR = TMP_DIR.resolve("cache")
    @RequiredDirectory
    val WEB_CACHE_DIR = CACHE_DIR.resolve("web")
    @RequiredDirectory
    val DOC_EXPORT_DIR = WEB_CACHE_DIR.resolve("web").resolve("export")
    @RequiredDirectory
    val FILE_CACHE_DIR = CACHE_DIR.resolve("files")
    @RequiredDirectory
    val TMP_CONF_DIR = TMP_DIR.resolve("conf")
    @RequiredDirectory
    val REPORT_DIR = TMP_DIR.resolve( "report")
    @RequiredDirectory
    val METRICS_DIR = REPORT_DIR.resolve( "metrics")
    @RequiredDirectory
    val SCRIPT_DIR = TMP_DIR.resolve( "scripts")
    @RequiredDirectory
    val TEST_DIR = TMP_DIR.resolve( "test")

    @RequiredDirectory
    val BROWSER_TMP_DIR = TMP_DIR.resolve( "browser")
    @RequiredFile
    val BROWSER_TMP_DIR_LOCK = TMP_DIR.resolve( "browser.lock")
    @RequiredDirectory
    val CHROME_TMP_DIR = BROWSER_TMP_DIR.resolve("google-chrome")

    /**
     * Proxy directory
     * */
    @RequiredDirectory
    val PROXY_BASE_DIR = TMP_DIR.resolve("proxy")
    @RequiredDirectory
    val ENABLED_PROVIDER_DIR = PROXY_BASE_DIR.resolve( "providers-enabled")
    @RequiredDirectory
    val AVAILABLE_PROVIDER_DIR = PROXY_BASE_DIR.resolve("providers-available")
    @RequiredDirectory
    val ENABLED_PROXY_DIR = PROXY_BASE_DIR.resolve( "proxies-enabled")

    @RequiredDirectory
    val AVAILABLE_PROXY_DIR = PROXY_BASE_DIR.resolve( "proxies-available")
    @RequiredDirectory
    val PROXY_ARCHIVE_DIR = PROXY_BASE_DIR.resolve("proxies-archived")
    @RequiredFile
    val PROXY_BANNED_HOSTS_FILE = PROXY_BASE_DIR.resolve("proxies-banned-hosts.txt")
    @RequiredFile
    val PROXY_BANNED_SEGMENTS_FILE = PROXY_BASE_DIR.resolve("proxies-banned-segments.txt")
    @RequiredFile
    val PROXY_BAN_STRATEGY = PROXY_BASE_DIR.resolve( "proxy-ban-strategy.txt")

    @RequiredDirectory
    val ARCHIVE_DIR = HOME_DIR.resolve("archive")
    @RequiredDirectory
    val TMP_ARCHIVE_DIR = TMP_DIR.resolve("archive")

    @RequiredFile
    val PATH_LOCAL_COMMAND = TMP_DIR.resolve("pulsar-commands")
    @RequiredFile
    val PATH_EMERGENT_SEEDS = TMP_DIR.resolve("emergent-seeds")

    @RequiredFile
    val PATH_LAST_BATCH_ID = REPORT_DIR.resolve("last-batch-id")
    @RequiredFile
    val PATH_LAST_GENERATED_ROWS = REPORT_DIR.resolve("last-generated-rows")
    @RequiredFile
    val PATH_BANNED_URLS = REPORT_DIR.resolve("banned-urls")
    @RequiredFile
    val PATH_UNREACHABLE_HOSTS = REPORT_DIR.resolve("unreachable-hosts.txt")

    private val tmpDirStr get() = TMP_DIR.toString()
    private val homeDirStr get() = HOME_DIR.toString()

    init {
        AppPaths::class.java.declaredFields
                .filter { it.annotations.any { it is RequiredDirectory } }
                .mapNotNull { it.get(AppPaths) as? Path }
                .forEach { it.takeUnless { Files.exists(it) }?.let { Files.createDirectories(it) } }

        AppPaths::class.java.declaredFields
                .filter { it.annotations.any { it is RequiredFile } }
                .mapNotNull { it.get(AppPaths) as? Path }
                .forEach {
                    it.parent.takeUnless { Files.exists(it) }?.let { Files.createDirectories(it) }
                    it.takeUnless { Files.exists(it) }?.let { Files.createFile(it) }
                }
    }

    fun get(first: String, vararg more: String): Path = Paths.get(homeDirStr, first.removePrefix(homeDirStr), *more)

    fun getTmp(first: String, vararg more: String): Path = Paths.get(tmpDirStr, first.removePrefix(tmpDirStr), *more)

    fun random(prefix: String = "", suffix: String = ""): String = "$prefix${UUID.randomUUID()}$suffix"

    fun hex(uri: String, prefix: String = "", suffix: String = ""): String {
        return DigestUtils.md5Hex(uri).let { "$prefix$it$suffix" }
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

    fun uniqueSymbolicLinkForUri(uri: String, suffix: String = ".htm"): Path {
        return SYS_TMP_LINKS_DIR.resolve(hex(uri, "", suffix))
    }
}
