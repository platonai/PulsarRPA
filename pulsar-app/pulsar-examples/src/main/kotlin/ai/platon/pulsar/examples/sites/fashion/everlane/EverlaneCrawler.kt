package ai.platon.pulsar.examples.sites.fashion.everlane

import ai.platon.pulsar.ql.context.withSQLContext
import ai.platon.pulsar.test.VerboseSQLExecutor

fun main() {
    val indexSQL = """
        select
           dom_first_text(dom, 'div.product-details h3.product__name') as `name`,
           dom_first_text(dom, 'div.product-details .product__price span:first-child') as `price`,
           dom_first_text(dom, 'div.product-details .product__price .product__price--cta') as `bundle_Price`,
           dom_first_text(dom, 'div.product-details .product__color') as `color`,
           dom_first_text(dom, 'div.product__additional-colors') as `variant_Count`,
           dom_first_href(dom, 'a.product-image__container') as `product_Link`,
           dom_first_attr(dom, 'a.product-image__container img.product-image__image', 'src') as `product_Img_Link`,
           dom_first_attr(dom, 'a.product-image__container img.product-image__image--hover', 'src') as `product_Img_Link2`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                'https://www.everlane.com/collections/mens-underwear -i 1d -netCond worse',
                'div.products > .product-container .product'
           )
        """

    val itemsSQL = """
        select
           dom_first_text(dom, 'div.product-heading h1 span[itemprop=name]') as `name`,
           dom_first_attr(dom, 'div.product-heading meta[itemprop=brand]', 'content') as `brand`,
           dom_first_attr(dom, 'div.product-heading meta[itemprop=color]', 'content') as `color`,
           dom_first_attr(dom, 'div.product-heading__price-container h2 meta[itemprop=lowPrice]', 'content') as `low_Price`,
           dom_first_attr(dom, 'div.product-heading__price-container h2 meta[itemprop=highPrice]', 'content') as `high_Price`,
           dom_first_attr(dom, 'div.product-heading__price-container h2 meta[itemprop=priceCurrency]', 'content') as `price_Currency`,
           dom_first_attr(dom, 'div.product-heading__price-container h2 meta[itemprop=offerCount]', 'content') as `offer_Count`,
           dom_first_text(dom, 'div.product-heading__price-container .bundle-cta.product-page__bundle-cta') as `bundle_CTA`,
           dom_first_text(dom, 'div.product-page__options span.review-mini-cta__rating-text') as `rating`,
           dom_first_text(dom, 'div.product-page__options span.review-mini-cta__count-text') as `reviews`,
           dom_first_text(dom, '.product-page__size-selector') as `size_Selection`,
           dom_first_text(dom, 'div.product-page__options .bundle__messasge') as `bundle_Messasge`,
           dom_first_text(dom, 'div.product-page__options .product-page__uniform-header') as `guarantee`,
           dom_first_text(dom, 'div.product-page__options .product-page__uniform-text') as `guarantee_Detail`,
           dom_first_text(dom, 'div.product-page__options .product-details') as `details`,
           dom_first_text(dom, 'div.product-page__options .product-description') as `description`,
           dom_first_text(dom, 'div.product-page__related-product') as `relatedProduct`,
           dom_first_attr(dom, 'div.product-heading meta[itemprop=url]', 'content') as `url`,
           dom_first_attr(dom, 'div.product-heading meta[itemprop=image]', 'content') as `image`,
           dom_base_uri(dom) as `baseUri`
        from
           load_out_pages(
                'https://www.everlane.com/collections/mens-underwear -i 7d -ii 100d -netCond worse',
                '.products a[href~=/products/]'
            )
        """

    val reviewsSQL = """
        select
           dom_all_texts(dom, 'div.review__info .review__info-attribute') as `attributes`,
           dom_first_attr(dom, 'div.review__content meta[itemprop=ratingValue]', 'content') as `rating`,
           dom_first_text(dom, 'div.review__content h4.review__title[itemprop=name]') as `title`,
           dom_first_text(dom, 'div.review__content p.review__body[itemprop=description]') as `content`,
           dom_first_attr(dom, 'div.review__content meta[itemprop=datePublished]', 'content') as `date_Published`,
           dom_base_uri(dom) as `baseUri`
        from
           load_and_select(
                'https://www.everlane.com/products/mens-boxer-brief-4-navy -i 1d -netCond worse',
                'div.product-page__reviews div[itemprop=review]'
           )
        """

    withSQLContext { ctx ->
        val executor = VerboseSQLExecutor(ctx)
        executor.executeQuery(indexSQL)
        executor.executeQuery(itemsSQL)
        executor.executeQuery(reviewsSQL)
    }
}
