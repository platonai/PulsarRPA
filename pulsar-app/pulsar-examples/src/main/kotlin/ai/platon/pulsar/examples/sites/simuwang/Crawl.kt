package ai.platon.pulsar.examples.sites.simuwang

import ai.platon.pulsar.crawl.AbstractJsEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.test.VerboseSQLExecutor

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
-- document.querySelector("button.comp-login-btn").click();
        """.trimIndent()

        return evaluate(driver, expressions.split(";"))
    }
}

fun main() {
    val portal = "https://dc.simuwang.com/"
    val args = "-i 30s -ii 30s -ol a[href~=product] -tl 10"

    // BrowserSettings.withDefaultDataDir()

    val executor = VerboseSQLExecutor()
    executor.eventHandler = LoginJsEventHandler()
    executor.load(portal, "-i 30s -scrollCount 1")

    val sql = """
select
    dom_first_attr(dom, '> div', 'name') as code,
    dom_first_attr(dom, '.ranking-table-ellipsis a[href~=product]', 'title') as name,
    dom_first_href(dom, '.ranking-table-ellipsis a[href~=product]') as productUrl,
    dom_first_text(dom, 'div a[href~=company]') as company,
    dom_first_href(dom, 'div a[href~=company]') as companyUrl,
    -- 排名期最新净值
    dom_first_text(dom, 'div div.nav.dc-home-333') as latestWorth,
    dom_first_text(dom, 'div div.nav.dc-home-333 ~ .price-date') as dateOfLatestWorth,
    dom_first_text(dom, 'div .ranking-table-profit-tbody') as profit
from
    load_and_select('$portal', '.ranking-table-tbody .ranking-table-tbody-tr')
        """.trimIndent()
    executor.executeQuery(sql)
    // crawler.loadOutPages(portal, args)
}
