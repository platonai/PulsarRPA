package ai.platon.pulsar.examples.sites.hanes

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseSQLExecutor
import kotlin.system.exitProcess

fun main() {
    BrowserSettings.withGUI()

    val sql = """
        select
            dom_first_text(dom, '.details a[product-item-link]') as `title`,
            dom_first_text(dom, '.details .normal-price .price') as `listprice`,
            dom_first_text(dom, '.details .old-price .price') as `price`,
            str_substring_before(dom_first_text(dom, '.details a[aria-label~=stars]'), ' out of') as `rating`,
            str_substring_between(dom_first_text(dom, '.details a[aria-label~=stars]'), 'stars. ', ' reviews') as `reviews`,
            dom_first_href(dom, '.details a[product-item-link]') as `product_link,`,
            dom_base_uri(dom) as `baseUri`
         from
            load_and_select('https://www.hanes.com/men/underwear.html -refresh -netCond worse', 'ol.products.list li[data-product-sku]')
        """

    withSQLContext { VerboseSQLExecutor(it).query(sql) }
}
