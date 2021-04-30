-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin 所在页面 广告位 追踪
-- set @url='https://www.amazon.com/dp/B0113UZJE2'
select dom_base_uri(dom)                                                                    as url,
       amazon_find_asin(dom_base_uri(dom))                                                        as asin,
       dom_first_own_text(dom_owner_body(dom),
                          '#sims-consolidated-1_feature_div h2.a-carousel-heading, ' ||
                          'div[cel_widget_id~=sims-consolidated-1] h2.a-carousel-heading'
         )                                                                                  as carousel_title,
       dom_first_text(dom_owner_body(dom),
                      '#sims-consolidated-1_feature_div h2.a-carousel-heading div.sp_desktop_sponsored_label, ' ||
                      'div[cel_widget_id~=sims-consolidated-1] h2.a-carousel-heading div.sp_desktop_sponsored_label'
         )                                                                                  as is_sponsored,
       dom_element_sibling_index(dom)                                                       as ad_asin_position,
       dom_first_href(dom,
                      'div[data-asin] a[href~=/dp/], div[data-asin] a[href~=/slredirect/]') as ad_asin_url,
       dom_first_text(dom, 'div[data-asin] a div:expr(img=0 && char>30)')                   as ad_asin_title,
       dom_first_text(dom, 'div[data-asin] a span.a-color-price')                           as ad_asin_price,
       dom_first_attr(dom, 'div[data-asin] a img[data-a-dynamic-image]',
                      'src')                                                                as ad_asin_img,
       dom_first_text(dom, 'div[data-asin] > div > a i.a-icon-star')                        as ad_asin_score,
       str_substring_after(dom_first_attr(dom, 'div[data-asin] > div > a i.a-icon-star', 'class'),
                           ' a-star-')                                                      as ad_asin_score_2,
       dom_first_text(dom, 'div[data-asin] > div > a i.a-icon-star ~ span') as ad_asin_starnum,
       dom_attr(dom_select_first(dom_owner_body(dom), '#PulsarMetaInformation'),
                'label')                                                                    as `label`,
       time_first_mysql_date_time(dom_attr(dom_select_first(dom_owner_body(dom), '#PulsarMetaInformation'),
                                           'taskTime'))                                     as `task_time`,
       'sims-1'                                                                             as `ad_type`
from load_and_select(@url,
                     '#sims-consolidated-1_feature_div ol.a-carousel li, ' ||
                     'div[cel_widget_id~=sims-consolidated-1] ol.a-carousel li'
  );
