package ai.platon.pulsar.examples.sites.simuwang

import ai.platon.pulsar.crawl.AbstractJsEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseCrawler

class LoginJsEventHandler: AbstractJsEventHandler() {
    override var verbose = true

    override suspend fun onBeforeComputeFeature(page: WebPage, driver: WebDriver): Any? {
        val username = System.getenv("EXOTIC_SIMUWANG_USERNAME")
        val password = System.getenv("EXOTIC_SIMUWANG_PASSWORD")

        val expressions = """
let message = "Login in ...";
document.querySelector("button.comp-login-b2").click();
document.querySelector("input[name=username]").value = '$username';
document.querySelector("input[name=username]").dispatchEvent(new Event('input'));
document.querySelector("input[type=password]").value = '$password';
document.querySelector("input[type=password]").dispatchEvent(new Event('input'));
document.querySelector("button.comp-login-btn").click();
        """.trimIndent()

        return evaluate(driver, expressions.split(";"))
    }
}

fun main() {
    val portal = "https://dc.simuwang.com/"
    val args = "-i 30s -ii 30s -ol a[href~=product] -tl 10"

    withSQLContext {
        val crawler = VerboseCrawler(it)
        crawler.eventHandler = LoginJsEventHandler()
        crawler.open(portal)
        // crawler.loadOutPages(portal, args)
    }
}
