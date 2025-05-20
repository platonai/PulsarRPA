package ai.platon.pulsar.common

import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.common.measure.ByteUnit
import kotlinx.coroutines.delay
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.awt.GraphicsEnvironment
import java.awt.HeadlessException
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.*
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.swing.JFrame
import kotlin.random.Random


/**
 * Runtime utility
 * */
object Runtimes {
    private val logger = LoggerFactory.getLogger(Runtimes::class.java)
    private val heavyOperationResultCache = ConcurrentExpiringLRUCache<String, Any>(ttl = Duration.ofSeconds(10))

    fun exec(name: String): List<String> {
        try {
            val process = Runtime.getRuntime().exec(name)
            return process.inputStream.bufferedReader().useLines { it.toList() }
        } catch (err: Exception) {
            err.printStackTrace()
        }

        return listOf()
    }

    fun locateBinary(executable: String): List<String> {
        val command = when {
            SystemUtils.IS_OS_WINDOWS -> "where $executable"
            SystemUtils.IS_OS_LINUX -> "whereis $executable"
            // TODO: more OS support
            else -> "whereis $executable"
        }

        return exec(command).asSequence()
            .filter { it.contains(File.pathSeparatorChar) }
            .filter { it.contains(executable) }
            .flatMap { it.split(" ") }
            .filter { Files.exists(Paths.get(it)) }
            .toList()
    }

    fun countSystemProcess(namePattern: String): Int {
        val command = when {
            SystemUtils.IS_OS_WINDOWS -> "tasklist /NH"
            SystemUtils.IS_OS_LINUX -> "ps -ef"
            // TODO: more OS support
            else -> "ps -ef"
        }
        return exec(command).count { it.contains(namePattern.toRegex()) }
    }

    fun checkIfProcessRunning(pattern: String): Boolean {
        return countSystemProcess(pattern) > 0
    }

    fun listAllChromeProcesses(): List<String> {
        return when {
            SystemUtils.IS_OS_WINDOWS -> listAllChromeProcessesOnWindows()
            SystemUtils.IS_OS_LINUX -> listAllChromeProcessesOnPosix()
            SystemUtils.IS_OS_MAC -> listAllChromeProcessesOnPosix()
            else -> listOf()
        }
    }

    fun listAllChromeProcessesOnPosix(): List<String> {
        if (!SystemUtils.IS_OS_LINUX) {
            return listOf()
        }

        val result = mutableListOf<String>()
        try {
            // Command to list all Chrome processes
            val command = "ps -ef | grep -i 'chrome' | grep -v 'grep'"

            // Execute the command
            val process = Runtime.getRuntime().exec(arrayOf("bash", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            // Read and print each line of the output
            var line: String?
            // println("Running Chrome Processes:")
            while ((reader.readLine().also { line = it }) != null) {
                line?.let { result.add(it) }
            }

            // Wait for the process to complete
            process.waitFor()
        } catch (e: java.lang.Exception) {
            System.err.println("An error occurred: " + e.message)
        }

        return result
    }

    fun listAllChromeProcessesOnWindows(): List<String> {
        if (!SystemUtils.IS_OS_WINDOWS) {
            return listOf()
        }

        val result = mutableListOf<String>()
        try {
            // Command to list all Chrome processes
            val command = "tasklist | findstr chrome && tasklist | findstr chromium"

            // Execute the command
            val process = Runtime.getRuntime().exec(arrayOf("cmd.exe", "/c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            // Read and print each line of the output
            var line: String?
            // println("Running Chrome Processes:")
            while ((reader.readLine().also { line = it }) != null) {
                line?.let { result.add(it) }
            }

            // Wait for the process to complete
            process.waitFor()
        } catch (e: java.lang.Exception) {
            System.err.println("An error occurred: " + e.message)
        }

        return result
    }

    fun destroyProcess(process: Process, shutdownWaitTime: Duration) {
        val info = formatProcessInfo(process.toHandle())

        process.children().forEach { destroyChildProcess(it) }

        process.destroy()
        try {
            if (!process.waitFor(shutdownWaitTime.seconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                process.waitFor(shutdownWaitTime.seconds, TimeUnit.SECONDS)
            }

            logger.info("Exit | {}", info)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            process.destroyForcibly()
            throw e
        } finally {
        }
    }

    fun destroyProcessForcibly(pid: Int) {
        if (pid <= 0) {
            return
        } else if (SystemUtils.IS_OS_WINDOWS) {
            exec("taskkill /F /PID $pid")
        } else if (SystemUtils.IS_OS_LINUX) {
            exec("kill -9 $pid")
        } else {
            // TODO: more OS support
            exec("kill -9 $pid")
        }
    }

    fun destroyProcessForcibly(namePattern: String) {
        try {
            val command = if (SystemUtils.IS_OS_WINDOWS) {
                // Windows
                "taskkill /F /IM $namePattern"
            } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
                // macOS or Linux
                "pkill -f $namePattern"
            } else {
                logger.info("Unsupported operating system")
                return
            }

            // Execute the command
            val process = Runtime.getRuntime().exec(command)
            process.waitFor()

            // Check if the command executed successfully
            if (process.exitValue() == 0) {
                logger.info("All Chrome processes have been terminated")
            }
        } catch (e: Exception) {
            logger.info("Failed to terminate Chrome processes", e)
        }
    }

    fun formatProcessInfo(process: ProcessHandle): String {
        val info = process.info()
        val user = info.user().orElse("")
        val pid = process.pid()
        val ppid = process.parent().orElseGet { null }?.pid()?.toString() ?: "?"
        val startTime = info.startInstant().orElse(null)
        val cpuDuration = info.totalCpuDuration()?.orElse(null)
        val cmdLine = info.commandLine().orElseGet { "" }

        return String.format(
            "%-8s %-6d %-6s %-25s %-10s %s",
            user,
            pid,
            ppid,
            startTime ?: "",
            cpuDuration ?: "",
            cmdLine
        )
    }

    fun deleteBrokenSymbolicLinks(symbolicLink: Path) {
        if (SystemUtils.IS_OS_WINDOWS) {
            // TODO: use command line
            Files.deleteIfExists(symbolicLink)
        } else if (SystemUtils.IS_OS_LINUX) {
            exec("find -L $symbolicLink -type l -delete")
        } else {
            // TODO: more OS support
        }
    }

    suspend fun randomDelay(timeMillis: Long, delta: Int) {
        delay(timeMillis + Random.nextInt(delta))
    }

    /**
     * Return the number of unallocated bytes of each file stores
     * */
    fun unallocatedDiskSpaces(): List<Long> {
        return try {
            FileSystems.getDefault().fileStores
                .filter { ByteUnit.BYTE.toGB(totalSpaceOr0(it)) > 20 }
                .map { unallocatedSpaceOr0(it) }
                .filter { it > 0 }
        } catch (e: Throwable) {
            return listOf()
        }
    }

    fun isRunningInDocker(): Boolean {
        return heavyOperationResultCache.computeIfAbsent("isRunningInDocker") { isRunningInDockerRT() } == true
    }

    /**
     * Check if the current process is running in Docker
     * */
    fun isRunningInDockerRT(): Boolean {
        // Check for /.dockerenv file
        if (File("/.dockerenv").exists()) {
            return true
        }
        // Check for 'docker' or 'kubepods' in /proc/1/cgroup
        return try {
            Files.readAllLines(Paths.get("/proc/1/cgroup")).any {
                it.contains("docker") || it.contains("kubepods")
            }
        } catch (e: Exception) {
            false
        }
    }

    fun supportHeadedBrowser(): Boolean {
        return heavyOperationResultCache.computeIfAbsent("supportHeadedChromium") { supportHeadedChromiumRT() } == true
    }

    fun supportHeadedChromiumRT(): Boolean {
        return when {
            isRunningInDocker() -> false
            SystemUtils.IS_OS_WINDOWS -> true
            SystemUtils.IS_OS_LINUX -> hasXGraphicalInterface()
            else -> isGUIAvailable()
        }
    }

    fun hasOnlyHeadlessBrowser(): Boolean {
        return !supportHeadedBrowser()
    }

    fun isGUIAvailable(): Boolean {
        return heavyOperationResultCache.computeIfAbsent("isGUIAvailable") { isGUIAvailableRT() } == true
    }

    fun isGUIAvailableRT(): Boolean {
        // First check: Java headless mode
        if (GraphicsEnvironment.isHeadless()) {
            return false
        }

        // Third check: Try to create a Swing window (safe fallback)
        return try {
            JFrame().apply { isVisible = false; dispose() }
            true
        } catch (e: HeadlessException) {
            false
        } catch (e: Exception) {
            false // In case of unexpected GUI-related errors
        }
    }

    fun hasXGraphicalInterface(): Boolean {
        // 方法 1: 检查 DISPLAY 环境变量
        val display = System.getenv("DISPLAY")
        if (!display.isNullOrEmpty()) {
            logger.info("Detected DISPLAY environment variable: $display")
            return true
        }

        // 方法 2: 检查 Xorg 是否安装
        val xorgPath = File("/usr/bin/Xorg")
        if (xorgPath.exists()) {
            logger.info("Xorg is installed at: ${xorgPath.path}")
            return true
        }

        // 方法 3: 检查常见桌面环境进程是否运行
        val desktopProcesses = listOf("gnome-session", "kdeinit", "xfce4-session")
        for (process in desktopProcesses) {
            if (checkIfProcessRunning(process)) {
                // println("Detected running desktop environment process: $process")
                return true
            }
        }

        // 如果所有检查都失败，则认为没有图形化界面
        logger.info("No graphical interface detected.")
        return false
    }

    private fun totalSpaceOr0(store: FileStore) = store.runCatching { totalSpace }.getOrNull() ?: 0L

    private fun unallocatedSpaceOr0(store: FileStore) = store.runCatching { unallocatedSpace }.getOrNull() ?: 0L

    private fun destroyChildProcess(process: ProcessHandle) {
        process.children().forEach { destroyChildProcess(it) }

        val info = formatProcessInfo(process)
        process.destroy()
        if (process.isAlive) {
            process.destroyForcibly()
        }

        logger.debug("Exit | {}", info)
    }
}

