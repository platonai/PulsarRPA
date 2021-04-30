-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

select
    dom_base_uri(dom) as `url`,
    dom_parse_tree1(dom, '#departments ul') as `category_tree`
from load_and_select(@url, ':root');
