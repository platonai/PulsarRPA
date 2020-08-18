package ai.platon.pulsar.common

import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

object Runtimes {
    private val logger = LoggerFactory.getLogger(Runtimes::class.java)

    fun exec(name: String): List<String> {
        if (!SystemUtils.IS_OS_LINUX) {
            System.err.println("Only available in linux")
            return listOf()
        }

        val lines = mutableListOf<String>()
        try {
            val p = Runtime.getRuntime().exec(name)
            val input = BufferedReader(InputStreamReader(p.inputStream))
            input.lines().toList().toCollection(lines)
            input.close()
        } catch (err: Exception) {
            err.printStackTrace()
        }

        return lines
    }

    fun countSystemProcess(pattern: String): Int {
        if (!SystemUtils.IS_OS_LINUX) {
            System.err.println("Only available in linux")
            return 0
        }

        return exec("ps -ef").filter { it.contains(pattern.toRegex()) }.count()
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
