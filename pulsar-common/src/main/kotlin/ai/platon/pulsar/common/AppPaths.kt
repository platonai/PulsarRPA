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

    @JvmField
    val SYS_TMP_DIR = Paths.get(AppConstants.TMP_DIR)
    // directory for symbolic links, this path should be as short as possible
    @RequiredDirectory @JvmField
    val SYS_TMP_LINKS_DIR = SYS_TMP_DIR.resolve("ln")

    @RequiredDirectory @JvmField
    val TMP_DIR = SParser(System.getProperty(PARAM_TMP_DIR)).getPath(AppConstants.PULSAR_DEFAULT_TMP_DIR)
    // TODO: check again whether we need a separate home dir
    // val HOME_DIR = SParser(System.getProperty(PARAM_HOME_DIR)).getPath(AppConstants.PULSAR_DEFAULT_TMP_DIR)
    @RequiredDirectory @JvmField
    val HOME_DIR = TMP_DIR
    /**
     * Application data are kept in the data dir
     * */
    @RequiredDirectory @JvmField
    val DATA_DIR = SParser(System.getProperty(PARAM_DATA_DIR)).getPath(AppConstants.PULSAR_DEFAULT_DATA_DIR)

    @RequiredDirectory @JvmField
    val CACHE_DIR = get(TMP_DIR, "cache")
    @RequiredDirectory @JvmField
    val WEB_CACHE_DIR = get(CACHE_DIR, "web")
    @RequiredDirectory @JvmField
    val FILE_CACHE_DIR = get(CACHE_DIR, "files")
    @RequiredDirectory @JvmField
    val REPORT_DIR = get(TMP_DIR, "report")
    @RequiredDirectory @JvmField
    val SCRIPT_DIR = get(TMP_DIR, "scripts")
    @RequiredDirectory @JvmField
    val TEST_DIR = get(TMP_DIR, "test")
    @RequiredDirectory @JvmField
    val BROWSER_TMP_DIR = get(TMP_DIR, "browser")
    @RequiredDirectory @JvmField
    val CHROME_TMP_DIR = get(BROWSER_TMP_DIR, "chrome")

    @RequiredDirectory @JvmField
    val PROXY_BASE_DIR = AppPaths.getTmp("proxy")
    @RequiredDirectory @JvmField
    val ENABLED_PROVIDER_DIR = AppPaths.get(PROXY_BASE_DIR, "providers-enabled")
    @RequiredDirectory @JvmField
    val AVAILABLE_PROVIDER_DIR = AppPaths.get(PROXY_BASE_DIR, "providers-available")
    @RequiredDirectory @JvmField
    val ENABLED_PROXY_DIR = AppPaths.get(PROXY_BASE_DIR, "proxies-enabled")

    @RequiredDirectory @JvmField
    val AVAILABLE_PROXY_DIR = AppPaths.get(PROXY_BASE_DIR, "proxies-available")
    @RequiredDirectory @JvmField
    val PROXY_ARCHIVE_DIR = AppPaths.get(PROXY_BASE_DIR, "proxies-archived")
    @RequiredFile @JvmField
    val LATEST_AVAILABLE_PROXY = AppPaths.get( "latest-available-proxy")
    @RequiredFile @JvmField
    val TEST_PROXY_FILE = AppPaths.get(PROXY_BASE_DIR, "test-ip")
    @RequiredFile @JvmField
    val PROXY_BANNED_HOSTS_FILE = AppPaths.get(PROXY_BASE_DIR, "proxies-banned-hosts.txt")
    @RequiredFile @JvmField
    val PROXY_BANNED_SEGMENTS_FILE = AppPaths.get(PROXY_BASE_DIR, "proxies-banned-segments.txt")
    @RequiredFile @JvmField
    val PROXY_BAN_STRATEGY = AppPaths.get(PROXY_BASE_DIR, "proxy-ban-strategy.txt")

    @RequiredDirectory @JvmField
    val ARCHIVE_DIR = get(HOME_DIR, "archive")
    @RequiredDirectory @JvmField
    val TMP_ARCHIVE_DIR = get(TMP_DIR, "archive")

    @RequiredFile @JvmField
    val PATH_LOCAL_COMMAND = get(TMP_DIR, "pulsar-commands")
    @RequiredFile @JvmField
    val PATH_EMERGENT_SEEDS = get(TMP_DIR, "emergent-seeds")

    @RequiredFile @JvmField
    val PATH_LAST_BATCH_ID = get(REPORT_DIR, "last-batch-id")
    @RequiredFile @JvmField
    val PATH_LAST_GENERATED_ROWS = get(REPORT_DIR, "last-generated-rows")
    @RequiredFile @JvmField
    val PATH_BANNED_URLS = get(REPORT_DIR, "banned-urls")
    @RequiredFile @JvmField
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

    fun get(baseDirectory: Path, vararg more: String): Path {
        return get(baseDirectory.toString(), *more)
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

    fun uniqueSymbolicLinkForURI(uri: String, suffix: String = ".htm"): Path {
        return SYS_TMP_LINKS_DIR.resolve(hex(uri, "", suffix))
    }
}
