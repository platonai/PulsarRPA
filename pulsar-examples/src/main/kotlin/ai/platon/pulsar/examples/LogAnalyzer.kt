package ai.platon.pulsar.examples

import java.nio.file.Files
import java.nio.file.Paths

class LogAnalyzer {
}

fun main() {
    val logDir = "/home/vincent/workspace/platon/logs"
    val logFile = "pulsar-test.log"
    val logPath = Paths.get(logDir, logFile)
    Files.readAllLines(logPath)
}
