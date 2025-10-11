package ai.platon.pulsar.common

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration

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

    /**
     * Waits for DevTools server is up on chrome process.
     *
     * @param process Chrome process.
     */
    fun waitFor(process: Process): String {
        val processOutput = StringBuilder()
        val readLineThread = Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String
                while (reader.readLine().also { line = it } != null) {
                    processOutput.appendLine(line)
                }
            }
        }
        readLineThread.start()

        try {
            readLineThread.join(Duration.ofMinutes(1).toMillis())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        return processOutput.toString()
    }
}