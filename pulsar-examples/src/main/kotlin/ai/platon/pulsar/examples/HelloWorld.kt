package ai.platon.pulsar.examples

import ai.platon.pulsar.common.BrowserControl
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
import java.nio.file.Paths

fun main() {
    val conf = ImmutableConfig()
    val bc = BrowserControl(conf)
    val js = bc.parseLibJs()
    FileUtils.write(Paths.get("/tmp/1.js").toFile(), js)
}
