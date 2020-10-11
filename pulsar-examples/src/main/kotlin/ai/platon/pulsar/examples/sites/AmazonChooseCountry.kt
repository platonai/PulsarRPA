package ai.platon.pulsar.examples.sites

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.context.withContext

fun main() {
    val portalUrl = "https://www.amazon.com/gp/browse.html?node=6563140011&ref_=nav_em_T1_0_4_13_1_amazon_smart_home"
    val args = "-i 1s"

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

    withContext { cx ->
        val context = cx as AbstractPulsarContext
        val session = context.createSession()

        val unmodifiedConfig = context.unmodifiedConfig.unbox()
        unmodifiedConfig.set(CapabilityTypes.PRIVACY_CONTEXT_ID_GENERATOR_CLASS, "ai.platon.pulsar.crawl.fetch.privacy.PrototypePrivacyContextIdGenerator")
        unmodifiedConfig.set(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")
        unmodifiedConfig.set(CapabilityTypes.FETCH_CLIENT_JS_AFTER_FEATURE_COMPUTE, expressions)

        session.load(portalUrl, args)

        unmodifiedConfig.unset(CapabilityTypes.FETCH_CLIENT_JS_AFTER_FEATURE_COMPUTE)
        val document = session.loadDocument(portalUrl, args)

        val text = document.selectFirstOrNull("#glow-ingress-block")?.text() ?: "(unknown)"
        println("Current area: $text")
        val path = session.export(document)
        println("Exported to file://$path")
    }
}
