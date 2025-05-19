package ai.platon.pulsar.examples.llm

import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts

/**
 * Demonstrates chat with a webpage.
 * */
fun main() {
    // !!! IMPORTANT !!!
    // CHANGE TO YOUR OWN LLM NAME AND API KEY
    //
    // see https://console.volcengine.com/ark/region:ark+cn-beijing/endpoint
    val apiKey = System.getProperty("llm.apiKey")
    PulsarSettings()
        .withLLMProvider("volcengine")
        .withLLMName("ep-20250218132011-1234567890")
        .withLLMAPIKey(apiKey)

    // LLM configuration guide:
    // https://github.com/platonai/PulsarRPA/blob/master/docs/config/llm/llm-config.md

    // Use the default browser which has an isolated profile
    PulsarSettings().withDefaultBrowser()
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
