-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- category-asin-extract.sql
-- set @url='https://www.amazon.com/b?node=16225007011&pf_rd_r=345GN7JFE6VHWVT896VY&pf_rd_p=e5b0c85f-569c-4c90-a58f-0c0a260e45a0'
select
    dom_base_uri(dom) as `url`,
    str_substring_between(dom_base_uri(dom), 'node=', '&') as `nodeID`,
    dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
    dom_first_text(dom, 'a:has(span.a-price) ~ span.a-text-strike') as `listprice`,
    dom_first_text(dom, 'a[title] h2') as `title`,
    dom_first_href(dom, 'a[title]:has(h2)') as `asin_url`,
    dom_first_text(dom, 'div span[class]:containsOwn(by) ~ span[class~=a-color-secondary]') as `brand`,
    dom_first_text(dom, 'div a:containsOwn(new offers), div a span:containsOwn(new offers)') as `follow_seller_num`,
    dom_first_attr(dom, 'a img[srcset]', 'src') as `pic`,
    dom_first_float(dom, 'div.a-row:expr(a>0) i[class~=a-icon-star]', 0.0) as `score`,
    'N/A' as `starnum`,
    dom_first_text(dom, 'div.a-row:expr(a>0) span[name] ~ a') as `reviews`,
    str_substring_between(dom_first_href(dom, 'a[title]:has(h2)'), '/dp/', '/ref=') as `asin`,
    dom_first_text(dom, 'div span:containsOwn(Amazon)') as `isac`,
    dom_first_text(dom, 'div span:containsOwn(Best Seller)') as `isbs`,
    dom_first_text(dom, 'span[data-component-type=s-coupon-component]') as `iscoupon`,
    dom_first_text(dom, 'div span:containsOwn(in stock), div span:containsOwn(More Buying Choices)') as `instock`,
    dom_first_text(dom, 'div span:containsOwn(Sponsored)') as `isad`,
    dom_first_text(dom_owner_body(dom), 'ul.a-pagination li.a-selected, #pagn span[class~=pagnCur]') as `adposition_page`,
    (dom_top(dom) - dom_top(dom_select_first(dom_owner_body(dom), '#mainResults ul.s-result-list'))) / dom_height(dom) as `adposition_page_row`,
    dom_element_sibling_index(dom) as `position_in_list`,
    dom_width(dom_select_first(dom, 'a img[srcset]')) as `pic_width`,
    dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
from load_and_select(@url, '#mainResults ul.s-result-list > li[data-asin]');
