package ai.platon.pulsar.examples.sites.simuwang

import ai.platon.pulsar.crawl.AbstractWebDriverHandler
import ai.platon.pulsar.crawl.DefaultPulsarEventPipelineHandler
import ai.platon.pulsar.crawl.PulsarEventPipelineHandler
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.SQLContexts

class LoginHandler: AbstractWebDriverHandler() {
    override suspend fun invoke(page: WebPage, driver: WebDriver): Any? {
println("login ...")
        login(page, driver)
        return null
    }

    private suspend fun login(page: WebPage, driver: WebDriver): Any? {
        val time = driver.waitFor(".comp-login-b2")
        if (time <= 0) {
            return null
        }

        val username = System.getenv("EXOTIC_SIMUWANG_USERNAME")
        val password = System.getenv("EXOTIC_SIMUWANG_PASSWORD")

        driver.bringToFront()
        driver.click("button.comp-login-b2")
        driver.type("input[name=username]", username)
        driver.type("input[type=password]", password)
        driver.click("button.comp-login-btn", count = 2)

        return null
    }
}

fun main() {
    val portalUrl = "https://dc.simuwang.com/"
    val args = "-i 30s -ii 30s -ol a[href~=product] -tl 10"

    val context = SQLContexts.create()
    val session = context.createSession()
    val loginHandler = LoginHandler()
    val options = session.options(args)
    options.eventHandler = DefaultPulsarEventPipelineHandler().also {
        it.simulateEventPipelineHandler.onBeforeComputeFeaturePipeline.addLast(loginHandler)
    }
    // open the portal page and login
    session.load(portalUrl, options)

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
    load_and_select('$portalUrl', '.ranking-table-tbody .ranking-table-tbody-tr')
        """.trimIndent()
    // extract fields from the portal page
    // context.executeQuery(sql)
    // load out pages
    session.loadOutPages(portalUrl, options)
    // wait for all done
    context.await()
}
