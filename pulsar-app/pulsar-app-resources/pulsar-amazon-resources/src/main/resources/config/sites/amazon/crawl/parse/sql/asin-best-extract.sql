-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 热卖产品排行
-- https://www.amazon.com/Best-Sellers/zgbs/ref=zg_bs_unv_0_7147440011_2
-- CREATE TABLE `asin_best` (
--     `asin` VARCHAR(255) NULL COMMENT 'asin',
--     `category` varchar(255) DEFAULT NULL COMMENT "所属分类nodeID",-- new add
--     `rank` int DEFAULT 0 DEFAULT NULL COMMENT '排名',
--     `price` FLOAT DEFAULT 0.0 NULL COMMENT 'price',
--     `title` varchar(255) DEFAULT NULL COMMENT '名称',
--     `pic` VARCHAR(255) NULL COMMENT '图片URL',
--     `score` FLOAT DEFAULT 0.0 COMMENT '平均评星数',
--     `starnum` INT DEFAULT 0 COMMENT '评星总数',
--     `is_recently` tinyint(4) DEFAULT 0 COMMENT '是否最新更新的数据',  -- new add
--     `create_time` VARCHAR(255) NULL COMMENT '更新时间--保留到天'
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='asin 热卖榜 基于销售量而定的最受欢迎商品  官网是 hourly update  我们12小时更新一次';

-- set @url='https://www.amazon.com/Best-Sellers-Toys-Games/zgbs/toys-and-games';
select
    dom_base_uri(dom) as url,
    str_substring_between(dom_first_href(dom, 'span.zg-item a'), '/dp/', '/ref=') as `asin`,
    str_substring_between(dom_base_uri(dom), '.com/', '/ref=') as `category`,
    dom_first_integer(dom, 'span.zg-badge-text', 0) as `rank`,
    dom_first_text(dom, 'div > a > span.a-color-price') as `price`,
    dom_first_text(dom, 'span.zg-item a > div:expr(img=0 && char>30)') as title,
    dom_first_attr(dom, 'span.zg-item div img[src]', 'src') as `pic`,
    str_substring_between(dom_first_attr(dom, 'span.zg-item div a i.a-icon-star', 'class'), ' a-star-', ' ') as score,
    dom_first_text(dom, 'span.zg-item div a:has(i.a-icon-star) ~ a') as starnum
from load_and_select(@url, 'ol#zg-ordered-list > li.zg-item-immersion');
