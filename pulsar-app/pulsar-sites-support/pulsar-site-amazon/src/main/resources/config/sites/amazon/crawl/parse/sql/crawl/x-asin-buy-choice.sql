-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_uri(dom) as `url`,
    dom_base_uri(dom) as `baseUri`,

    dom_first_attr(dom, 'input[name=asin]', 'value') as `asin`,
    dom_first_text(dom, 'span[id~=mbc-price]') as `price`,
    dom_first_text(dom, 'span[id~=mbc-shipping]') as `shipping`,
    dom_first_href(dom, 'span[id~=mbc-shipping] a') as `shippinghelp`,
    dom_first_text(dom, 'div[id~=mbc-sold-by]') as `soldby`,
    dom_first_text(dom, 'span[id~=mbc-buybutton-addtocart]') as `addtocart`
from load_and_select(@url, '#moreBuyingChoices_feature_div .mbc-offer-row');
