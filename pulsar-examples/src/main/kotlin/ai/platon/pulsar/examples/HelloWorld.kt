package ai.platon.pulsar.examples

import ai.platon.pulsar.common.isBlankBody
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
//    val html = Files.readString(Paths.get("/tmp/ln/4f1cac6cb70d8087f9c2599a67c55853.htm"))
    val html = "....<body></body>...."
    val b = isBlankBody(html)
    println(b)
}
