package ai.platon.pulsar.skeleton.common.proxy

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.proxy.ProxyParser
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.session.PulsarSession
import java.net.Proxy
import java.time.Duration
import java.time.Instant

class UniversalProxyParser(
    private val session: PulsarSession = PulsarContexts.getOrCreateSession()
) : ProxyParser() {
    private val logger = getLogger(this)
    private val prompt = """
Extract proxies from the text, and return them in JSON format:

```json
{
    status: "the status of the response, it can be one of the following: [success, failure]",
    host: "the extracted host, it can be an IP address or a domain name",
    port: "the extracted port, it should be an integer",
}
```

Your response should contains ONLY the JSON object, and nothing else.

    """.trimIndent()

    override val name = "UniversalProxyParser"

    /**
     * TODO: parse multiple proxies
     * */
    override fun parse(text: String, format: String): List<ProxyEntry> {
        val response = session.chat(prompt, text).content
        if (response == "LLM not available") {
            logger.warn(response)
            return listOf()
        }

        logger.info("LLM response: $response")

        val jsonText = response.substringAfter("```json").substringBeforeLast("```")

        val json = pulsarObjectMapper().readTree(jsonText)

        if (json.has("status") && json.has("host") && json.has("port")) {
            val status = json.get("status").asText()
            val host = json.get("host").asText()
            val port = json.get("port").asText()
            val type = Proxy.Type.SOCKS
            val declaredTTL = Instant.now() + Duration.ofMinutes(30)

            if (status == "success" && Strings.isNumericLike(port)) {
                val proxyEntry = ProxyEntry(host, port.toInt(), type = type).also {
                    it.declaredTTL = declaredTTL
                }
                return listOf(proxyEntry)
            } else {
                logger.warn("Invalid proxy entry: $response")
            }
        } else {
            logger.warn("Failed to extract proxy entry: $response")
        }
        return listOf()
    }
}
