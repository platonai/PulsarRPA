package ai.platon.pulsar.examples.sites.simuwang

import ai.platon.pulsar.crawl.AbstractJsEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseCrawler

class LoginJsEventHandler: AbstractJsEventHandler() {
    override var verbose = true

    override suspend fun onAfterComputeFeature(page: WebPage, driver: WebDriver): Any? {
        val expressions = """
            let message = "Login in ...";
            document.querySelector(".comp-login input.comp-login-input").value = 'username';
            document.querySelector(".comp-login input#GLUXZipUpdateInput").value = 'password';
            document.querySelector(".comp-login button#comp-login-btn").click();
        """.trimIndent()

        return evaluate(driver, expressions.split(";"))
    }
}

fun main() {
    val seed = "https://ly.simuwang.com/"
    val args = "-i 1s -ii 10d -ol a[href~=roadshow] -tl 100"

    withSQLContext {
        val crawler = VerboseCrawler(it)
        crawler.load(seed, "$args -refresh", LoginJsEventHandler())
        crawler.loadOutPages(seed, args)
    }
}
