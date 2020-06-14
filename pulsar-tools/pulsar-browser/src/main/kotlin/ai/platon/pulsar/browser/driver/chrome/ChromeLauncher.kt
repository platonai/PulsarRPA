package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.chrome.LauncherConfig.Companion.CHROME_BINARY_SEARCH_PATHS
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessTimeoutException
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.CapabilityTypes
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.channels.FileChannel
import java.nio.file.*
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.collections.component1
import kotlin.collections.component2

class LauncherConfig {
    var startupWaitTime = DEFAULT_STARTUP_WAIT_TIME
    var shutdownWaitTime = DEFAULT_SHUTDOWN_WAIT_TIME
    var threadWaitTime = THREAD_JOIN_WAIT_TIME

    companion object {
        /** Default startup wait time in seconds.  */
        val DEFAULT_STARTUP_WAIT_TIME = Duration.ofSeconds(60)
        /** Default shutdown wait time in seconds.  */
        val DEFAULT_SHUTDOWN_WAIT_TIME = Duration.ofSeconds(60)
        /** 5 seconds wait time for threads to stop.  */
        val THREAD_JOIN_WAIT_TIME = Duration.ofSeconds(5)

        val CHROME_BINARY_SEARCH_PATHS = arrayOf(
                "/usr/bin/google-chrome-stable",
                "/usr/bin/google-chrome",
                "/opt/google/chrome/chrome",
                "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
                "/Applications/Chromium.app/Contents/MacOS/Chromium",
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser"
        )
    }
}

/** Chrome argument */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ChromeParameter(val value: String)

class ChromeDevtoolsOptions(
        @ChromeParameter(ARG_USER_DATA_DIR)
        var userDataDir: Path = AppPaths.CHROME_TMP_DIR,
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
        @ChromeParameter("metrics-recording-only")
        var metricsRecordingOnly: Boolean = true,
        @ChromeParameter("safebrowsing-disable-auto-update")
        var safebrowsingDisableAutoUpdate: Boolean = true,
        @ChromeParameter("ignore-certificate-errors")
        var ignoreCertificateErrors: Boolean = true
) {
    var xvfb = false

    val additionalArguments: MutableMap<String, Any?> = mutableMapOf()

    fun addArguments(key: String, value: String? = null): ChromeDevtoolsOptions {
        additionalArguments[key] = value
        return this
    }

    fun removeArguments(key: String): ChromeDevtoolsOptions {
        additionalArguments.remove(key)
        return this
    }

    fun merge(args: Map<String, Any?>) = args.forEach { (key, value) -> addArguments(key, value?.toString()) }

    fun toMap(): Map<String, Any?> {
        val args = ChromeDevtoolsOptions::class.java.declaredFields
                .filter { it.annotations.any { it is ChromeParameter } }
                .onEach { it.isAccessible = true }
                .associateTo(mutableMapOf()) { it.getAnnotation(ChromeParameter::class.java).value to it.get(this) }

        args.putAll(additionalArguments)

        return args
    }

    fun toList() = toList(toMap())

    private fun toList(args: Map<String, Any?>): List<String> {
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

    companion object {
        const val ARG_USER_DATA_DIR = "user-data-dir"
    }
}

class ProcessLauncher {
    private val log = LoggerFactory.getLogger(ProcessLauncher::class.java)

    @Throws(IOException::class)
    fun launch(program: String, args: List<String>): Process {
        val command = mutableListOf<String>().apply { add(program); addAll(args) }
        val processBuilder = ProcessBuilder()
                .command(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)

        log.info("Launching process:\n{}", processBuilder.command().joinToString(" ") {
            Strings.doubleQuoteIfContainsWhitespace(it)
        })

        return processBuilder.start()
    }
}

class ChromeLauncher(
        private val processLauncher: ProcessLauncher = ProcessLauncher(),
        private val shutdownHookRegistry: ShutdownHookRegistry = RuntimeShutdownHookRegistry(),
        private val config: LauncherConfig = LauncherConfig()
) : AutoCloseable {

    companion object {
        private val DEVTOOLS_LISTENING_LINE_PATTERN = Pattern.compile("^DevTools listening on ws:\\/\\/.+:(\\d+)\\/")
        const val XVFB = "xvfb-run"
        val XVFB_ARGS = listOf(
                "-a",
                "-e", "/dev/stdout",
                "-s", "-screen 0 1920x1080x24"
        )
    }

    private val log = LoggerFactory.getLogger(ChromeLauncher::class.java)
    private var process: Process? = null
    private var userDataDir = AppPaths.CHROME_TMP_DIR
    private val shutdownHookThread = Thread { this.close() }

    fun launch(chromeBinaryPath: Path, options: ChromeDevtoolsOptions): RemoteChrome {
        userDataDir = options.userDataDir
        prepareUserDataDir()

        val port = launchChromeProcess(chromeBinaryPath, options)
        return Chrome(port)
    }

    fun launch(options: ChromeDevtoolsOptions) = launch(searchChromeBinary(), options)

    fun launch(headless: Boolean) = launch(searchChromeBinary(), ChromeDevtoolsOptions().also { it.headless = headless })

    fun launch() = launch(true)

    override fun close() {
        val p = process ?: return
        this.process = null
        if (p.isAlive) {
            destroyProcess(p)
            kotlin.runCatching { shutdownHookRegistry.remove(shutdownHookThread) }
                    .onFailure { log.warn("Unexpected exception", it) }
        }

        if (Files.exists(userDataDir)) {
            Files.deleteIfExists(userDataDir.resolve("INSTANCE_CLOSING"))
            Files.writeString(userDataDir.resolve("INSTANCE_CLOSED"), "", StandardOpenOption.CREATE_NEW)
        }
    }

    /**
     * Returns an exit value. This is just proxy to [Process.exitValue].
     *
     * @return Exit value of the process if exited.
     * @throws [IllegalThreadStateException] if the subprocess has not yet terminated. [     ] If the process hasn't even started.
     */
    fun exitValue(): Int {
        checkNotNull(process) { "Chrome process has not been started" }
        return process!!.exitValue()
    }

    /**
     * Tests whether the subprocess is alive. This is just proxy to [Process.isAlive].
     *
     * @return True if the subprocess has not yet terminated.
     * @throws IllegalThreadStateException if the subprocess has not yet terminated.
     */
    val isAlive: Boolean get() = process?.isAlive == true

    /**
     * Returns the chrome binary path.
     *
     * @return Chrome binary path.
     */
    private fun searchChromeBinary(): Path {
        val path = System.getProperty(CapabilityTypes.BROWSER_CHROME_PATH)
        if (path != null) {
            return Paths.get(path).takeIf { Files.isExecutable(it) }?.toAbsolutePath()
                    ?: throw RuntimeException("CHROME_PATH is not executable | $path")
        }

        return CHROME_BINARY_SEARCH_PATHS.map { Paths.get(it) }
                .firstOrNull { Files.isExecutable(it) }
                ?.toAbsolutePath()
                ?: throw RuntimeException("Could not find chrome binary in search path. Try setting CHROME_PATH environment value")
    }

    /**
     * Launches a chrome process given a chrome binary and its arguments.
     *
     * Launching chrome processes is CPU consuming, so we do this in a synchronized manner
     *
     * @param chromeBinary Chrome binary path.
     * @param chromeOptions Chrome arguments.
     * @return Port on which devtools is listening.
     * @throws IllegalStateException If chrome process has already been started.
     * @throws ChromeProcessException If an I/O error occurs during chrome process start.
     * @throws ChromeProcessTimeoutException If timeout expired while waiting for chrome to start.
     */
    @Throws(ChromeProcessException::class)
    @Synchronized
    private fun launchChromeProcess(chromeBinary: Path, chromeOptions: ChromeDevtoolsOptions): Int {
        check(!isAlive) { "Chrome process has already been started" }
        val xvfb = chromeOptions.xvfb
        val program = if (xvfb) XVFB else "$chromeBinary"
        val arguments = if (!xvfb) chromeOptions.toList() else {
            XVFB_ARGS + arrayOf("$chromeBinary", "--no-sandbox", "--disable-setuid-sandbox") + chromeOptions.toList()
        }

        return try {
            shutdownHookRegistry.register(shutdownHookThread)
            process = processLauncher.launch(program, arguments)

            process?.also {
                Files.writeString(chromeOptions.userDataDir.resolve("PID"), it.pid().toString(), StandardOpenOption.CREATE_NEW)
            }
            waitForDevToolsServer(process!!)
        } catch (e: IOException) {
            // Unsubscribe from registry on exceptions.
            shutdownHookRegistry.remove(shutdownHookThread)
            throw ChromeProcessException("Failed starting chrome process", e)
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    /**
     * Waits for DevTools server is up on chrome process.
     *
     * @param process Chrome process.
     * @return DevTools listening port.
     * @throws ChromeProcessTimeoutException If timeout expired while waiting for chrome process.
     */
    @Throws(ChromeProcessTimeoutException::class)
    private fun waitForDevToolsServer(process: Process): Int {
        var port = 0
        val processOutput = StringBuilder()
        val readLineThread = Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                // Wait for DevTools listening line and extract port number.
                var line: String
                while (reader.readLine().also { line = it } != null) {
                    log.takeIf { line.isNotBlank() }?.info("[output] - $line")
                    val matcher = DEVTOOLS_LISTENING_LINE_PATTERN.matcher(line)
                    if (matcher.find()) {
                        port = matcher.group(1).toInt()
                        break
                    }
                    processOutput.appendln(line)
                }
            }
        }
        readLineThread.start()

        try {
            readLineThread.join(config.startupWaitTime.toMillis())

            if (port == 0) {
                close(readLineThread)
                log.info("Process output:>>>\n$processOutput\n<<<")
                throw ChromeProcessTimeoutException("Timeout to waiting for chrome to start")
            }
        } catch (e: InterruptedException) {
            close(readLineThread)
            log.error("Interrupted while waiting for dev tools server", e)
            throw RuntimeException("Interrupted while waiting for dev tools server", e)
        }
        return port
    }

    private fun destroyProcess(process: ProcessHandle) {
        process.children().forEach { destroyProcess(it) }

        val info = formatProcessInfo(process)
        process.destroy()
        if (process.isAlive) {
            process.destroyForcibly()
        }

        log.info("Exit | {}", info)
    }

    private fun destroyProcess(process: Process) {
        val info = formatProcessInfo(process.toHandle())

        process.children().forEach { destroyProcess(it) }

        process.destroy()
        try {
            if (!process.waitFor(config.shutdownWaitTime.seconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                process.waitFor(config.shutdownWaitTime.seconds, TimeUnit.SECONDS)
            }
            cleanUp()

            log.info("Exit | {}", info)
        } catch (e: InterruptedException) {
            log.error("Interrupted while waiting for chrome process to shutdown", e)
            process.destroyForcibly()
        } finally {
        }
    }

    private fun formatProcessInfo(process: ProcessHandle): String {
        val info = process.info()
        val user = info.user().orElse("")
        val pid = process.pid()
        val ppid = process.parent().orElseGet { null }?.pid()?.toString()?:"?"
        val startTime = info.startInstant().orElse(null)
        val cpuDuration = info.totalCpuDuration()?.orElse(null)
        val cmdLine = info.commandLine().orElseGet { "" }

        return String.format("%-8s %-6d %-6s %-25s %-10s %s", user, pid, ppid, startTime?:"", cpuDuration?:"", cmdLine)
    }

    private fun close(thread: Thread) {
        try {
            thread.join(config.threadWaitTime.toMillis())
        } catch (ignored: InterruptedException) {}
    }

    private fun prepareUserDataDir() {
        val lock = AppPaths.BROWSER_TMP_DIR_LOCK
        val backupUserDataDir = AppPaths.CHROME_DATA_BACKUP_DIR
        if (Files.exists(backupUserDataDir)) {
            FileChannel.open(lock, StandardOpenOption.APPEND).use {
                it.lock()

                if (!Files.exists(userDataDir.resolve("Default"))) {
                    log.info("User data dir does not exist, copy from backup | {} <- {}", userDataDir, backupUserDataDir)
                    FileUtils.copyDirectory(backupUserDataDir.toFile(), userDataDir.toFile())
                } else {
                    Files.deleteIfExists(userDataDir.resolve("Default/Cookies"))
                    FileUtils.deleteDirectory(userDataDir.resolve("Default/Local Storage/leveldb").toFile())
                    arrayOf("Default/Cookies", "Default/Local Storage/leveldb").forEach {
                        Files.copy(backupUserDataDir.resolve(it), userDataDir.resolve(it), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    private fun cleanUp() {
        kotlin.runCatching {
            // wait for 1 second hope every thing is clean
            Thread.sleep(1000)

            val target = userDataDir.resolve("Default/Cache")
            FileUtils.deleteQuietly(target.toFile())
            if (Files.exists(target)) {
                log.warn("Failed to delete browser cache, try again | {}", target)
                forceDeleteDirectory(target)

                if (Files.exists(target)) {
                    log.error("Can not delete browser cache | {}", target)
                }
            }
        }
    }

    /**
     * Force delete all browser data
     * */
    private fun forceDeleteDirectory(dirToDelete: Path) {
        // TODO: delete data only if they contain privacy data, cookies, sessions, local storage, etc
        synchronized(ChromeLauncher::class.java) {
            // TODO: should use a better file lock
            val lock = AppPaths.BROWSER_TMP_DIR_LOCK

            val maxTry = 10
            var i = 0
            while (i++ < maxTry && Files.exists(dirToDelete)) {
                FileChannel.open(lock, StandardOpenOption.APPEND).use {
                    it.lock()
                    kotlin.runCatching { FileUtils.deleteDirectory(dirToDelete.toFile()) }
                            .onFailure { log.warn("Unexpected exception", it) }
                }

                Thread.sleep(500)
            }

            require(Files.exists(lock))
        }
    }

    interface ShutdownHookRegistry {
        fun register(thread: Thread) {
            Runtime.getRuntime().addShutdownHook(thread)
        }

        fun remove(thread: Thread) {
            Runtime.getRuntime().removeShutdownHook(thread)
        }
    }

    class RuntimeShutdownHookRegistry : ShutdownHookRegistry
}
