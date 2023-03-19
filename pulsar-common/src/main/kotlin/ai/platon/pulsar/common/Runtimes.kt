package ai.platon.pulsar.common

import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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

    fun locateBinary(executable: String): List<String> {
        val command = when {
            SystemUtils.IS_OS_WINDOWS -> "where $executable"
            SystemUtils.IS_OS_LINUX -> "whereis $executable"
            else -> return listOf()
        }

        return exec(command)
            .filter { it.contains(File.pathSeparatorChar) }
            .filter { it.contains(executable) }
            .flatMap { it.split(" ") }
            .filter { Files.exists(Paths.get(it)) }
    }

    fun countSystemProcess(pattern: String): Int {
        val command = when {
            SystemUtils.IS_OS_WINDOWS -> "tasklist /NH"
            SystemUtils.IS_OS_LINUX -> "ps -ef"
            else -> return 0
        }
        return exec(command).count { it.contains(pattern.toRegex()) }
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

    fun destroyProcessForcibly(pid: Int) {
        if (pid <= 0) {
            return
        } else if (SystemUtils.IS_OS_LINUX) {
            exec("kill -9 $pid")
        } else if (SystemUtils.IS_OS_WINDOWS) {
            exec("taskkill /F /PID $pid")
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

/**
 * The process launcher
 * */
object ProcessLauncher {
    private val log = LoggerFactory.getLogger(ProcessLauncher::class.java)

    @Throws(IOException::class)
    fun launch(executable: String, args: List<String>): Process {
        val command = mutableListOf<String>().apply { add(executable); addAll(args) }
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
