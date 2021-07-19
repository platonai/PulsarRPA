package ai.platon.pulsar.examples.sites.everlane

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseSQLExecutor

fun main() {
    BrowserSettings.withGUI()

    val sql = """
        select
           dom_first_text(dom, 'div.product-details h3.product__name') as `name`,
           dom_first_text(dom, 'div.product-details .product__price span:first-child') as `price`,
           dom_first_text(dom, 'div.product-details .product__color') as `color`,
           dom_first_text(dom, 'div.product-details .product__price .product__price--cta') as `price2`,
           dom_first_text(dom, 'div.product__additional-colors') as `variantCount`,
           str_substring_before(dom_first_attr(dom, 'a[data-product-link] div[title~=reviews]'), ' star') as `rating`,
           str_substring_between(dom_first_attr(dom, 'a[data-product-link] div[title~=reviews]'), 'with ', ' reviews') as `reviews`,
           dom_first_href(dom, 'a.product-image__container') as `productLink`,
           dom_first_href(dom, 'a.product-image__container img.product-image__image') as `productImgLink`,
           dom_first_href(dom, 'a.product-image__container img.product-image__image--hover') as `productImgLink2`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                'https://www.everlane.com/collections/mens-underwear -refresh -netCond worse',
                'div.products > .product-container .product'
           )
        """

    withSQLContext { VerboseSQLExecutor(it).query(sql) }
}
