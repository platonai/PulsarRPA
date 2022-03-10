package ai.platon.pulsar.examples.sites.simuwang

import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.crawl.AbstractEmulateEventHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.test.VerboseSQLExecutor
import kotlinx.coroutines.delay

class LoginHandler: AbstractEmulateEventHandler() {
    override var verbose = true

    override suspend fun onBeforeCheckDOMState(page: WebPage, driver: WebDriver): Any? {
        delay(5000)
        if (!driver.exists(".comp-login-b2")) {
            return null
        }

        val username = System.getenv("EXOTIC_SIMUWANG_USERNAME")
        val password = System.getenv("EXOTIC_SIMUWANG_PASSWORD")

        driver.click("button.comp-login-b2")
        driver.type("input[name=username]", username)
        driver.type("input[type=password]", password)
        driver.click("button.comp-login-btn", count = 2)

        return null
    }
}

fun main() {
    val portal = "https://dc.simuwang.com/"
    val args = "-i 30s -ii 30s -ol a[href~=product] -tl 10"

    val executor = VerboseSQLExecutor()
    executor.eventHandler = LoginHandler()
    executor.open(portal, "-scrollCount 1")
//    executor.open("https://bot.sannysoft.com/", "-scrollCount 1")

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
//    executor.executeQuery(sql)
//    executor.loadOutPages(portal, args)
}
