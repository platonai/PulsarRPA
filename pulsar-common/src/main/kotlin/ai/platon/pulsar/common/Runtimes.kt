package ai.platon.pulsar.common

import org.apache.commons.io.IOUtils
import java.nio.charset.Charset

object Runtimes {
    fun exec() {
        val p = ProcessBuilder("cat", "/etc/something").start()
        val stderr = IOUtils.toString(p.errorStream, Charset.defaultCharset())
        // val stdout = IOUtils.toString(p.inputStream, Charset.defaultCharset())
    }
}
