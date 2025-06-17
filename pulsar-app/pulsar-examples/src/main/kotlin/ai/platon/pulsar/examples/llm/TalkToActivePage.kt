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
    // Use the default browser for best experience
    // Enable Single Page Application mode to avoid timeout
    PulsarSettings().withDefaultBrowser().withSPA()

    val session = PulsarContexts.createSession()
    val url = "https://www.amazon.com/dp/B0C1H26C46"

    val prompts = """
move cursor to the element with id 'title' and click it
scroll to middle
scroll to top
get the text of the element with id 'title'
        """.trimIndent().split("\n").filter { it.isNotBlank() }

    val eventHandlers = DefaultPageEventHandlers()
    eventHandlers.browseEventHandlers.onDocumentFullyLoaded.addLast { page, driver ->
        while (true) {
            println("Talk and execute on the active page")

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

            println("All done.")
        }
    }
    session.open(url, eventHandlers)

    readlnOrNull()
}
