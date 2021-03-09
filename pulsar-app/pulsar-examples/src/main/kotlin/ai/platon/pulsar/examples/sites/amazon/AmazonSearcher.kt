package ai.platon.pulsar.examples.sites.amazon

import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.context.withContext
import ai.platon.pulsar.crawl.AbstractJsEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.persist.WebPage

class AmazonSearcherJsEventHandler: AbstractJsEventHandler() {

    override suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any? {
        val selector = "input#twotabsearchtextbox"
        val expressions = "document.querySelector('$selector').value = 'cup';" +
                "document.querySelector('$selector').click();" +
                "document.querySelector('$selector').focus({preventScroll: true});" +
                "let a = 1+1;" +
                "var b = 1+2;" +
                "let c = 1+3;"

        evaluate(driver, expressions.split(";"))

        val expression = "document.querySelector('#suggestions').outerHTML;"
        val value = evaluate(driver, expression)

        if (value is String && value.contains("<div")) {
            val doc = Documents.parseBodyFragment(value)
            val suggestions = doc.select(".s-suggestion")
            suggestions.forEach {
                println("................................")
                println("alias: " + it.attr("data-alias"))
                println("keyword: " + it.attr("data-keyword"))
                println("isfb: " + it.attr("data-isfb"))
                println("crid: " + it.attr("data-crid"))
            }
        }

        return value
    }
}

fun main() {
    val portalUrl = "https://www.amazon.com/ -i 0s"

    withContext { cx ->
        cx.unmodifiedConfig.unbox().set(CapabilityTypes.BROWSER_DRIVER_HEADLESS, "false")
        val i = cx.createSession()
        i.sessionConfig.putBean(AmazonSearcherJsEventHandler())
        i.load(portalUrl)
    }
}
