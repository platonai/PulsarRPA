package ai.platon.pulsar.examples.sites.saxx

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseSQLExecutor
import kotlin.system.exitProcess

fun main() {
    BrowserSettings.withGUI()

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

    val itemSQL = """
        select
           dom_first_text(dom, 'section.product-main h1.product-info__title') as `name`,
           dom_first_text(dom, 'section.product-main span.product-info__price') as `price`,
           dom_first_text(dom, 'section.product-main .yotpo-stars span:contains(star rating)') as `rating`,
           dom_first_text(dom, 'section.product-main .yotpo-stars ~ a') as `reviews`,

           dom_all_attrs(dom, 'div[data-color-section=Color] ul li input', 'value') as `color_Variants`,
           dom_all_texts(dom, 'div[data-option-name=Size] ul li') as `size_Variants`,

           dom_first_text(dom, 'div.product-details-container section[data-product-details-description]') as `product_Details`,
           dom_first_text(dom, 'div.product-page__options .product-description') as `description`,

           dom_first_attr(dom, 'div.product-gallery div[data-zoom-img]', 'data-zoom-img') as `big_Images`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                'https://www.saxxunderwear.com/collections/underwear -i 1d -netCond worse',
                'body'
           )
        """

    val reviewsSQL = """
        select
           dom_first_text(dom, 'p.reviewer-user-name') as `user_Name`,
           dom_first_text(dom, 'p.reviewer-user-type') as `user_Type`,
           dom_first_text(dom, 'p.yotpo-review-buyer-data') as `buyer_Data`,
           dom_first_text(dom, 'p.reviewer-user-type') as `user_Type`,

           dom_first_text(dom, 'span.yotpo-review-stars span:contains(rating)') as `rating`,
           dom_first_text(dom, 'p.yotpo-review-title') as `title`,
           dom_first_text(dom, 'p.yotpo-review-title') as `content`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                'https://www.saxxunderwear.com/collections/underwear -i 1d -netCond worse',
                'ul.review-list li.review-item'
           )
        """

    withSQLContext { ctx ->
        val executor = VerboseSQLExecutor(ctx)
        executor.query(indexSQL)
        executor.query(itemSQL)
        executor.query(reviewsSQL)
    }
}
