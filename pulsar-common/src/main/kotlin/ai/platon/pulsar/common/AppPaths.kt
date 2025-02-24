package ai.platon.pulsar.common

import ai.platon.pulsar.common.AppPaths.fromHost
import ai.platon.pulsar.common.urls.UrlUtils
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class RequiredFile

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class RequiredDirectory

/**
 * Created by vincent on 18-3-23.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
object AppPaths {
    
    val SYS_TMP_DIR = Paths.get(AppContext.TMP_DIR)
    val SYS_USER_DIR = Paths.get(AppContext.USER_DIR)
    val SYS_USER_HOME = Paths.get(AppContext.USER_HOME)
    
    /**
     * The directory for the user's default browser.
     *
     * The placeholder directory for the user's default browser. This is a placeholder, actually no data dir
     * should be specified, so the browser driver opens a browser just like a normal user opens it.
     * The actual data dir of user's browser are different on different operating systems, for example,
     * on linux, chrome's data dir is: ~/.config/google-chrome/
     */
    val SYSTEM_DEFAULT_BROWSER_DATA_DIR_PLACEHOLDER = SYS_TMP_DIR.resolve(".SYSTEM_DEFAULT_DATA_DIR_PLACEHOLDER")
    val SYSTEM_DEFAULT_BROWSER_CONTEXT_DIR_PLACEHOLDER = SYSTEM_DEFAULT_BROWSER_DATA_DIR_PLACEHOLDER
    
    // Directory for symbolic links, this path should be as short as possible
    @RequiredDirectory
    val SYS_TMP_LINKS_DIR = SYS_TMP_DIR.resolve("ln")
    
    @RequiredDirectory
    val DATA_DIR = AppContext.APP_DATA_DIR
    
    @RequiredDirectory
    val CONF_DIR = AppContext.APP_DATA_DIR.resolve("config")
    
    @RequiredDirectory
    val BROWSER_DATA_DIR = DATA_DIR.resolve("browser")
    
    @RequiredDirectory
    val CHROME_DATA_DIR_PROTOTYPE = BROWSER_DATA_DIR.resolve("chrome/prototype/google-chrome")
    
    @RequiredDirectory
    val CONTEXT_DEFAULT_DIR = BROWSER_DATA_DIR.resolve("chrome/default")
    
    @RequiredDirectory
    val LOCAL_DATA_DIR = DATA_DIR.resolve("data")
    
    @RequiredDirectory
    val LOCAL_STORAGE_DIR = LOCAL_DATA_DIR.resolve("store")
    
    @RequiredDirectory
    val LOCAL_TEST_DATA_DIR = LOCAL_DATA_DIR.resolve("test")
    
    @RequiredDirectory
    val LOCAL_TEST_WEB_PAGE_DIR = LOCAL_TEST_DATA_DIR.resolve("web")
    
    @RequiredDirectory
    val TMP_DIR = AppContext.APP_TMP_DIR
    
    @RequiredDirectory
    val PROC_TMP_DIR = AppContext.APP_PROC_TMP_DIR
    
    @RequiredDirectory
    val PROC_TMP_TMP_DIR = PROC_TMP_DIR.resolve("tmp")
    
    @RequiredDirectory
    val CACHE_DIR = PROC_TMP_DIR.resolve("cache")
    
    @RequiredDirectory
    val WEB_CACHE_DIR = CACHE_DIR.resolve("web")
    
    @RequiredDirectory
    val DOC_EXPORT_DIR = WEB_CACHE_DIR.resolve("export")
    
    @RequiredDirectory
    val WEB_SCREENSHOT_DIR = WEB_CACHE_DIR.resolve("screenshot")
    
    @RequiredDirectory
    val FILE_CACHE_DIR = CACHE_DIR.resolve("files")
    
    @RequiredDirectory
    val PROMPT_CACHE_DIR = CACHE_DIR.resolve("prompts")
    
    @RequiredDirectory
    val TMP_CONF_DIR = PROC_TMP_DIR.resolve("conf")
    
    @RequiredDirectory
    val REPORT_DIR = PROC_TMP_DIR.resolve("report")
    
    @RequiredDirectory
    val PROC_DIR = PROC_TMP_DIR.resolve("proc")
    
    @RequiredDirectory
    val METRICS_DIR = REPORT_DIR.resolve("metrics")
    
    @RequiredDirectory
    val SCRIPT_DIR = PROC_TMP_DIR.resolve("scripts")
    
    @RequiredDirectory
    val TEST_DIR = PROC_TMP_DIR.resolve("test")
    
    @RequiredDirectory
    val CONTEXT_BASE_DIR = PROC_TMP_DIR.resolve("context")
    
    @RequiredDirectory
    val CONTEXT_GROUP_BASE_DIR = CONTEXT_BASE_DIR.resolve("groups")
    
    @RequiredDirectory
    val CONTEXT_TMP_DIR = CONTEXT_BASE_DIR.resolve("tmp")
    
    @RequiredFile
    val BROWSER_TMP_DIR_LOCK = CONTEXT_TMP_DIR.resolve("browser.tmp.lock")
    
    /**
     * Proxy directory
     * */
    @RequiredDirectory
    val PROXY_BASE_DIR = DATA_DIR.resolve("proxy")
    
    @RequiredDirectory
    val ENABLED_PROVIDER_DIR = PROXY_BASE_DIR.resolve("providers-enabled")
    
    @RequiredDirectory
    val AVAILABLE_PROVIDER_DIR = PROXY_BASE_DIR.resolve("providers-available")
    
    @RequiredDirectory
    val ENABLED_PROXY_DIR = PROXY_BASE_DIR.resolve("proxies-enabled")
    
    @RequiredDirectory
    val AVAILABLE_PROXY_DIR = PROXY_BASE_DIR.resolve("proxies-available")
    
    @RequiredDirectory
    val PROXY_ARCHIVE_DIR = PROXY_BASE_DIR.resolve("proxies-archived")
    
    @RequiredFile
    val PROXY_BANNED_HOSTS_FILE = PROXY_BASE_DIR.resolve("proxies-banned-hosts.txt")
    
    @RequiredFile
    val PROXY_BANNED_SEGMENTS_FILE = PROXY_BASE_DIR.resolve("proxies-banned-segments.txt")
    
    @RequiredFile
    val PROXY_BAN_STRATEGY = PROXY_BASE_DIR.resolve("proxy-ban-strategy.txt")
    
    @RequiredFile
    val PATH_LOCAL_COMMAND = TMP_DIR.resolve("pulsar-commands")
    
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
    
    /**
     * Resolve the given path parts to a path.
     * Copy from JDK 22 for backward compatibility.
     * */
    fun resolve(base: Path, first: String, vararg more: String): Path {
        var result = base.resolve(first)
        for (s in more) {
            result = result.resolve(s)
        }
        return result
    }
    
    fun get(first: String, vararg more: String): Path = Paths.get(homeDirStr, first.removePrefix(homeDirStr), *more)
    
    /**
     * Get a path of the application's temporary directory.
     *
     * A typical application temporary directory is:
     *
     * ```powershell
     * $env:Temp/pulsar-$env:USERNAME/
     * ```
     *
     * TODO: fix me: assertTrue(path2.startsWith(AppPaths.TMP_DIR))
     * ```kotlin
     * assertTrue(path2.startsWith(AppPaths.TMP_DIR), "$path -> $path2")
     * ```
     *
     * @param first the first part of the path
     * @param more the rest parts of the path
     */
    fun getTmp(first: String, vararg more: String): Path = resolve(TMP_DIR, first, *more)
    
    fun getRandomTmp(prefix: String = "", suffix: String = ""): Path =
        getTmp(prefix, RandomStringUtils.randomAlphabetic(18), suffix)
    
    fun getProcTmp(first: String, vararg more: String): Path = resolve(PROC_TMP_DIR, first, *more)
    
    /**
     * Get a path of the temporary directory in the process's temporary directory.
     *
     * A typical process temporary directory is:
     *
     * ```powershell
     * $env:TMP/pulsar-$env:USERNAME/
     * ```
     *
     * And the tmp-tmp directory is:
     *
     * ```powershell
     * $env:TMP/pulsar-$env:USERNAME/tmp
     * ```
     *
     * * @param first the first part of the path
     * @param more the rest parts of the path
     *
     * @return the path in the process's temporary directory
     * */
    fun getProcTmpTmp(first: String, vararg more: String): Path = resolve(PROC_TMP_DIR.resolve("tmp"), first, *more)
    
    fun getRandomProcTmpTmp(prefix: String = "", suffix: String = ""): Path =
        getProcTmpTmp(prefix + RandomStringUtils.randomAlphabetic(18) + suffix)
    
    fun random(prefix: String = "", suffix: String = ""): String =
        "$prefix${RandomStringUtils.randomAlphabetic(18)}$suffix"
    
    fun hex(uri: String, prefix: String = "", suffix: String = ""): String {
        return DigestUtils.md5Hex(uri).let { "$prefix$it$suffix" }
    }
    
    fun fileId(uri: String) = DigestUtils.md5Hex(uri)
    
    /**
     * Create a mock page path.
     * */
    fun mockPagePath(uri: String): Path {
        val filename = fromUri(uri, "", ".htm")
        return LOCAL_TEST_WEB_PAGE_DIR.resolve(filename)
    }
    
    /**
     * Create a filename compatible string from the given url.
     * */
    fun fromHost(url: URL): String {
        var host = url.host
        host = if (Strings.isIpLike(host) || Strings.isIpPortLike(host) || host == "localhost") {
            host
        } else {
            runCatching { InternetDomainName.from(host).topPrivateDomain().toString() }.getOrNull() ?: "unknown"
        }
        
        return host.replace('.', '-')
    }

    /**
     * Create a filename compatible string from the given url.
     * */
    fun fromHost(url: String): String {
        val u = UrlUtils.getURLOrNull(url) ?: return "unknown"
        return fromHost(u)
    }
    
    @Deprecated("Use AppPaths.fromHost instead", replaceWith = ReplaceWith("AppPaths.fromHost(url)"))
    fun fromDomain(url: String) = fromHost(url)
    
    /**
     * Create a filename compatible string from the given uri.
     * */
    fun fromUri(uri: String, prefix: String = "", suffix: String = ""): String {
        val u = UrlUtils.getURLOrNull(uri) ?: return "${prefix}unknown$suffix"
        
        val dirForDomain = fromHost(u)
        val fileId = fileId(uri)
        return "$prefix$dirForDomain-$fileId$suffix"
    }
    
    /**
     * Create a symbolic link from the given uri.
     *
     * The symbolic link is url based, unique, shorter but not readable filename
     * */
    fun uniqueSymbolicLinkForUri(uri: String, suffix: String = ".htm"): Path {
        return SYS_TMP_LINKS_DIR.resolve(hex(uri, "", suffix))
    }
}
