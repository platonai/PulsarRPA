package ai.platon.pulsar.common

import org.apache.commons.lang3.SystemUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.streams.toList

object Runtimes {

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
}
