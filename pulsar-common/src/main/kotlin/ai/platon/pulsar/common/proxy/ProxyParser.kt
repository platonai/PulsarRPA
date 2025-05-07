package ai.platon.pulsar.common.proxy

import java.nio.file.Files
import java.nio.file.Path

abstract class ProxyParser {
    abstract val name: String

    abstract fun parse(text: String, format: String = "auto"): List<ProxyEntry>

    open fun parse(path: Path, format: String): List<ProxyEntry> = parse(Files.readString(path), format)
}
