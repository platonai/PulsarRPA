-- noinspection SqlDialectInspectionForFile
-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_uri(dom) as `url`,
    dom_base_uri(dom) as `baseUri`,
    dom_first_text(dom, '#productTitle') as `title`,
    str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), '&node=') as `category`,
    cast(dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as varchar) as `categorypath`,
    dom_first_text(dom, '#wayfinding-breadcrumbs_container ul li:last-child a') as `categoryname`,
    array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), '|') as `categorynamelevel`,
    dom_first_slim_html(dom, 'div#centerCol a#bylineInfo') as `brand`,
    cast(dom_all_slim_htmls(dom, '#imageBlock img') as varchar) as `gallery`,
    dom_first_attr(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width>400)', 'data-old-hires') as `imgsrc`,
    dom_first_attr(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width>400)', 'data-a-dynamic-image') as `dynamicimgsrcs`,
    dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width>400)') as `img`,
    dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as `listprice`,
    dom_first_text(dom, '#price tr td:matches(^Price) ~ td, #price_inside_buybox') as `price`,
    dom_first_text(dom, '#price #priceblock_dealprice, #price tr td:contains(Deal of the Day) ~ td') as `withdeal`,
    dom_first_text(dom, '#price #dealprice_savings .priceBlockSavingsString, #price tr td:contains(You Save) ~ td') as `yousave`,
    dom_first_text(dom, '#price_inside_buybox') as `buyboxprice`,

    str_is_not_empty(dom_first_text(dom, '#acBadge_feature_div i:contains(Best Seller)')) as `isbs`,
    str_is_not_empty(dom_first_text(dom, '#acBadge_feature_div span:contains(Amazon)')) as `isac`,
    str_is_not_empty(dom_first_text(dom, '#centerCol #couponBadgeRegularVpc')) as `iscoupon`,
    dom_first_text(dom, '#centerCol div i:contains(Prime Day Deal)') as `isprime`,

    to_json(map(
        'isbs', str_left(dom_first_text(dom, '#acBadge_feature_div i:contains(Best Seller)'), 8),
        'isac', str_left(dom_first_text(dom, '#acBadge_feature_div span:contains(Amazon)'), 8),
        'iscoupon', str_left(dom_first_text(dom, '#centerCol #couponBadgeRegularVpc'), 8),
        'isprime', str_left(dom_first_text(dom, '#centerCol div i:contains(Prime Day Deal)'), 8),
        'isaddcart', str_left(dom_first_text(dom, '#addToCart_feature_div span:contains(Add to Cart), #submit.add-to-cart-ubb-announce'), 8),
        'isbuy', str_left(dom_first_text(dom, '#buyNow span:contains(Buy now)'), 8),
        'isa', array_length(dom_all_imgs(dom, '#prodDetails img[src], #productDescription img[src]')),
        'iscpfb', str_left(dom_first_text(dom, '#climatePledgeFriendlyBadge'), 8),
        'isgone', str_left(dom_first_attr(dom, '#g a img[src~=error]', 'alt'), 8)
    )) as tags,

    cast(dom_all_texts(dom, 'a#sellerProfileTriggerId[href~=seller], #tabular-buybox tr:has(td:contains(Sold by)) td a[href~=seller], #usedbuyBox div:contains(Sold by) a[href~=seller], #merchant-info a[href~=seller], #buybox-tabular a[href~=seller]') as varchar) as `soldby`,
    cast(dom_all_hrefs(dom, 'a#sellerProfileTriggerId[href~=seller], #tabular-buybox tr:has(td:contains(Sold by)) td a[href~=seller], #usedbuyBox div:contains(Sold by) a[href~=seller], #merchant-info a[href~=seller], #buybox-tabular a[href~=seller]') as varchar) as `sellerID`,
    cast(dom_all_hrefs(dom, 'a#sellerProfileTriggerId[href~=seller], #tabular-buybox tr:has(td:contains(Sold by)) td a[href~=seller], #usedbuyBox div:contains(Sold by) a[href~=seller], #merchant-info a[href~=seller], #buybox-tabular a[href~=seller]') as varchar) as `marketplaceID`,
    cast(dom_all_texts(dom, '#desktop_buybox #merchant-info, #tabular-buybox tr:has(td:contains(Ships from)) td, #buybox-tabular tr:has(td:contains(Ships from)) td') as varchar) as `shipsfrom`,

    to_json(map(
       'globaldeliverto', dom_first_text(dom, '#glow-ingress-block'),
       'deliverto', dom_first_text(dom, '#contextualIngressPtLabel_deliveryShortLine, #glowContextualIngressPt_feature_div, div[data-feature-name=glowContextualIngressPt]')
    )) as `deliverto`,

    str_abbreviate(dom_first_text(dom, '#availability, #outOfStock'), 1024) as `instock`,
    dom_first_text(dom, '#selectQuantity select option:last-child') as `quantity`,
    cast(dom_all_hrefs(dom, '#availability a, #olp-upd-new-used a, #olp-upd-new a, #certified-refurbished-version a[href~=/dp/], #newer-version a[href~=/dp/]') as varchar) as `sellsameurl`,

    array_join_to_string(dom_all_attrs(dom, '#sims-fbt #sims-fbt-content ul li[data-p13n-asin-metadata]', 'data-p13n-asin-metadata'), '^|^') as `boughttogethermetadata`,
    array_join_to_string(dom_all_attrs(dom, '#sims-fbt #sims-fbt-content ul li[class~=sims-fbt-image] img', 'src'), '^|^') as `boughttogetherimgs`,
    array_join_to_string(dom_all_texts(dom, '#sims-fbt #sims-fbt-content ul li[data-p13n-asin-metadata]'), '^|^') as `boughttogether`,

    str_substring_between(dom_first_text(dom, '#olp-upd-new-used a, #olp-upd-new a'), '(', ')') as `othersellernum`,

    str_is_not_empty(dom_first_text(dom, '#addToCart_feature_div span:contains(Add to Cart), #submit.add-to-cart-ubb-announce')) as `isaddcart`,
    str_is_not_empty(dom_first_text(dom, '#buyNow span:contains(Buy now)')) as `isbuy`,

    cast(dom_all_slim_htmls(dom, '#productOverview_feature_div table') as varchar) as `overviewbullets`,
    cast(dom_all_slim_htmls(dom, '#detailBullets_feature_div, #productDetails_detailBullets_sections1 table') as varchar) as `detailbullets`,
    cast(dom_all_slim_htmls(dom, '#feature-bullets ul li') as varchar) as `featurebullets`,
    dom_first_text(dom, '#productDescription, h2:contains(Product Description) + div') as `desc`,

    cast(dom_all_slim_htmls(dom, '#prodDetails h1:contains(Feedback) ~ div a') as varchar) as `feedbackurl`,
    dom_first_text(dom, '#prodDetails table tr > th:contains(ASIN) ~ td, #detailBullets_feature_div ul li span:contains(ASIN) ~ span') as `asin`,

    dom_first_text(dom_owner_body(dom), '#pulsarJsVariables pre.parentAsin') as `pasin`,
    dom_first_text(dom_owner_body(dom), '#pulsarJsVariables pre.num_total_variations') as `totalvariations`,
    dom_first_text(dom_owner_body(dom), '#pulsarJsVariables pre.jsVariables') as `jsVariables`,

    dom_first_text(dom, '#prodDetails table tr > th:contains(Product Dimensions) ~ td, #detailBullets_feature_div ul li span:contains(Package Dimensions) ~ span') as `volume`,
    dom_first_text(dom, '#prodDetails table tr > th:contains(Item Weight) ~ td') as `weight`,
    dom_outer_html(dom_select_first(dom, '#prodDetails table tr > th:contains(Best Sellers Rank) ~ td, #detailBullets_feature_div ul li span:contains(Best Sellers Rank)')) as `rank`,
    dom_first_text(dom, '#detailBullets_feature_div ul li span:contains(Best Sellers Rank)') as `rank2`,
    dom_first_text(dom, '#prodDetails table tr > th:contains(Date First) ~ td, #detailBullets_feature_div ul li span:contains(Date First Available) ~ span') as `onsaletime`,
    cast(dom_all_attrs(dom, '#prodDetails img[src], #productDescription img[src], #dpx-aplus-product-description_feature_div img[src], #dpx-aplus-3p-product-description_feature_div img[src]', 'src') as varchar) as `detailimgs`,
    cast(dom_all_hrefs(dom, '#rvs-vse-related-videos ol li a[href~=/vdp/]') as varchar) as `detailvideos`,

    array_length(dom_all_imgs(dom, '#prodDetails img[src], #productDescription img[src]')) as `isa`,

    str_first_integer(dom_first_text(dom, '#askATFLink, .askTopQandALoadMoreQuestions a'), 0) as `qanum`,
    str_first_integer(dom_first_text(dom, '#acrCustomerReviewText, #reviewsMedley div[data-hook=total-review-count] span, #reviewsMedley span:contains(ratings), #reviewsMedley span:contains(customer ratings)'), 0) as `reviews`,

    cast(dom_all_texts(dom, '#reviewsMedley div[data-hook=lighthut-terms-list] a, #reviewsMedley h3:contains(Read reviews that mention) ~ div a') as varchar) as `reviewsmention`,
    str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as `score`,
    str_first_integer(dom_first_text(dom, '#reviewsMedley div[data-hook=total-review-count], #acrCustomerReviewText'), 0) as `starnum`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(5 star) ~ td:contains(%)') as `score5percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(4 star) ~ td:contains(%)') as `score4percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(3 star) ~ td:contains(%)') as `score3percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(2 star) ~ td:contains(%)') as `score2percent`,
    dom_first_text(dom, 'table#histogramTable:expr(width > 100) td:contains(1 star) ~ td:contains(%)') as `score1percent`,
    dom_all_texts(dom, 'div#cr-dp-summarization-attributes div[data-hook=cr-summarization-attribute]') as `scoresbyfeature`,
    dom_first_href(dom, '#reviews-medley-footer a') as `reviewsurl`,

    dom_first_attr(dom_owner_document(dom), 'head meta[name=keywords]', 'content') as `meta_keywords`,
    dom_first_attr(dom_owner_document(dom), 'head meta[name=description]', 'content') as `meta_description`,

    dom_first_attr(dom, '#PulsarMetaInformation', 'href') as `href`,
    dom_first_attr(dom, '#PulsarMetaInformation', 'referer') as `referer`,
    dom_first_attr(dom, '#PulsarMetaInformation', 'label') as `label`,
    time_first_mysql_date_time(dom_attr(dom_select_first(dom_owner_body(dom), '#PulsarMetaInformation'), 'taskTime')) as `task_time`,

    dom_ch(dom) as `numchars`,
    dom_a(dom) as `numlinks`,
    dom_img(dom) as `numimgs`,
    dom_height(dom) as `height`
from load_and_select(@url, ':root body');
