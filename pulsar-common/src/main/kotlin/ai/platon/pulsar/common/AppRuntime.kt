package ai.platon.pulsar.common

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

object AppRuntime {

    private val log = LoggerFactory.getLogger(AppRuntime::class.java)

    fun checkIfProcessRunning(regex: String): Boolean {
        try {
            val proc = Runtime.getRuntime().exec("ps -ef")
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            var line: String
            while (reader.readLine().also { line = it } != null) {
                if (line.matches(regex.toRegex())) {
                    return true
                }
            }
        } catch (e: Exception) {
            log.error(e.toString())
        }
        return false
    }
}
