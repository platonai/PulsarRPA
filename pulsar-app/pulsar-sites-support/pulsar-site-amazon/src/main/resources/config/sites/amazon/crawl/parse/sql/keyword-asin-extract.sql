-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_base_uri(dom) as `url`,
    str_substring_after(dom_base_uri(dom), '&rh=') as `nodeID`,
    str_substring_between(dom_base_uri(dom), 'k=', '&') as `keyword`,
    dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
    dom_first_text(dom, 'a:has(span.a-price) span:containsOwn(/Item)') as `priceperitem`,
    dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
    dom_first_text(dom, 'h2 a') as `title`,
    dom_first_href(dom, 'h2 a') as `asin_url`,
    str_substring_between(dom_first_href(dom, 'h2 a'), '/dp/', '/ref=') as `asin`,
    dom_first_text(dom, 'div span[class]:containsOwn(by) ~ span[class~=a-color-secondary]') as `brand`,
    dom_first_text(dom, 'div a:containsOwn(new offers), div a span:containsOwn(new offers)') as `follow_seller_num`,

    dom_first_attr(dom, 'span[data-component-type=s-product-image] a img', 'src') as `pic`,
    dom_first_float(dom, 'div.a-section:expr(a>0) span[aria-label~=stars] i span', 0.0) as score,
    dom_first_text(dom, 'div.a-section:expr(a>0) span[aria-label~=stars] ~ span a') as `reviews`,
    dom_first_text(dom, 'div span[aria-label~=Amazon], div span:containsOwn(Amazon)') as `isac`,
    dom_first_text(dom, 'a[id~=BESTSELLER], div a[href~=bestsellers], div span:containsOwn(Best Seller)') as `isbs`,
    dom_first_text(dom, 'span[data-component-type=s-coupon-component]') as `iscoupon`,
    dom_first_text(dom, 'div.a-section span:contains(in stock), div.a-section span:contains(More Buying Choices)') as `instock`,
    dom_first_text(dom, 'div span:containsOwn(Sponsored)') as `isad`,
    dom_first_href(dom, 'div.a-section a[href~=events]') as `holidaygiftguide`,
    dom_first_text(dom, 'div.a-section span:containsOwn(Limited time deal)') as `limitedtimedeal`,
    dom_first_attr(dom, 'div.a-section i.a-icon-prime', 'aria-label') as `isprime`,
    dom_first_text(dom, 'div.a-section span[aria-label~=soon] span:last-child') as `getassoonas`,
    dom_first_text(dom, 'div.a-section span:containsOwn(Shipping)') as `shipping`,
    dom_first_text(dom, 'div.a-section span:containsOwn(Arrives)') as `arrives`,
    dom_first_text(dom_owner_body(dom), 'ul.a-pagination li.a-selected') as `adposition_page`,
    (dom_top(dom) - dom_top(dom_select_first(dom_owner_body(dom), 'div.s-main-slot.s-result-list.s-search-results'))) / dom_height(dom) as `adposition_page_row`,
    dom_element_sibling_index(dom) as `position_in_list`,
    dom_width(dom_select_first(dom, 'a img[srcset]')) as `pic_width`,
    dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
from load_and_select(@url, 'div.s-main-slot.s-result-list.s-search-results > div:expr(img>0)');
