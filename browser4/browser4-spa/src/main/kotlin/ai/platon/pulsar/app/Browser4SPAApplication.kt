package ai.platon.pulsar.app

import ai.platon.pulsar.agentic.AgenticSession
import ai.platon.pulsar.agentic.context.AgenticContexts
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.external.ChatModelFactory
import ai.platon.pulsar.skeleton.PulsarSettings
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.context.support.AbstractApplicationContext

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan(
    "ai.platon.pulsar.boot.autoconfigure",
    "ai.platon.pulsar.rest.api",
    "ai.platon.pulsar.app.api",
)
class Browser4Application(
    val applicationContext: ApplicationContext
) {
    private val logger = getLogger(Browser4Application::class)

    @Value("\${server.port:8182}")
    var port: Int = 8182

    @Value("\${server.servlet.context-path:}")
    lateinit var contextPath: String

    @Value("\${server.hostname:localhost}")
    lateinit var hostname: String

    @Autowired
    lateinit var session: AgenticSession

    @PostConstruct
    fun initialize() {

    }

    @PostConstruct
    fun showHelp() {
        try {
            val llmHelp = getLLMStatusMessage()
            val urls = buildServerUrls()
            val help = buildHelpMessage(llmHelp, urls)

            logger.info("Welcome to Browser4 SPA! \n{}", help)
        } catch (e: Exception) {
            logger.error("Failed to display help message", e)
        }
    }

    private fun getLLMStatusMessage(): String {
        return try {
            val hasLLM = ChatModelFactory.isModelConfigured(session.configuration, verbose = false)
            if (hasLLM) {
                "LLM is configured, you can use LLM commands."
            } else {
                "LLM is not configured, you can only use non-LLM commands. X-SQL is still available. " +
                        "It is highly recommended to set DEEPSEEK_API_KEY or other LLM keys to enable LLM features."
            }
        } catch (e: Exception) {
            logger.warn("Failed to check LLM configuration", e)
            "LLM configuration check failed. Please verify your setup."
        }
    }

    private fun buildServerUrls(): ServerUrls {
        val baseURL = URLUtils.buildServerUrl(hostname, port, contextPath)
            .replace("/api", "") // Ensure the URL is correct even if the API is upgraded
            .trimEnd('/')

        return ServerUrls(
            navigateEndpoint = "$baseURL/navigate",
            actEndpoint = "$baseURL/act",
            screenshotEndpoint = "$baseURL/screenshot",
            extractEndpoint = "$baseURL/extract/",
        )
    }

    private fun buildHelpMessage(llmHelp: String, urls: ServerUrls): String {
        val message = """
1. navigate to a page - ${urls.navigateEndpoint}
2. act on a page - ${urls.actEndpoint}
3. take screenshot - ${urls.screenshotEndpoint}
4. extract data from the page - ${urls.extractEndpoint}
        """.trimIndent()

        return message
    }

    private data class ServerUrls(
        val navigateEndpoint: String,
        val actEndpoint: String,
        val screenshotEndpoint: String,
        val extractEndpoint: String,
    )
}

class Browser4SPAContextInitializer : ApplicationContextInitializer<AbstractApplicationContext> {
    override fun initialize(applicationContext: AbstractApplicationContext) {
        PulsarSettings.withDefaultBrowser()
        AgenticContexts.create(applicationContext)
    }
}

fun main(args: Array<String>) {
    runApplication<Browser4Application>(*args) {
        addInitializers(Browser4SPAContextInitializer())
        setAdditionalProfiles("spa")
        setLogStartupInfo(true)
    }
}
