package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.impl.Chrome
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeProcessTimeoutException
import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.ProcessLauncher
import ai.platon.pulsar.common.Runtimes
import ai.platon.pulsar.common.browser.Browsers
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.regex.Pattern

/**
 * The chrome launcher
 * */
class ChromeLauncher(
    private val userDataDir: Path = BrowserSettings.defaultUserDataDir(),
    private val shutdownHookRegistry: ShutdownHookRegistry = RuntimeShutdownHookRegistry(),
    private val options: LauncherOptions = LauncherOptions("CHROME")
) : AutoCloseable {

    companion object {
        private val DEVTOOLS_LISTENING_LINE_PATTERN = Pattern.compile("^DevTools listening on ws:\\/\\/.+:(\\d+)\\/")

        fun enableDebugChromeOutput() {
//            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
//            loggerContext.getLogger("com.github.kklisura.cdt.launch.chrome.output").level = Level.DEBUG
        }
    }

    private val log = LoggerFactory.getLogger(ChromeLauncher::class.java)
    private var process: Process? = null
    private val shutdownHookThread = Thread { this.close() }

    /**
     * Launch the chrome
     * */
    fun launch(chromeBinaryPath: Path, options: ChromeOptions): RemoteChrome {
        // if the data dir is the default dir, we might have problem to prepare user dir
        if ("context\\default--" !in userDataDir.toString()) {
        }
        kotlin.runCatching { prepareUserDataDir() }.onFailure {
            log.warn("Failed to prepare user data dir", it)
        }

        val port = launchChromeProcess(chromeBinaryPath, userDataDir, options)
        return Chrome(port)
    }

    /**
     * Launch the chrome
     * */
    fun launch(options: ChromeOptions) = launch(Browsers.searchChromeBinary(), options)

    /**
     * Launch the chrome
     * */
    fun launch(headless: Boolean) = launch(Browsers.searchChromeBinary(), ChromeOptions().also { it.headless = headless })

    /**
     * Launch the chrome
     * */
    fun launch() = launch(true)

    override fun close() {
        val p = process ?: return
        this.process = null
        if (p.isAlive) {
            Runtimes.destroyProcess(p, options.shutdownWaitTime)
            kotlin.runCatching { shutdownHookRegistry.remove(shutdownHookThread) }
                    .onFailure { log.warn("Unexpected exception", it) }
        }

        // if the data dir is the default dir, we might have problem to clean up
        if (!userDataDir.toString().contains("context\\default", true)) {
            kotlin.runCatching { cleanUp() }.onFailure {
                log.warn("Failed to clear user data dir | {} | {}", userDataDir, it.message)
            }
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
     * Launches a chrome process given a chrome binary and its arguments.
     *
     * Launching chrome processes is CPU consuming, so we do this in a synchronized manner
     *
     * @param chromeBinary Chrome binary path.
     * @param userDataDir Chrome user data dir.
     * @param chromeOptions Chrome arguments.
     * @return Port on which devtools is listening.
     * @throws IllegalStateException If chrome process has already been started.
     * @throws ChromeProcessException If an I/O error occurs during chrome process start.
     * @throws ChromeProcessTimeoutException If timeout expired while waiting for chrome to start.
     */
    @Throws(ChromeProcessException::class, IllegalStateException::class, ChromeProcessTimeoutException::class)
    @Synchronized
    private fun launchChromeProcess(chromeBinary: Path, userDataDir: Path, chromeOptions: ChromeOptions): Int {
        check(!isAlive) { "Chrome process has already been started" }
        var supervisorProcess = options.supervisorProcess
        if (supervisorProcess != null && Runtimes.locateBinary(supervisorProcess).isEmpty()) {
            log.warn("Supervisor program {} can not be located", options.supervisorProcess)
            supervisorProcess = null
        }

        val executable = supervisorProcess?:"$chromeBinary"
        var arguments = if (supervisorProcess == null) chromeOptions.toList() else {
            options.supervisorProcessArgs + arrayOf("$chromeBinary") + chromeOptions.toList()
        }
        arguments += " --user-data-dir=$userDataDir"

        return try {
            shutdownHookRegistry.register(shutdownHookThread)
            process = ProcessLauncher.launch(executable, arguments)
            process?.also {
                Files.createDirectories(userDataDir)
                val pidPath = userDataDir.resolveSibling("chromeLauncher.pid")
                Files.writeString(pidPath, it.pid().toString(), StandardOpenOption.CREATE)
            }
            waitForDevToolsServer(process!!)
        } catch (e: IOException) {
            // Unsubscribe from registry on exceptions.
            shutdownHookRegistry.remove(shutdownHookThread)
            throw ChromeProcessException("Failed to start chrome", e)
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
                    processOutput.appendLine(line)
                }
            }
        }
        readLineThread.start()

        try {
            readLineThread.join(options.startupWaitTime.toMillis())

            if (port == 0) {
                close(readLineThread)
                log.info("Process output:>>>\n$processOutput\n<<<")
                throw ChromeProcessTimeoutException("Timeout to waiting for chrome to start")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.error("Interrupted while waiting for devtools server, close it", e)
            close(readLineThread)
        }

        return port
    }

    private fun close(thread: Thread) {
        try {
            thread.join(options.threadWaitTime.toMillis())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    @Throws(IOException::class)
    private fun prepareUserDataDir() {
        val prototypeUserDataDir = AppPaths.CHROME_DATA_DIR_PROTOTYPE
        if (userDataDir == prototypeUserDataDir || userDataDir.toString().contains("/default/")) {
            log.info("Running chrome with prototype/default data dir, no cleaning | {}", userDataDir)
            return
        }

        val lock = AppPaths.BROWSER_TMP_DIR_LOCK
        if (Files.exists(prototypeUserDataDir.resolve("Default"))) {
            FileChannel.open(lock, StandardOpenOption.APPEND).use {
                it.lock()

                if (!Files.exists(userDataDir.resolve("Default"))) {
                    log.info("User data dir does not exist, copy from prototype | {} <- {}", userDataDir, prototypeUserDataDir)
                    // remove dead symbolic links
                    Files.list(prototypeUserDataDir).filter { Files.isSymbolicLink(it) && !Files.exists(it) }.forEach { Files.delete(it) }
                    FileUtils.copyDirectory(prototypeUserDataDir.toFile(), userDataDir.toFile())
                } else {
                    Files.deleteIfExists(userDataDir.resolve("Default/Cookies"))
                    val leveldb = userDataDir.resolve("Default/Local Storage/leveldb")
                    if (Files.exists(leveldb)) {
                        FileUtils.deleteDirectory(leveldb.toFile())
                    }

                    arrayOf("Default/Cookies", "Default/Local Storage/leveldb").forEach {
                        val target = userDataDir.resolve(it)
                        Files.createDirectories(target.parent)
                        Files.copy(prototypeUserDataDir.resolve(it), target, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun cleanUp() {
        val target = userDataDir

        // delete user data dir only if it's in the system tmp dir
        if (target.startsWith(AppPaths.SYS_TMP_DIR)) {
            FileUtils.deleteQuietly(target.toFile())
            if (!SystemUtils.IS_OS_WINDOWS && Files.exists(target)) {
                log.warn("Failed to delete browser cache, try again | {}", target)
                forceDeleteDirectory(target)

                if (Files.exists(target)) {
                    log.error("Could not delete browser cache | {}", target)
                }
            }
        }
    }

    /**
     * Force delete all browser data
     * */
    @Throws(IOException::class)
    private fun forceDeleteDirectory(dirToDelete: Path) {
        synchronized(ChromeLauncher::class.java) {
            val lock = AppPaths.BROWSER_TMP_DIR_LOCK

            val maxTry = 10
            var i = 0
            while (i++ < maxTry && Files.exists(dirToDelete) && !Files.isSymbolicLink(dirToDelete)) {
                FileChannel.open(lock, StandardOpenOption.APPEND).use {
                    it.lock()
                    kotlin.runCatching { FileUtils.deleteDirectory(dirToDelete.toFile()) }
                            .onFailure { log.warn("Failed to delete directory | {} | {}",
                                dirToDelete, it.message)
                            }
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
            // TODO: java.lang.IllegalStateException: Shutdown in progress
            // Runtime.getRuntime().removeShutdownHook(thread)
        }
    }

    class RuntimeShutdownHookRegistry : ShutdownHookRegistry
}
