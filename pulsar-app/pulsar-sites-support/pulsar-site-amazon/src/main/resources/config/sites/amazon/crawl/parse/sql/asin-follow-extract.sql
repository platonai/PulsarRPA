-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin  跟卖列表
select
    dom_base_uri(dom) as url,
    dom_first_text(dom_owner_body(dom), 'div#olpProductDetails h1') as name,
    dom_first_text(dom_owner_body(dom), 'div#olpProductDetails i.a-icon-star') as globalstars,
    dom_first_text(dom_owner_body(dom), 'div#olpProductDetails a[href~=product-reviews]') as globalcustomratings,
    dom_first_img(dom_owner_body(dom), 'div#olpProductImage img[src]') as img,
    str_substring_between(dom_base_uri(dom), '/offer-listing/', '/ref=') as asin,

    cast(dom_all_slim_htmls(dom, 'div.olpDeliveryColumn ul li') as varchar) as delivery,
    dom_first_text(dom, 'div.olpDeliveryColumn .olpBadge') as deliverybadge,

    dom_first_text(dom, 'div.olpSellerColumn h3.olpSellerName') as soldby,
    dom_first_href(dom, 'div.olpSellerColumn h3.olpSellerName a') as sellerID,
    dom_first_href(dom, 'div.olpSellerColumn h3.olpSellerName a') as seller_url,
    dom_first_text(dom, 'div.olpPriceColumn span') as price,
    dom_first_text(dom, 'div.olpPriceColumn p.olpShippingInfo') as shipby,
    dom_first_href(dom, 'div.olpPriceColumn p.olpShippingInfo a') as shippingdetail,
    dom_first_text(dom, 'div.olpSellerColumn i.a-icon-star') as stars,
    dom_first_slim_html(dom, 'div.olpSellerColumn p:has(i[class~=a-star]) a') as ratinglink,
    dom_first_text(dom, 'div.olpSellerColumn p:has(i[class~=a-star]) a') as ratingpercentage,
    dom_own_text(dom_select_first(dom, 'div.olpSellerColumn p:has(i[class~=a-star])')) as reviews,
    dom_first_text(dom, 'div.olpConditionColumn') as is_out
from load_and_select(@url, '#olpOfferList div.olpOffer[role=row]');
