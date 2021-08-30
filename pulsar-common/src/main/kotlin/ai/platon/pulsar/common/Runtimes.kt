package ai.platon.pulsar.common

import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

object Runtimes {
    private val logger = LoggerFactory.getLogger(Runtimes::class.java)

    fun exec(name: String): List<String> {
        try {
            val process = Runtime.getRuntime().exec(name)
            return process.inputStream.bufferedReader().useLines { it.toList() }
        } catch (err: Exception) {
            err.printStackTrace()
        }

        return listOf()
    }

    fun locateBinary(executable: String): String? {
        val command = when {
            SystemUtils.IS_OS_WINDOWS -> "where $executable"
            SystemUtils.IS_OS_LINUX -> "whereis $executable"
            else -> return null
        }
        return exec(command)
            .filter { it.contains(File.pathSeparatorChar) }
            .find { it.contains(executable) }
    }

    fun countSystemProcess(pattern: String): Int {
        val command = when {
            SystemUtils.IS_OS_WINDOWS -> "tasklist /NH"
            SystemUtils.IS_OS_LINUX -> "ps -ef"
            else -> return 0
        }
        return exec(command).filter { it.contains(pattern.toRegex()) }.count()
    }

    fun checkIfProcessRunning(pattern: String): Boolean {
        return countSystemProcess(pattern) > 0
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

    fun formatProcessInfo(process: ProcessHandle): String {
        val info = process.info()
        val user = info.user().orElse("")
        val pid = process.pid()
        val ppid = process.parent().orElseGet { null }?.pid()?.toString()?:"?"
        val startTime = info.startInstant().orElse(null)
        val cpuDuration = info.totalCpuDuration()?.orElse(null)
        val cmdLine = info.commandLine().orElseGet { "" }

        return String.format("%-8s %-6d %-6s %-25s %-10s %s", user, pid, ppid, startTime?:"", cpuDuration?:"", cmdLine)
    }

    fun deleteBrokenSymbolicLinks(directory: Path) {
        if (SystemUtils.IS_OS_LINUX) {
            exec("find -L $directory -type l -delete")
        }
    }

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
