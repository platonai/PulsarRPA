package ai.platon.pulsar.examples.sites.fashion.hanes

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.ProductExtractor
import kotlin.system.exitProcess

fun main() {
    val itemsSQLTemplate = """
        select
            dom_first_text(dom, 'h1.page-title') as `title`,
            dom_first_text(dom, '.product-info-price span[data-price-type=oldPrice]') as `listprice`,
            dom_first_text(dom, '.product-info-price span[data-price-type=finalPrice]') as `price`,
            str_substring_before(dom_first_text(dom, '.product.info.detailed div[aria-hidden]:contains(stars)'), ' stars') as `rating`,
            str_substring_between(dom_first_text(dom, '.product-info-main .bv_numReviews_component_container'), '(', ')') as `reviews`,

            dom_all_texts(dom, '.swatch-attribute.size .swatch-option.text') as `size_Variants`,
            dom_first_text(dom, '.product.attribute.description') as `product_Details`,

            dom_base_uri(dom) as `baseUri`
        from
            load_out_pages(
                '{{url}} -refresh -netCond worse', 
                '.product.details a.product-item-link'
            )
        """

    withSQLContext { ctx ->
        val now = DateTimes.formatNow("HH")
        val path = AppPaths.getTmp("rs").resolve(now).resolve("tommy")
        val executor = ProductExtractor(path, ctx)
        val itemUrls = arrayOf(
            "https://www.hanes.com/men/underwear.html",
            "https://www.hanes.com/men/sleepwear-lounge/sets.html"
        )
        itemUrls.forEach { url ->
            val itemsSQL = SQLTemplate(itemsSQLTemplate).createInstance(url).sql
            executor.extract(itemsSQL)
        }
    }

    exitProcess(0)
}
