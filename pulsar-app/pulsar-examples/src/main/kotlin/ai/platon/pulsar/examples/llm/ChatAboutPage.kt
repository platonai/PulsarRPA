package ai.platon.pulsar.examples.llm

import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts

/**
 * Demonstrates chat with a webpage.
 * */
fun main() {
    // Change to your own LLM name and api key
    // see https://console.volcengine.com/ark/region:ark+cn-beijing/endpoint
    val apiKey = System.getProperty("llm.apiKey")
    PulsarSettings()
        .withLLMProvider("volcengine")
        .withLLMName("ep-20250218132011-2scs8")
        .withLLMAPIKey(apiKey)

    // Or use config file under $PULSAR_HOME/config/conf-enabled
    // You can find the template config files here:
    // https://github.com/platonai/PulsarRPA/blob/master/docs/config/llm/template

    // Create a pulsar session
    val session = PulsarContexts.createSession()
    // The main url we are playing with
    val url = "https://www.amazon.com/dp/B0C1H26C46"

    val page = session.load(url)
    val document = session.parse(page)

    var response = session.chat("Tell me a joke about programming . . .")
    println(response)

    response = session.chat("Tell me something about this webpage", document)
    println(response)
}
