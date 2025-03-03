package ai.platon.pulsar.examples.llm

import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.event.impl.DefaultPageEventHandlers
import kotlinx.coroutines.delay
import java.time.OffsetDateTime

/**
 * Demonstrates talking to the active webpage.
 * */
fun main() {
    // !!! IMPORTANT !!!
    // CHANGE TO YOUR OWN LLM NAME AND API KEY
    //
    // see https://console.volcengine.com/ark/region:ark+cn-beijing/endpoint
    val apiKey = System.getProperty("llm.apiKey")
    PulsarSettings()
        .withSPA() // enable Single Page Application mode, so the execution will not be timeout
        .withLLMProvider("volcengine") // use volcengine as the LLM provider
        .withLLMName("ep-20250218132011-2scs8") // the LLM name, you should change it to your own
        .withLLMAPIKey(apiKey) // the LLM api key, you should change it to your own

    // Or
    // use config file under $PULSAR_HOME/config/conf-enabled
    // You can find the template config files here:
    // https://github.com/platonai/PulsarRPA/blob/master/docs/config/llm/template

    // Use the default browser which has an isolated user data directory
    PulsarSettings().withDefaultBrowser()

    val session = PulsarContexts.createSession()
    val url = "https://www.amazon.com/dp/B0C1H26C46"

    val eventHandlers = DefaultPageEventHandlers()
    eventHandlers.browseEventHandlers.onDocumentActuallyReady.addLast { page, driver ->
        val prompts = listOf(
            "move cursor to the element with id 'title' and click it",
            "scroll to middle",
            "scroll to top",
            "get the text of the element with id 'title'",
        )

        while (!Thread.interrupted()) {
            prompts.forEach { prompt ->
                println("\n")
                println(OffsetDateTime.now())
                println(">>> $prompt")
                val result = session.instruct(prompt, driver)
                println(result.modelResponse)
                result.functionCalls.forEachIndexed { i, functionCall ->
                    println()
                    println("> $functionCall")
                    val functionResult = result.functionResults.getOrNull(i)
                    if (functionResult != null && functionResult !is Unit) {
                        val s = functionResult.toString()
                        if (s.isNotBlank()) {
                            println()
                            println(s)
                        }
                    }
                }
                delay(1000)
            }
        }
    }
    session.open(url, eventHandlers)

    readlnOrNull()
}
