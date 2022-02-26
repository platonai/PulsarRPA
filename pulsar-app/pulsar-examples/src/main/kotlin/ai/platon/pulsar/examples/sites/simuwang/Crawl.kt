package ai.platon.pulsar.examples.sites.simuwang

import ai.platon.pulsar.crawl.AbstractJsEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseCrawler

class LoginJsEventHandler: AbstractJsEventHandler() {
    override var verbose = true

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
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
    val seed = "https://ly.simuwang.com/"
    val args = "-i 1s -ii 10d -ol a[href~=roadshow] -tl 100"

    withSQLContext {
        val crawler = VerboseCrawler(it)
        crawler.eventHandler = LoginJsEventHandler()
        crawler.load(seed, "$args -refresh")
        // crawler.loadOutPages(seed, args)
    }
}
