-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin 所在页面 广告位 追踪
-- set @url='https://www.amazon.com/Etekcity-Multifunction-Stainless-Batteries-Included/dp/B0113UZJE2/ref=zg_bs_home-garden_21?_encoding=UTF8&psc=1&refRID=TS59NMS2K6A2PSXTTS4F';
select
    dom_base_uri(dom) as url,
    str_substring_between(dom_base_uri(dom), '/dp/', '/ref=') as asin,
    dom_first_text(dom, 'h2') as section_title,
    dom_element_sibling_index(dom) as ad_asin_position,
    str_substring_between(dom_first_href(dom, 'div a[href~=/dp/]'), '/dp/', '/ref=') as ad_asin,
    str_substring_between(dom_first_href(dom, 'div a[href~=/dp/]'), '/dp/', '/ref=') as ad_asin_bsr,
    dom_first_href(dom, 'div a[href~=/dp/]') as ad_asin_url,
    dom_first_text(dom, 'div a div:expr(img=0 && char>30)') as ad_asin_title,
    dom_first_text(dom, 'div a span.a-color-price') as ad_asin_price,
    dom_first_attr(dom, 'div a img[data-a-dynamic-image]', 'src') as ad_asin_img,
    str_substring_after(dom_first_attr(dom, 'div > div > a i.a-icon-star', 'class'), ' a-star-') as ad_asin_score,
    dom_first_text(dom, 'div a:contains(out of 5 stars) ~ a[href~=reviews]') as ad_asin_starnum,
    dom_attr(dom_select_first(dom, '#PulsarMetaInformation'), 'label') as `label`,
    time_first_mysql_date_time(dom_attr(dom_select_first(dom, '#PulsarMetaInformation'), 'taskTime')) as `task_time`,
    'also-view' as `ad_type`
from load_and_select(@url, '#sims-consolidated-1_feature_div ol.a-carousel li, #sims-consolidated-2_feature_div ol.a-carousel li, #sims-consolidated-3_feature_div ol.a-carousel li');
