package ai.platon.pulsar.app

import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan("ai.platon.pulsar.rest.api")
class PulsarApplication {
    private val logger = getLogger(PulsarApplication::class)

    @Value("\${server.port}")
    var port: Int = 8182

    @Value("\${server.servlet.context-path}")
    lateinit var contextPath: String

    @Autowired
    lateinit var session: PulsarSession

    @PostConstruct
    fun showHelp() {
        val hasLLM = ChatModelFactory.isModelConfigured(session.unmodifiedConfig, verbose = false)
        val llmHelp = if (hasLLM) {
            "LLM is configured, you can use LLM commands."
        } else {
            "LLM is not configured, you can only use non-LLM commands. X-SQL is still available. " +
                    "It is highly recommended to set DEEPSEEK_API_KEY or other LLM keys to enable LLM features."
        }
        val baseURL = URLUtils.buildServerUrl("localhost", port, contextPath)
            .replace("/api", "") // Ensure the URL is correct even if the API is upgraded
            .trimEnd('/')
        val frontendURL = "$baseURL/command.html"
        val commandEndpoint = "$baseURL/api/commands/plain"
        val scrapingEndpoint = "$baseURL/api/x/e"

        val help = buildString {
            appendLine("====================================================================================")
            appendLine(llmHelp)
            appendLine("------------------------------------------------------------------------------")
            appendLine("Example 1: Using the WebUI to run a command:")
            appendLine(frontendURL)
            appendLine("------------------------------------------------------------------------------")
            appendLine("Example 2: For Beginners – Just Text, No Code:")
            appendLine(
                """
            ```shell
            curl -X POST "$commandEndpoint" -H "Content-Type: text/plain" -d "
                Go to https://www.amazon.com/dp/B0C1H26C46

                After browser launch: clear browser cookies.
                After page load: scroll to the middle.

                Summarize the product.
                Extract: product name, price, ratings.
                Find all links containing /dp/.
            "
            ```
            """.trimIndent()
            )
            appendLine("------------------------------------------------------------------------------")
            appendLine("Example 3: For Advanced Users — LLM + X-SQL: Precise, Flexible, Powerful:")
            appendLine(
                """
            ```shell
            curl -X POST "$scrapingEndpoint" -H "Content-Type: text/plain" -d "
            select
              llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
              dom_base_uri(dom) as url,
              dom_first_text(dom, '#productTitle') as title,
              dom_first_slim_html(dom, 'img:expr(width > 400)') as img
            from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
            "
            ```
            """.trimIndent()
            )
        }

        logger.info("Welcome to PulsarRPA! \n{}", help)
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarApplication>(*args) {
        addInitializers(PulsarContextInitializer())
        setAdditionalProfiles("master", "private")
        setLogStartupInfo(true)
    }
}
