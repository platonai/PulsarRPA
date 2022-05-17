package ai.platon.pulsar.browser.driver.chrome.common

import ai.platon.pulsar.browser.common.BrowserSettings
import java.time.Duration

/**
 * The launch config
 * */
class LauncherOptions(
    val browserSettings: BrowserSettings = BrowserSettings(),
    var supervisorProcess: String? = null,
    val supervisorProcessArgs: MutableList<String> = mutableListOf()
) {
    var startupWaitTime = DEFAULT_STARTUP_WAIT_TIME
    var shutdownWaitTime = DEFAULT_SHUTDOWN_WAIT_TIME
    var threadWaitTime = THREAD_JOIN_WAIT_TIME

    companion object {
        /** Default startup wait time in seconds. */
        val DEFAULT_STARTUP_WAIT_TIME = Duration.ofSeconds(60)
        /** Default shutdown wait time in seconds. */
        val DEFAULT_SHUTDOWN_WAIT_TIME = Duration.ofSeconds(60)
        /** 5 seconds wait time for threads to stop. */
        val THREAD_JOIN_WAIT_TIME = Duration.ofSeconds(5)
    }
}

/** Chrome argument */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ChromeParameter(val value: String)

/**
 * The options to open chrome devtools
 * */
class ChromeOptions(
    // user data dir is set in LauncherOptions
//        @ChromeParameter("user-data-dir")
//        var userDataDir: Path = AppPaths.CHROME_TMP_DIR,
    @ChromeParameter("proxy-server")
    var proxyServer: String? = null,
    @ChromeParameter("headless")
    var headless: Boolean = false,
    @ChromeParameter("incognito")
    var incognito: Boolean = false,
    @ChromeParameter("disable-gpu")
    var disableGpu: Boolean = true,
    @ChromeParameter("hide-scrollbars")
    var hideScrollbars: Boolean = true,
    @ChromeParameter("remote-debugging-port")
    var remoteDebuggingPort: Int = 0,
    @ChromeParameter("no-default-browser-check")
    var noDefaultBrowserCheck: Boolean = true,
    @ChromeParameter("no-first-run")
    var noFirstRun: Boolean = true,
    @ChromeParameter("no-startup-window")
    var noStartupWindow: Boolean = true,
    @ChromeParameter("mute-audio")
    var muteAudio: Boolean = true,
    @ChromeParameter("disable-background-networking")
    var disableBackgroundNetworking: Boolean = true,
    @ChromeParameter("disable-background-timer-throttling")
    var disableBackgroundTimerThrottling: Boolean = true,
    @ChromeParameter("disable-client-side-phishing-detection")
    var disableClientSidePhishingDetection: Boolean = true,
    @ChromeParameter("disable-default-apps")
    var disableDefaultApps: Boolean = false,
    @ChromeParameter("disable-extensions")
    var disableExtensions: Boolean = false,
    @ChromeParameter("disable-hang-monitor")
    var disableHangMonitor: Boolean = true,
    @ChromeParameter("disable-popup-blocking")
    var disablePopupBlocking: Boolean = true,
    @ChromeParameter("disable-prompt-on-repost")
    var disablePromptOnRepost: Boolean = true,
    @ChromeParameter("disable-sync")
    var disableSync: Boolean = true,
    @ChromeParameter("disable-translate")
    var disableTranslate: Boolean = true,
    @ChromeParameter("disable-blink-features")
    var disableBlinkFeatures: String = "AutomationControlled",
    @ChromeParameter("metrics-recording-only")
    var metricsRecordingOnly: Boolean = true,
    @ChromeParameter("safebrowsing-disable-auto-update")
    var safebrowsingDisableAutoUpdate: Boolean = true,
    @ChromeParameter("no-sandbox")
    var noSandbox: Boolean = false,
    @ChromeParameter("ignore-certificate-errors")
    var ignoreCertificateErrors: Boolean = true
) {
    val additionalArguments: MutableMap<String, Any?> = mutableMapOf()

    fun addArgument(key: String, value: String? = null): ChromeOptions {
        additionalArguments[key] = value
        return this
    }

    fun removeArgument(key: String): ChromeOptions {
        additionalArguments.remove(key)
        return this
    }

    fun merge(args: Map<String, Any?>) = args.forEach { (key, value) -> addArgument(key, value?.toString()) }

    fun toMap(): Map<String, Any?> {
        val args = ChromeOptions::class.java.declaredFields
            .filter { it.annotations.any { it is ChromeParameter } }
            .onEach { it.isAccessible = true }
            .associateTo(LinkedHashMap()) { it.getAnnotation(ChromeParameter::class.java).value to it.get(this) }

        args.putAll(additionalArguments)

        return args
    }

    fun toList() = toList(toMap())

    fun toList(args: Map<String, Any?>): List<String> {
        val result = ArrayList<String>()
        for ((key, value) in args) {
            if (value != null && false != value) {
                if (true == value) {
                    result.add("--$key")
                } else {
                    result.add("--$key=$value")
                }
            }
        }
        return result
    }

    override fun toString() = toList().joinToString(" ") { it }
}
