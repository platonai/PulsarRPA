package ai.platon.pulsar.common

import ai.platon.pulsar.common.urls.UrlUtils
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

    val SYS_TMP_DIR = Paths.get(AppContext.TMP_DIR)
    val SYS_USER_DIR = Paths.get(AppContext.USER_DIR)
    val SYS_USER_HOME = Paths.get(AppContext.USER_HOME)

    // directory for symbolic links, this path should be as short as possible
    @RequiredDirectory
    val SYS_TMP_LINKS_DIR = SYS_TMP_DIR.resolve("ln")

    @RequiredDirectory
    val DATA_DIR = AppContext.APP_DATA_DIR
    @RequiredDirectory
    val BROWSER_DATA_DIR = DATA_DIR.resolve( "browser")
    @RequiredDirectory
    val CHROME_DATA_DIR_PROTOTYPE = BROWSER_DATA_DIR.resolve("chrome/prototype/google-chrome")
    @RequiredDirectory
    val LOCAL_DATA_DIR = DATA_DIR.resolve("data")
    @RequiredDirectory
    val LOCAL_STORAGE_DIR = LOCAL_DATA_DIR.resolve("store")
    @RequiredDirectory
    val LOCAL_TEST_DATA_DIR = LOCAL_DATA_DIR.resolve( "test")
    @RequiredDirectory
    val LOCAL_TEST_WEB_PAGE_DIR = LOCAL_TEST_DATA_DIR.resolve( "web")

    @RequiredDirectory
    val TMP_DIR = AppContext.APP_TMP_DIR
    @RequiredDirectory
    val PROC_TMP_DIR = AppContext.PROC_TMP_DIR
    @RequiredDirectory
    val CACHE_DIR = PROC_TMP_DIR.resolve("cache")
    @RequiredDirectory
    val WEB_CACHE_DIR = CACHE_DIR.resolve("web")
    @RequiredDirectory
    val DOC_EXPORT_DIR = WEB_CACHE_DIR.resolve("export")
    @RequiredDirectory
    val FILE_CACHE_DIR = CACHE_DIR.resolve("files")
    @RequiredDirectory
    val TMP_CONF_DIR = PROC_TMP_DIR.resolve("conf")
    @RequiredDirectory
    val REPORT_DIR = PROC_TMP_DIR.resolve( "report")
    @RequiredDirectory
    val PROC_DIR = PROC_TMP_DIR.resolve( "proc")
    @RequiredDirectory
    val METRICS_DIR = REPORT_DIR.resolve( "metrics")
    @RequiredDirectory
    val SCRIPT_DIR = PROC_TMP_DIR.resolve( "scripts")
    @RequiredDirectory
    val TEST_DIR = PROC_TMP_DIR.resolve( "test")

    @RequiredDirectory
    val CONTEXT_TMP_DIR = PROC_TMP_DIR.resolve( "context")
    @RequiredDirectory
    val BROWSER_TMP_DIR = CONTEXT_TMP_DIR.resolve( "browser")
    @RequiredFile
    val BROWSER_TMP_DIR_LOCK = CONTEXT_TMP_DIR.resolve( "browser.lock")
    @RequiredDirectory
    val CHROME_TMP_DIR = BROWSER_TMP_DIR.resolve("google-chrome")

    /**
     * Proxy directory
     * */
    @RequiredDirectory
    val PROXY_BASE_DIR = DATA_DIR.resolve("proxy")
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
    val ARCHIVE_DIR = DATA_DIR.resolve("archive")
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
    private val procTmpDirStr get() = PROC_TMP_DIR.toString()
    private val homeDirStr get() = DATA_DIR.toString()

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

    fun getProcTmp(first: String, vararg more: String): Path = Paths.get(procTmpDirStr, first.removePrefix(procTmpDirStr), *more)

    fun random(prefix: String = "", suffix: String = ""): String = "$prefix${UUID.randomUUID()}$suffix"

    fun hex(uri: String, prefix: String = "", suffix: String = ""): String {
        return DigestUtils.md5Hex(uri).let { "$prefix$it$suffix" }
    }

    fun fileId(uri: String) = DigestUtils.md5Hex(uri)

    fun mockPagePath(uri: String): Path {
        val filename = fromUri(uri, "", ".htm")
        return LOCAL_TEST_WEB_PAGE_DIR.resolve(filename)
    }

    fun fromUri(uri: String, prefix: String = "", suffix: String = ""): String {
        val u = UrlUtils.getURLOrNull(uri) ?: return "$prefix${UUID.randomUUID()}$suffix"

        var host = u.host.takeIf { Strings.isIpPortLike(it) } ?: InternetDomainName.from(u.host).topPrivateDomain().toString()
        host = host.replace('.', '-')
        val fileId = fileId(uri)
        return "$prefix$host-$fileId$suffix"
    }

    fun uniqueSymbolicLinkForUri(uri: String, suffix: String = ".htm"): Path {
        return SYS_TMP_LINKS_DIR.resolve(hex(uri, "", suffix))
    }
}
