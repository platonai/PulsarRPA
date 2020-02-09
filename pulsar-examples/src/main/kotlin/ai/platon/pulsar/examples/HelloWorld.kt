package ai.platon.pulsar.examples

import ai.platon.pulsar.net.browser.WebDriverControl
import ai.platon.pulsar.common.config.ImmutableConfig
import org.apache.commons.io.FileUtils
import java.nio.file.Paths

fun main() {
    val conf = ImmutableConfig()
    val bc = WebDriverControl(conf)
    val js = bc.parseLibJs()
    FileUtils.write(Paths.get("/tmp/1.js").toFile(), js)
}
