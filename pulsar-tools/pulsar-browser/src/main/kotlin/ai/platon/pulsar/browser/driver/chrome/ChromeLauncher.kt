package ai.platon.pulsar.browser.driver.chrome

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.BrowserSettings.Companion
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.browser.driver.chrome.impl.ChromeImpl
import ai.platon.pulsar.browser.driver.chrome.util.ChromeLaunchException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeLaunchTimeoutException
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.browser.BrowserFiles
import ai.platon.pulsar.common.browser.BrowserFiles.PID_FILE_NAME
import ai.platon.pulsar.common.browser.BrowserFiles.PORT_FILE_NAME
import ai.platon.pulsar.common.browser.Browsers
import ai.platon.pulsar.common.concurrent.RuntimeShutdownHookRegistry
import ai.platon.pulsar.common.concurrent.ShutdownHookRegistry
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.channels.FileChannel
import java.nio.channels.FileLockInterruptionException
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.Charset
import java.nio.file.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * The chrome launcher
 * */
class ChromeLauncher constructor(
    val userDataDir: Path = BrowserFiles.computeNextSequentialContextDir(),
    val options: LauncherOptions = LauncherOptions(),
    private val shutdownHookRegistry: ShutdownHookRegistry = RuntimeShutdownHookRegistry()
) : AutoCloseable {

    companion object {
        private val logger = LoggerFactory.getLogger(ChromeLauncher::class.java)

        private val DEVTOOLS_LISTENING_LINE_PATTERN = Pattern.compile("^DevTools listening on ws://.+:(\\d+)/")
    }

    private val closed = AtomicBoolean()
    private val pidPath get() = userDataDir.resolveSibling(PID_FILE_NAME)
    private val portPath get() = userDataDir.resolveSibling(PORT_FILE_NAME)
    private val temporaryUddExpiry = BrowserFiles.TEMPORARY_UDD_EXPIRY
    // The number of recent temporary user data directories to keep, the browser has to be closed
    private val recentNToKeep = 10
    private var process: Process? = null

    private val isClosed get() = closed.get()
    private val isActive get() = AppContext.isActive && !Thread.currentThread().isInterrupted
    private val shutdownHookThread = Thread {
        // System.err.println("Shutting down chrome process ...")

        // the upper layer should also close the launcher
        if (!isClosed) {
            sleepSeconds(10)
            this.close()
        }
    }

    /**
     * Launches a Chrome process using the specified Chrome binary and options.
     *
     * This function prepares the user data directory and then launches the Chrome process with the given binary path and options.
     * If the preparation of the user data directory fails, a warning is logged but the process continues.
     * The function returns a [RemoteChrome] instance that represents the launched Chrome process.
     *
     * @param chromeBinaryPath The path to the Chrome binary executable.
     * @param options The Chrome options to be used when launching the Chrome process.
     * @return A [RemoteChrome] instance representing the launched Chrome process.
     * @throws ChromeLaunchException If an error occurs during the Chrome process launch.
     */
    @Throws(ChromeLaunchException::class)
    fun launch(chromeBinaryPath: Path, options: ChromeOptions): RemoteChrome {
        // Attempt to prepare the user data directory
        prepareUserDataDir()

        // Launch the Chrome process with the specified binary path, user data directory, and options.
        val port = launchChromeProcess(chromeBinaryPath, userDataDir, options)

        // Return a new instance of ChromeImpl initialized with port
        return ChromeImpl(port)
    }

    /**
     * Launch a chrome
     * */
    @Throws(ChromeLaunchException::class)
    fun launch(options: ChromeOptions) = launch(Browsers.searchChromeBinary(), options)

    /**
     * Launch a chrome
     * */
    @Throws(ChromeLaunchException::class)
    fun launch(headless: Boolean) =
        launch(Browsers.searchChromeBinary(), ChromeOptions().also { it.headless = headless })

    /**
     * Launch a chrome
     * */
    @Throws(ChromeLaunchException::class)
    fun launch() = launch(true)

    /**
     * Destroy the chrome process forcibly.
     * */
    fun destroyForcibly() {
        try {
            Files.deleteIfExists(portPath)
            val pid = Files.readAllLines(pidPath).firstOrNull { it.isNotBlank() }?.toIntOrNull() ?: 0
            if (pid > 0) {
                logger.warn("Destroy chrome launcher forcibly, pid: {} | {}", pid, userDataDir)
                Runtimes.destroyProcessForcibly(pid)
            }
        } catch (e: NoSuchFileException) {
            logger.warn("NoSuchFileException | {}", e.message)
        } catch (e: IOException) {
            logger.warn("IOException | {}", e.message)
        } catch (t: Throwable) {
            warnInterruptible(this, t, "Failed to destroy chrome launcher forcibly | {}", userDataDir)
        }
    }

    /**
     * Close the chrome process.
     * The method throws nothing by design.
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            // delete the port file to indicate that the chrome process can be killed and the resources can be cleaned up
            portPath.deleteIfExists()

            val p = process ?: return
            this.process = null
            if (p.isAlive) {
                Runtimes.destroyProcess(p, options.shutdownWaitTime)
                // TODO: java.lang.IllegalStateException: Shutdown in progress
//                kotlin.runCatching { shutdownHookRegistry.remove(shutdownHookThread) }
//                        .onFailure { logger.warn("Unexpected exception", it) }
            }

            BrowserFiles.runCatching {
                cleanUpContextTmpDir(temporaryUddExpiry)
                cleanOldestContextTmpDirs(120.seconds.toJavaDuration(), recentNToKeep)
            }.onFailure { warnForClose(this, it) }
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

    val shouldBeWorking: Boolean get() = portPath.exists()

    /**
     * Launches a chrome process given a chrome binary and its arguments.
     *
     * Launching chrome processes is CPU consuming, so we do this in a synchronized manner
     *
     * @param chromeBinary Chrome binary path.
     * @param userDataDir Chrome user data dir.
     * @param chromeOptions Chrome arguments.
     * @return Port on which devtools is listening.
     * @throws ChromeLaunchException If an error occurs during chrome process start.
     */
    @Throws(ChromeLaunchException::class)
    @Synchronized
    private fun launchChromeProcess(chromeBinary: Path, userDataDir: Path, chromeOptions: ChromeOptions): Int {
        if (!isActive) {
            return 0
        }

        check(process == null) { "Chrome process has already been started" }
        check(!isAlive) { "Chrome process has already been started" }

        var supervisorProcess = options.supervisorProcess
        if (supervisorProcess != null && Runtimes.locateBinary(supervisorProcess).isEmpty()) {
            logger.warn("Supervisor program {} can not be located", options.supervisorProcess)
            supervisorProcess = null
        }

        if (Runtimes.isHeadless()) {
            logger.info("The current environment has no GUI support, force to headless mode")
            chromeOptions.headless = true
        }

        val executable = supervisorProcess ?: "$chromeBinary"
        var arguments = if (supervisorProcess == null) chromeOptions.toList() else {
            options.supervisorProcessArgs + arrayOf("$chromeBinary") + chromeOptions.toList()
        }.toMutableList()

        if (userDataDir.startsWith(AppPaths.SYSTEM_DEFAULT_BROWSER_DATA_DIR_PLACEHOLDER)) {
            // Open the default browser just like a real user daily do,
            // open a blank page not to choose the profile
            val args = "--remote-debugging-port=0 --remote-allow-origins=* about:blank"
            arguments = args.split(" ").toMutableList()
        } else {
            arguments.add("--user-data-dir=$userDataDir")
        }

        return try {
            Files.createDirectories(portPath.parent)
            portPath.deleteIfExists()
            Files.writeString(portPath, "0", StandardOpenOption.CREATE)

            shutdownHookRegistry.register(shutdownHookThread)
            process = ProcessLauncher.launch(executable, arguments)

            val p = process ?: throw ChromeLaunchException("Failed to start chrome process")

            // write pid file to indicate the process is alive
            Files.writeString(pidPath, p.pid().toString(), StandardOpenOption.CREATE)

            val port = waitForDevToolsServer(p)

            // write port to indicate the process can be connected
            Files.writeString(portPath, port.toString(), StandardOpenOption.TRUNCATE_EXISTING)

            port
        } catch (e: IllegalStateException) {
            shutdownHookRegistry.remove(shutdownHookThread)
            close()
            throw ChromeLaunchException("IllegalStateException while trying to launch chrome", e)
        } catch (e: IOException) {
            // Unsubscribe from registry on exceptions.
            shutdownHookRegistry.remove(shutdownHookThread)
            close()
            throw ChromeLaunchException("IOException while trying to start chrome", e)
        } catch (e: Exception) {
            // Close the process if failed to start, it throws nothing by design.
            close()
            throw e
        }
    }

    /**
     * Waits for DevTools server is up on chrome process.
     *
     * @param process Chrome process.
     * @return DevTools listening port.
     * @throws ChromeLaunchTimeoutException If timeout expired while waiting for chrome process.
     */
    @Throws(ChromeLaunchTimeoutException::class)
    private fun waitForDevToolsServer(process: Process): Int {
        var port = 0
        val processOutput = StringBuilder()
        val readLineThread = Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                // Wait for DevTools listening line and extract port number.
//                var line: String? = reader.readLine()
                var line: String? = String(reader.readLine().toByteArray(Charset.defaultCharset()))
                while (line != null) {
                    if (line.isNotBlank()) {
                        logger.info("[output] - $line")
                    }

                    val matcher = DEVTOOLS_LISTENING_LINE_PATTERN.matcher(line)
                    if (matcher.find()) {
                        port = matcher.group(1).toInt()
                        break
                    }
                    processOutput.appendLine(line)

                    line = reader.readLine()
                }
            }
        }
        readLineThread.start()

        try {
            readLineThread.join(options.startupWaitTime.toMillis())

            if (port == 0) {
                close(readLineThread)
                logger.info("Process output:>>>\n$processOutput\n<<<")

                handleChromeFailedToStart()

                throw ChromeLaunchTimeoutException("Timeout to waiting for chrome to start | $userDataDir")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("Interrupted while waiting for devtools server, close it", e)
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

    private fun handleChromeFailedToStart() {
        val count = Runtimes.countSystemProcess("chrome")
        if (count == 0) {
            logger.warn("Failed to start Chrome, no chrome process running in the system")
            return
        }

        // val isSystemDefaultBrowser = userDataDir == AppPaths.SYSTEM_DEFAULT_BROWSER_DATA_DIR_PLACEHOLDER

        val message = """

===============================================================================
!!!   FAILED TO START CHROME   !!!

Failed to start Chrome programmatically, but there are already $count chrome
processes running in the system.

Kill all Chrome processes and run the program again.

===============================================================================

                    """.trimIndent()

        logger.warn(message)
        return
    }

    /**
     * Prepare user data dir.
     *
     * @throws IOException If failed to create user data dir.
     * */
    @Throws(IOException::class)
    private fun prepareUserDataDir() {
        try {
            prepareUserDataDir0()
        } catch (e: OverlappingFileLockException) {
            logger.warn("OverlappingFileLockException, rethrow | {} | \n{}", userDataDir, e.brief())
            throw ChromeLaunchException("Failed to prepare user data dir", e)
        } catch (e: FileLockInterruptionException) {
            logger.warn("FileLockInterruptionException, rethrow | {} | \n{}", userDataDir, e.brief())
            Thread.currentThread().interrupt()
            throw ChromeLaunchException("Failed to prepare user data dir", e)
        }
    }

    /**
     * Prepare user data dir.
     *
     * @throws FileLockInterruptionException – If the invoking thread is interrupted while blocked in this method
     * @throws OverlappingFileLockException – If a lock that overlaps the requested region is already held
     *      by this Java virtual machine, or if another thread is already blocked in this method and is
     *      attempting to lock an overlapping region of the same file
     * @throws IOException If failed to create user data dir.
     * */
    @Throws(FileLockInterruptionException::class, OverlappingFileLockException::class, IOException::class)
    private fun prepareUserDataDir0() {
        val prototypeUserDataDir = AppPaths.CHROME_DATA_DIR_PROTOTYPE
        if (userDataDir == prototypeUserDataDir || userDataDir.toString().contains("/default/")) {
            logger.info("Running chrome with prototype/default data dir, no cleaning | {}", userDataDir)
            return
        }

        // Lock the group so that only one instance can run at the same time
        val lock = BrowserFiles.getContextGroupLockFileFromUserDataDir(userDataDir)
        if (isActive && Files.exists(prototypeUserDataDir.resolve("Default"))) {
            FileChannel.open(lock, StandardOpenOption.APPEND).use {
                it.lock()

                if (!isActive) {
                    return
                }

                if (!Files.exists(userDataDir.resolve("Default"))) {
                    logger.info(
                        "User data dir does not exist, copy from prototype | {} <- {}",
                        userDataDir,
                        prototypeUserDataDir
                    )
                    // remove dead symbolic links
                    Files.list(prototypeUserDataDir)
                        .filter { Files.isSymbolicLink(it) && !Files.exists(it) }
                        .forEach { Files.delete(it) }

                    // ISSUE#29: https://github.com/platonai/PulsarRPA/issues/29
                    // Failed to copy chrome data dir when there is a SingletonSocket symbol link
                    val fileFilter = FileFilter { !Files.isSymbolicLink(it.toPath()) }

//                    val fileFilter = { f: File -> !Files.isSymbolicLink(f.toPath())
//                        // Copy only the default profile directory
//                        && f.name == "Default"
//                    }
                    FileUtils.copyDirectory(prototypeUserDataDir.toFile(), userDataDir.toFile(), fileFilter)
                } else {
                    handleExistUserDataDir(prototypeUserDataDir)
                }
            }
        }
    }

    private fun handleExistUserDataDir(prototypeUserDataDir: Path) {
        // the user data dir exists
        Files.deleteIfExists(userDataDir.resolve("Default/Cookies"))
        val leveldb = userDataDir.resolve("Default/Local Storage/leveldb")
        if (Files.exists(leveldb)) {
            // might have permission issue on Windows
            // FileUtils.deleteDirectory(leveldb.toFile())
        }

        arrayOf("Default/Cookies", "Default/Local Storage/leveldb").forEach {
            val target = userDataDir.resolve(it)
            Files.createDirectories(target.parent)
            val source = prototypeUserDataDir.resolve(it)
            if (Files.exists(source)) {
                // Files.copy(prototypeUserDataDir.resolve(it), target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
