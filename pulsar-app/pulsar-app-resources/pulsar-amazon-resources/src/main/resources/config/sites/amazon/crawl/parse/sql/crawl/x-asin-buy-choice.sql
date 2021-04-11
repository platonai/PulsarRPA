-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_uri(dom) as `url`,
    dom_base_uri(dom) as `baseUri`,

    dom_first_attr(dom, 'input[name=asin]', 'value') as `asin`,
    dom_first_text(dom, '#mbc-price-1') as `price`,
    dom_first_text(dom, '#mbc-shipping-temp-cfs-1') as `shipping`,
    dom_first_href(dom, '#mbc-shipping-temp-cfs-1 a') as `shippinghelp`,
    dom_first_text(dom, '#mbc-sold-by-1') as `soldby`,
    dom_first_text(dom, '#mbc-buybutton-addtocart-1') as `addtocart`
from load_and_select(@url, '#moreBuyingChoices_feature_div .mbc-offer-row');
