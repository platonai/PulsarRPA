-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 热卖产品排行

-- 品类-关键词商品监控表
CREATE TABLE `keyword_asin` (
    `nodeID` VARCHAR(255) NULL COMMENT '品类ID', -- new add
    `keyword` VARCHAR(255) NULL COMMENT '关键词',
    `price` FLOAT  DEFAULT 0.0 COMMENT 'price',
    `listprice` FLOAT  DEFAULT 0.0 COMMENT '挂牌价',
    `title` varchar(255) DEFAULT NULL COMMENT '名称',
    `pic` VARCHAR(255) NULL COMMENT '图片URL',
    `score` FLOAT  DEFAULT 0.0 COMMENT '平均评星数',
    `starnum` INT DEFAULT 0 COMMENT '评星总数',
    `reviews` INT  DEFAULT 0 COMMENT '评论数',
    `is_have_rating` tinyint DEFAULT 0 COMMENT '是否有评分数 -- 先用该字段来判断评论是否被清空',
    `asin` VARCHAR(255) NULL COMMENT '商品asin编码',
    `isac` tinyint DEFAULT 0 COMMENT '是否amazon推荐',
    `isbs` tinyint DEFAULT 0 COMMENT '是否热卖排行榜',
    `iscoupon` tinyint DEFAULT 0 COMMENT '是否使用优惠券',
    `stock_status`  tinyint DEFAULT 0 COMMENT '是否缺货 0 不缺货  1  缺货  2 即将缺货 ',  -----  new add
    `instock`  VARCHAR(255) NULL COMMENT '是否缺货 0 不缺货  1  缺货  2 即将缺货 ',  -----  new fix
    `left_stock` int DEFAULT 0 COMMENT '剩余库存',  ---- new  add
    `out_stock_time` VARCHAR(255) NULL COMMENT '即将缺货时间',  ---- new  add
    `score5percent` VARCHAR(255) NULL COMMENT '5星级占比',-- new  add
    `score4percent` VARCHAR(255) NULL COMMENT '4星级占比',-- new  add
    `score3percent` VARCHAR(255) NULL COMMENT '3星级占比',-- new  add
    `score2percent` VARCHAR(255) NULL COMMENT '2星级占比',-- new  add
    `score1percent` VARCHAR(255) NULL COMMENT '1星级占比',-- new  add
    `isad` tinyint DEFAULT 0 COMMENT '是否列表广告推广',-- new  add
    `adposition_page` VARCHAR(255) NULL COMMENT '列表广告位置页码',-- new  add
    `adposition_page_row` VARCHAR(255) NULL COMMENT '列表广告位置  页码行数',-- new  add
    `is_recently` tinyint(4) DEFAULT '0' COMMENT '是否最新更新的数据',  -- new add
    `createtime` VARCHAR(255) NULL COMMENT '更新日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='关键词ASIN表';
