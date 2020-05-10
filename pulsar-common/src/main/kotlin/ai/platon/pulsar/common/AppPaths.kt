package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes.PARAM_DATA_DIR
import ai.platon.pulsar.common.config.CapabilityTypes.PARAM_TMP_DIR
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
    val TMP_DIR = SParser(System.getProperty(PARAM_TMP_DIR)).getPath(AppConstants.PULSAR_DEFAULT_TMP_DIR)
    // TODO: check again whether we need a separate home dir
    // val HOME_DIR = SParser(System.getProperty(PARAM_HOME_DIR)).getPath(AppConstants.PULSAR_DEFAULT_TMP_DIR)
    @RequiredDirectory
    val HOME_DIR = TMP_DIR
    /**
     * Application data are kept in the data dir
     * */
    @RequiredDirectory
    val DATA_DIR = SParser(System.getProperty(PARAM_DATA_DIR)).getPath(AppConstants.PULSAR_DEFAULT_DATA_DIR)

    @RequiredDirectory
    val CACHE_DIR = get(TMP_DIR, "cache")
    @RequiredDirectory
    val WEB_CACHE_DIR = get(CACHE_DIR, "web")
    @RequiredDirectory
    val DOC_EXPORT_DIR = get(WEB_CACHE_DIR, "web", "export")
    @RequiredDirectory
    val FILE_CACHE_DIR = get(CACHE_DIR, "files")
    @RequiredDirectory
    val CONF_DIR = get(TMP_DIR, "etc")
    @RequiredDirectory
    val REPORT_DIR = get(TMP_DIR, "report")
    @RequiredDirectory
    val METRICS_DIR = get(REPORT_DIR, "metrics")
    @RequiredDirectory
    val SCRIPT_DIR = get(TMP_DIR, "scripts")
    @RequiredDirectory
    val TEST_DIR = get(TMP_DIR, "test")
    @RequiredDirectory
    val BROWSER_TMP_DIR = get(TMP_DIR, "browser")
    @RequiredFile
    val BROWSER_TMP_DIR_LOCK = get(TMP_DIR, "browser.lock")
    @RequiredDirectory
    val CHROME_TMP_DIR = get(BROWSER_TMP_DIR, "chrome")

    @RequiredDirectory
    val PROXY_BASE_DIR = AppPaths.getTmp("proxy")
    @RequiredDirectory
    val ENABLED_PROVIDER_DIR = AppPaths.get(PROXY_BASE_DIR, "providers-enabled")
    @RequiredDirectory
    val AVAILABLE_PROVIDER_DIR = AppPaths.get(PROXY_BASE_DIR, "providers-available")
    @RequiredDirectory
    val ENABLED_PROXY_DIR = AppPaths.get(PROXY_BASE_DIR, "proxies-enabled")

    @RequiredDirectory
    val AVAILABLE_PROXY_DIR = AppPaths.get(PROXY_BASE_DIR, "proxies-available")
    @RequiredDirectory
    val PROXY_ARCHIVE_DIR = AppPaths.get(PROXY_BASE_DIR, "proxies-archived")
    @RequiredFile
    val PROXY_BANNED_HOSTS_FILE = AppPaths.get(PROXY_BASE_DIR, "proxies-banned-hosts.txt")
    @RequiredFile
    val PROXY_BANNED_SEGMENTS_FILE = AppPaths.get(PROXY_BASE_DIR, "proxies-banned-segments.txt")
    @RequiredFile
    val PROXY_BAN_STRATEGY = AppPaths.get(PROXY_BASE_DIR, "proxy-ban-strategy.txt")

    @RequiredDirectory
    val ARCHIVE_DIR = get(HOME_DIR, "archive")
    @RequiredDirectory
    val TMP_ARCHIVE_DIR = get(TMP_DIR, "archive")

    @RequiredFile
    val PATH_LOCAL_COMMAND = get(TMP_DIR, "pulsar-commands")
    @RequiredFile
    val PATH_EMERGENT_SEEDS = get(TMP_DIR, "emergent-seeds")

    @RequiredFile
    val PATH_LAST_BATCH_ID = get(REPORT_DIR, "last-batch-id")
    @RequiredFile
    val PATH_LAST_GENERATED_ROWS = get(REPORT_DIR, "last-generated-rows")
    @RequiredFile
    val PATH_BANNED_URLS = get(REPORT_DIR, "banned-urls")
    @RequiredFile
    val PATH_UNREACHABLE_HOSTS = get(REPORT_DIR, "unreachable-hosts.txt")

    // TODO: distinct tmp dir and home dir
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

    fun get(baseDirectory: Path, vararg more: String): Path = get(baseDirectory.toString(), *more)

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
