package ai.platon.pulsar.app

import ai.platon.pulsar.boot.autoconfigure.PulsarContextInitializer
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.URLUtils
import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.SystemUtils
import org.h2.tools.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import java.nio.file.Paths
import java.sql.SQLException

@SpringBootApplication
@ImportResource("classpath:pulsar-beans/app-context.xml")
@ComponentScan("ai.platon.pulsar.rest.api")
class PulsarApplication {
    private val logger = getLogger(PulsarApplication::class)

    @Value("\${server.port}")
    var port: Int = 8182

    @Value("\${server.servlet.context-path}")
    lateinit var contextPath: String

    @PostConstruct
    fun showHelp() {
        var url = URLUtils.buildServerUrl("localhost", port, contextPath, "api/commands/plain")
            .replace("/api/api/", "/api/") // Ensure the URL is correct even the api is upgraded

        var example = """
```shell
curl -X POST "$url" -H "Content-Type: text/plain" -d '
    Go to https://www.amazon.com/dp/B0C1H26C46
    
    After browser launch: clear browser cookies.
    After page load: scroll to the middle.
    
    Summarize the product.
    Extract: product name, price, ratings.
    Find all links containing /dp/.
'
```
        """.trimIndent()

        if (SystemUtils.IS_OS_WINDOWS) {
            example = example.replace("\\", "`")
        }
        logger.info("For Beginners – Just Text, No Code: \n{}", example)

        url = URLUtils.buildServerUrl("localhost", port, contextPath, "api/x/e")
            .replace("/api/api/", "/api/") // Ensure the URL is correct even the api is upgraded

        example = """
```shell
curl -X POST "$url" -H "Content-Type: text/plain" -d "
select
  llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
  dom_base_uri(dom) as url,
  dom_first_text(dom, '#productTitle') as title,
  dom_first_slim_html(dom, 'img:expr(width > 400)') as img
from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
```
        """.trimIndent()

        if (SystemUtils.IS_OS_WINDOWS) {
            example = example.replace("\\", "`")
        }
        logger.info("For Advanced Users — LLM + X-SQL: Precise, Flexible, Powerful: \n{}", example)
    }
}

fun main(args: Array<String>) {
    runApplication<PulsarApplication>(*args) {
        addInitializers(PulsarContextInitializer())
        setAdditionalProfiles("master", "private")
        setLogStartupInfo(true)
    }
}
