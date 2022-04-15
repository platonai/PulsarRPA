package ai.platon.pulsar.amazon.environment

import ai.platon.pulsar.session.PulsarSession
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.crawl.*
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory
import java.time.Duration

class ChooseLanguageJsEventHandler: AbstractWebPageWebDriverHandler() {
    override var verbose = true

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        val expressions = "document.querySelector(\"input[value=en_US]\").click();\n" +
                "document.querySelector(\"span#icp-btn-save input[type=submit]\").click();"
        return evaluate(driver, expressions.split(";"))
    }
}

class ChooseCountryJsEventHandler: AbstractWebPageWebDriverHandler() {
    override var verbose = true

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        // New York City
        val zipcode = listOf("10001", "10001", "10002", "10002").shuffled().first()
        val resource = "sites/amazon/js/choose-district.js"
        val expressions = ResourceLoader.readString(resource)
            .replace("10001", zipcode)
            .split(";\n")
            .filter { it.isNotBlank() }
            .filter { !it.startsWith("// ") }
            .joinToString(";\n")

        return evaluate(driver, expressions.split(";"))
    }
}

class ChooseCountry(
    val portalUrl: String,
    val loadArguments: String,
    val session: PulsarSession
) {
    private val log = LoggerFactory.getLogger(ChooseCountry::class.java)
    val chooseLanguageUrl = "https://www.amazon.com/gp/customer-preferences/select-language"

    val options = session.options(loadArguments).apply {
        expires = Duration.ZERO
        refresh = true
    }

    fun choose() {
        // 1. warn up
        val page = session.load(portalUrl, options)

        var document = session.parse(page)
        var text = document.selectFirstOrNull("#glow-ingress-block")?.text() ?: "(unknown)"
        println("Current area: $text")

        // 2. choose language
//        var jsEventHandler: JsEventHandler = ChooseLanguageJsEventHandler()
//        session.load(chooseLanguageUrl, options)
//        session.sessionConfig.removeBean(jsEventHandler)

        // 3. choose district
        val jsEventHandler = ChooseCountryJsEventHandler()
        options.ensureEventHandler().simulateEventHandler.onBeforeComputeFeature.addLast(jsEventHandler)
        session.load(portalUrl, options)

        // 4. check the result
        document = session.loadDocument(portalUrl, options)

        text = document.selectFirstOrNull("#nav-tools a span.icp-nav-flag")?.attr("class") ?: "(unknown)"
        log.info("Current country: $text")

        text = document.selectFirstOrNull("#glow-ingress-block")?.text() ?: "(unknown)"
        log.info("Current area: $text")
        val path = session.export(document)
        log.info("Exported to file://$path")
    }
}
