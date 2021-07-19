package ai.platon.pulsar.examples.sites.tommyjohn

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseSQLExecutor

fun main() {
    BrowserSettings.withGUI()

    val sql = """
        select
           dom_attr(dom, 'data-name') as `title`,
           dom_attr(dom, 'data-variant-price') as `listprice`,
           dom_attr(dom, 'data-price') as `price`,
           dom_first_attr(dom, 'div[product-item__swatch-list]', 'data-swatch-count') as `variantCount`,
           str_substring_before(dom_first_attr(dom, 'a[data-product-link] div[title~=reviews]'), ' star') as `rating`,
           str_substring_between(dom_first_attr(dom, 'a[data-product-link] div[title~=reviews]'), 'with ', ' reviews') as `reviews`,
           dom_first_href(dom, 'a[data-product-link]') as `product_link`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                'https://tommyjohn.com/collections/loungewear-mens?sort-by=relevance&sort-order=descending
                    -refresh -netCond worse',
                'div[data-collection-entry] > div[data-product-id]'
           )
        """

    withSQLContext { VerboseSQLExecutor(it).query(sql) }
}
