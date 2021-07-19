package ai.platon.pulsar.examples.sites.saxx

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseSQLExecutor
import kotlin.system.exitProcess

fun main() {
    BrowserSettings.withGUI()

    val sql = """
        select
           dom_first_text(dom, 'p.product-title') as `name`,
           dom_first_text(dom, 'p.product-price > del') as `listprice`,
           dom_first_text(dom, 'p.product-price > span') as `price`,
           dom_first_text(dom, 'p.product-color') as `color`,
           dom_first_href(dom, 'a.product-card-link') as `productLink`,
           dom_first_attr(dom, 'div.product-card-upper-image img', 'data-src') as `productImgLink`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                'https://www.saxxunderwear.com/collections/underwear -refresh -netCond worse',
                'div.collection-list > .collection-list-row div.product-card'
           )
        """

    withSQLContext { VerboseSQLExecutor(it).query(sql) }
}
