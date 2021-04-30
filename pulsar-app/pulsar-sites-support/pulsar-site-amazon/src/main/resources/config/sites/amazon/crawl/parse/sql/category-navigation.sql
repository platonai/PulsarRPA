-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_base_uri(dom) as `url`,
    dom_first_text(dom, 'div.a-box-inner span:containsOwn(results for) , div.a-box-inner span:containsOwn(results), div.sg-col-inner div.a-section span:containsOwn(results for) , div.sg-col-inner div.a-section span:containsOwn(results)') as `results`,
    array_join_to_string(dom_all_texts(dom, 'ul.a-pagination > li, div#pagn > span'), '|') as `pagination`
from load_and_select(@url, 'body');
