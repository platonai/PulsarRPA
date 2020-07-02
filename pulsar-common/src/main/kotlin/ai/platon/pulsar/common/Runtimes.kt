package ai.platon.pulsar.common

import org.apache.commons.lang3.SystemUtils
import java.io.BufferedReader
import java.io.InputStreamReader

object Runtimes {

    fun countSystemProcess(name: String): Int {
        if (!SystemUtils.IS_OS_LINUX) {
            System.err.println("Only available in linux")
            return 0
        }

        var count = 0
        try {
            val p = Runtime.getRuntime().exec("ps -few | grep -c $name")
            val input = BufferedReader(InputStreamReader(p.inputStream))
            var line: String? = null
            while (input.readLine().also { line = it } != null) {
                count = Strings.getFirstInteger(line, 0)
            }
            input.close()
        } catch (err: Exception) {
            err.printStackTrace()
        }

        return count
    }
}
