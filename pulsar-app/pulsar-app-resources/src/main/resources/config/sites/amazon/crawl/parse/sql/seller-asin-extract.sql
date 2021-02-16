-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin  跟卖列表
-- 每天的跟卖列表不同
-- https://www.amazon.com/s?me=A2QJQR8DPHL921&marketplaceID=ATVPDKIKX0DER
select
    dom_base_uri(dom) as `url`,
    str_substring_between(dom_base_uri(dom), '&me=', '&') as `sellerID`,
    str_substring_between(dom_base_uri(dom), 'marketplaceID=', '&') as `marketplaceID`,
    dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
    dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
    dom_first_text(dom, 'h2 a') as `title`,
    dom_first_attr(dom, 'span[data-component-type=s-product-image] a img', 'src') as `pic`,
    dom_first_float(dom, 'div.a-section:expr(a>0) span[aria-label~=star] i span', 0.0) as score,
    dom_first_text(dom, 'div.a-section:expr(a>0) span[aria-label~=star] ~ span a') as starnum,
    dom_first_text(dom, 'div.reviews') as `reviews`,
    str_substring_between(dom_first_href(dom, 'h2 a'), '/dp/', '/ref=') as `asin`,
    dom_first_text(dom, 'div span[aria-label~=Amazon], div span:containsOwn(Amazon)') as `isac`,
    dom_first_text(dom, 'div span[aria-label~=Best], div span:containsOwn(Best Seller)') as `isbs`,
    dom_first_text(dom, 'span[data-component-type=s-coupon-component]') as `iscoupon`,
    dom_first_text(dom, 'div.a-section span:contains(in stock), div.a-section span:contains(More Buying Choices)') as `instock`,
    dom_first_text(dom, 'div span:containsOwn(Sponsored)') as `isad`,
    dom_first_text(dom_owner_body(dom), 'ul.a-pagination li.a-selected') as `adposition_page`,
    (dom_top(dom) - dom_top(dom_select_first(dom_owner_body(dom), 'div.s-main-slot.s-result-list.s-search-results'))) / dom_height(dom) as `adposition_page_row`
from load_and_select(@url, 'div.s-main-slot.s-result-list.s-search-results > div:expr(img>0)');
