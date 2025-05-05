package ai.platon.pulsar.common.proxy

import java.nio.file.Files
import java.nio.file.Path

abstract class ProxyParser {
    companion object {
        const val IPADDRESS_PATTERN =
            "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
    }

    abstract val name: String

    open val providerDescription = ""

    abstract fun parse(text: String, format: String = "auto"): List<ProxyEntry>

    open fun parse(path: Path, format: String): List<ProxyEntry> = parse(Files.readString(path), format)
}
