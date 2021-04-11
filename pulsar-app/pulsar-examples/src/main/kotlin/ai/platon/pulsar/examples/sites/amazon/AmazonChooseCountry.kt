package ai.platon.pulsar.examples.sites.amazon

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.crawl.AbstractJsEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import org.slf4j.LoggerFactory

class ChooseCountryJsEventHandler: AbstractJsEventHandler() {
    private val log = LoggerFactory.getLogger(ChooseCountryJsEventHandler::class.java)!!

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
        val zipcode = listOf("10001", "10002", "10003", "10004", "10005", "90002", "90003", "90004", "90005").shuffled().first()
        val expressions = """
document.querySelector("#glow-ingress-block").textContent;
document.querySelector("div#nav-global-location-slot a").click();
document.querySelector("input#GLUXZipUpdateInput").value;
document.querySelector("input#GLUXZipUpdateInput").value = '$zipcode';
document.querySelector("a#GLUXChangePostalCodeLink").click();
document.querySelector("div#GLUXZipInputSection input[type=submit]").click();
document.querySelector("#glow-ingress-block").textContent;
document.querySelector("span#a-autoid-1 button").click();
document.querySelector("button[name=glowDoneButton]").click();
document.querySelector("#glow-ingress-block").click();
document.querySelector("#glow-ingress-block").textContent;
    """.trimIndent()

        evaluate(driver, expressions.split(";"))

        val expression = "document.querySelector('#suggestions').outerHTML;"

        val value = evaluate(driver, expression)

        println(value)

        return value
    }
}

fun main() {
    withContext { cx ->
        val context = cx as AbstractPulsarContext
        val session = context.createSession()
        val portalUrl = "https://www.amazon.com/gp/browse.html?node=6563140011"
        val args = "-i 1s"

        val unmodifiedConfig = context.unmodifiedConfig.unbox()
        unmodifiedConfig.set(CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS, "ai.platon.pulsar.crawl.fetch.privacy.PrototypePrivacyContextIdGenerator")
        unmodifiedConfig.set(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")

        session.sessionConfig.putBean(ChooseCountryJsEventHandler())
        session.load(portalUrl, args)

        val document = session.loadDocument(portalUrl, args)

        val text = document.selectFirstOrNull("#glow-ingress-block")?.text() ?: "(unknown)"
        println("Current area: $text")
        val path = session.export(document)
        println("Exported to file://$path")
    }
}
