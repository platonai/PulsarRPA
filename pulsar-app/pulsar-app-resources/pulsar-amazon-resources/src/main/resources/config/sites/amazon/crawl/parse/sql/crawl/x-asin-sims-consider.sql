-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_base_uri(dom) as url,
    amazon_find_asin(dom_base_uri(dom)) as asin,
    dom_first_text(dom, 'h4') as carousel_title,
    dom_first_href(dom, 'div#value-pick-title-row a') as ad_asin_url,
    dom_first_text(dom, 'div#value-pick-title-row a') as ad_asin_title,
    dom_first_text(dom, 'div.a-row span.a-color-price') as ad_asin_price,
    dom_first_attr(dom, 'img#value-pick-image, img[src~=images]', 'src') as ad_asin_img,
    str_substring_after(dom_first_attr(dom, 'div.a-row i.a-icon-star', 'class'), 'a-star-') as ad_asin_score,
    str_substring_between(dom_first_text(dom, 'div.a-row i.a-icon-star ~ a[href~=reviews]'), '(', ')') as ad_asin_starnum,
    dom_attr(dom_select_first(dom_owner_body(dom), '#PulsarMetaInformation'), 'label') as `label`,
    time_first_mysql_date_time(dom_attr(dom_select_first(dom_owner_body(dom), '#PulsarMetaInformation'), 'taskTime')) as `task_time`,
    'sims-consider' as `ad_type`
from load_and_select(@url, '#valuePick_feature_div');
