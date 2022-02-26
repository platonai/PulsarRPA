package ai.platon.pulsar.examples.sites.fashion.saxx

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.ProductExtractor
import kotlin.system.exitProcess

fun main() {
    val indexSQL = """
        select
           dom_attr(dom, 'data-name') as `title`,
           dom_attr(dom, 'data-variant-price') as `list_Price`,
           dom_attr(dom, 'data-price') as `price`,
           dom_first_attr(dom, 'div.product-item__swatch-list', 'data-swatch-count') as `variant_Count`,
           dom_first_attr(dom, 'a[data-product-link] div[title~=star]', 'title') as `rating_Text`,
           str_substring_before(dom_first_attr(dom, 'a[data-product-link] div[title~=star]', 'title'), ' star') as `rating`,
           str_substring_between(dom_first_attr(dom, 'a[data-product-link] div[title~=star]', 'title'), 'with ', ' reviews') as `reviews`,
           dom_first_href(dom, 'a[data-product-link]') as `product_Link`,
           dom_base_uri(dom) as `base_Uri`
        from
           load_and_select(
                'https://www.saxxunderwear.com/collections/underwear 
                    -i 1d -requireSize 500000 -netCond worse',
                'div.collection-list > .collection-list-row div.product-card'
           )
        """

    val itemsSQLTemplate = """
        select
           dom_first_text(dom, 'h1.title') as `name`,
           dom_first_text(dom, '.price-preview del[data-product-id]') as `list_Price`,
           dom_first_text(dom, '.price-preview span[data-product-id]') as `price`,
           dom_first_text(dom, 'div.bottom-line-items span:contains(star rating)') as `rating`,
           dom_first_text(dom, 'span.reviews-qa-label') as `reviews`,

           dom_all_texts(dom, '.product-options.has-options .swatch.size label') as `color_Variants`,

           dom_first_text(dom, '.product-description') as `description`,

           dom_first_attr(dom, 'div.product-gallery div[data-zoom-img]', 'data-zoom-img') as `big_Images`,
           dom_base_uri(dom) as `baseUri`
        from
           load_out_pages(
                '{{url}}
                    -i 1d -requireSize 500000 -itemRequireSize 300000 -ignoreFailure -netCond worst',
                'div.product-card a[href~=/products/]'
           )
        """

    val reviewsSQLTemplate = """
        select
           dom_first_text(dom, '.yotpo-user-name') as `user_Name`,
           dom_first_text(dom, '.yotpo-review-date') as `date`,
           dom_first_text(dom, '.yotpo-user-field:contains(Fit)') as `fit`,
           dom_first_text(dom, '.yotpo-user-title') as `user_Type`,

           dom_first_text(dom, 'div.yotpo-review-stars span:contains(rating)') as `rating`,
           dom_first_text(dom, '.yotpo-main .content-title') as `title`,
           dom_first_text(dom, '.content-review') as `content`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                '{{url}} -i 1d -netCond worse',
                '.yotpo-reviews .yotpo-review'
           )
        """

    withSQLContext { ctx ->
        val now = DateTimes.formatNow("HH")
        val path = AppPaths.getTmp("rs").resolve(now).resolve("saxx")
        val executor = ProductExtractor(path, ctx)
        val itemUrls = arrayOf(
            "https://www.saxxunderwear.com/collections/underwear",
        )
        itemUrls.forEach { url ->
            val itemsSQL = SQLTemplate(itemsSQLTemplate).createInstance(url).sql
            executor.extract(itemsSQL, reviewsSQLTemplate)
        }
    }

    exitProcess(0)
}
