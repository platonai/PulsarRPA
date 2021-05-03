-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_base_uri(dom) as url,
    amazon_find_asin(dom_first_href(dom, 'span.zg-item a')) as `asin`,
    dom_first_href(dom, 'a[href~=/dp/]') as `asin_url`,
    str_substring_between(dom_base_uri(dom), 'zgbs/', '/') as `categoryinurl`,
    array_join_to_string(dom_all_texts(dom_owner_body(dom), 'ul#zg_browseRoot li.zg_browseUp'), ' > ') as `categorylevel`,
    dom_first_text(dom_owner_body(dom), 'ul#zg_browseRoot .zg_selected') as `selectedcategory`,
    dom_base_uri(dom) as `category_url`,
    dom_first_integer(dom, 'span.zg-badge-text', 0) as `rank`,
    dom_first_text(dom, 'div > a > span.a-color-price') as `price`,
    dom_first_text(dom, 'span.zg-item a > div:expr(img=0 && char>30)') as `title`,
    dom_first_attr(dom, 'span.zg-item div img[src]', 'src') as `pic`,
    str_substring_between(dom_first_attr(dom, 'span.zg-item div a i.a-icon-star', 'class'), ' a-star-', ' ') as `score`,
    dom_first_text(dom, 'span.zg-item div a:has(i.a-icon-star) ~ a') as `starnum`,
    time_first_mysql_date_time(dom_attr(dom_select_first(dom_owner_body(dom), '#PulsarMetaInformation'), 'taskTime')) as `task_time`
from load_and_select(@url, 'ol#zg-ordered-list > li.zg-item-immersion');
