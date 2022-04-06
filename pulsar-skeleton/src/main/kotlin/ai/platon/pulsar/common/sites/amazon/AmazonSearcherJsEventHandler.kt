package ai.platon.pulsar.common.sites.amazon

import ai.platon.pulsar.crawl.AbstractWebPageWebDriverHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.persist.WebPage

data class AmazonSuggestion(
    var alias: String,
    var keyword: String,
    var isfb: String,
    var crid: String
)

class AmazonSearcherJsEventHandler(
    val keyword: String,
    val suggestions: MutableList<AmazonSuggestion> = mutableListOf()
): AbstractWebPageWebDriverHandler() {

    override suspend fun invokeDeferred(page: WebPage, driver: WebDriver): Any? {
        if (!page.url.matches(".+amazon.+".toRegex())) {
            return null
        }

        val selector = "input#twotabsearchtextbox"
        val expressions = "document.querySelector('$selector').value = '$keyword';" +
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
            doc.select(".s-suggestion").mapTo(suggestions) {
                AmazonSuggestion(
                    it.attr("data-alias"),
                    it.attr("data-keyword"),
                    it.attr("data-isfb"),
                    it.attr("data-crid"))
            }
        }

        return suggestions
    }
}
