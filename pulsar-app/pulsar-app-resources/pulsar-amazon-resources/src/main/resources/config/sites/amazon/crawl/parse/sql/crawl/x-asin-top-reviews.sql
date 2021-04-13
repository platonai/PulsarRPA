-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_base_uri(dom) as `url`,
    amazon_find_asin(dom_base_uri(dom)) as asin,
    dom_first_href(dom, 'a.review-title-content, a[data-hook=review-title]') as `reviews_url`,
    dom_first_text(dom_owner_body(dom), '#filter-info-section div[data-hook=cr-filter-info-review-rating-count], #filter-info-section') as `ratingcount`,
    dom_attr(dom, 'id') as `comment_id`,
    dom_first_text(dom, '.review-date, span[data-hook=review-date]') as `comment_time`,
    dom_first_text(dom, 'a.a-profile[href~=profile] .a-profile-name') as `comment_name`,
    dom_first_text(dom, 'a.review-title-content, a[data-hook=review-title]') as `comment_title`,
    dom_first_href(dom, 'a.a-profile[href~=profile]') as `comment_name_url`,
    dom_first_href(dom, 'a[data-hook=format-strip]') as `sku_asin`,
    dom_first_text(dom, 'a span[data-hook=avp-badge], a span:containsOwn(Verified)') as `is_verified`,
    dom_first_text(dom, 'div.a-profile-content span.a-profile-descriptor, div.a-profile-content span:containsOwn(Top Contributor)') as `is_top_contributor`,
    dom_first_text(dom, 'div.genome-widget-row a span:containsOwn(VINE VOICE)') as `is_vine_voice`,
    dom_first_text(dom, '.review-text-content, span[data-hook=review-body]') as `content`,
    str_first_float(dom_first_text(dom, 'a[title~=out of], i[data-hook=review-star-rating]'), 0.0) as `score`,
    cast(dom_img(dom_select_first(dom, 'div.review-image-tile-section')) as integer) as `ispic`,
    cast(dom_all_attrs(dom, 'div.review-image-tile-section img[data-hook=review-image-tile]', 'src') as varchar) as `pics`,
    str_first_integer(dom_first_text(dom, '.review-comments .cr-vote .cr-vote-text, span[data-hook=helpful-vote-statement]'), 0) as `helpfulnum`,

    dom_attr(dom_select_first(dom_owner_body(dom), '#PulsarMetaInformation'), 'label') as `label`,
    time_first_mysql_date_time(dom_attr(dom_select_first(dom_owner_body(dom), '#PulsarMetaInformation'), 'taskTime')) as `task_time`,

    dom_own_texts(dom_select_first(dom, 'a[data-hook=format-strip]')) as `sku`
from load_and_select(@url, '#cm-cr-dp-review-list > div[data-hook=review]');
