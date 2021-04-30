-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 行业商品监控表
CREATE TABLE `category_asin` (
     `nodeID` VARCHAR(255) NULL COMMENT 'nodeID',
     `smallrank` INT default 0 COMMENT '小类排名',
     `bigrank` INT default 0  COMMENT '大类排名',
     `rank_level` VARCHAR(255) NULL COMMENT '所在不同品类的不同排名',   ---- new  add
     `day_sales` INT  default 0 COMMENT '日销量',
     `price` FLOAT default 0.0 COMMENT 'price',
     `listprice` FLOAT default 0.0 COMMENT '挂牌价',
     `title` varchar(255) DEFAULT NULL COMMENT '名称',
     `pic` VARCHAR(255) NULL COMMENT '图片URL',
     `score` FLOAT default 0.0 COMMENT '平均评星数',
     `starnum` INT default 0 COMMENT '评星总数',
     `asin` VARCHAR(255) DEFAULT NULL COMMENT '商品asin编码',
     `isac` tinyint DEFAULT 0 COMMENT '是否amazon推荐',
     `isbs` tinyint DEFAULT 0 COMMENT '是否热卖排行榜',
     `iscoupon` tinyint DEFAULT 0 COMMENT '是否使用优惠券',
     `instock`  VARCHAR(255) DEFAULT NULL  COMMENT '是否缺货 0 不缺货  1  缺货  2 即将缺货 ',  -----  new fix
     `left_stock` int DEFAULT 0 COMMENT '剩余库存',  ---- new  add
     `out_stock_time` VARCHAR(255)  DEFAULT NULL COMMENT '即将缺货时间',  ---- new  add
     `stock_status`  tinyint DEFAULT 0 COMMENT '是否缺货 0 不缺货  1  缺货  2 即将缺货 ',  -----  new add
     `is_have_rating` tinyint DEFAULT 0 COMMENT '是否有评分数 -- 先用该字段来判断评论是否被清空',
     `isad` tinyint DEFAULT 0 COMMENT '是否列表广告推广',-- new  add
     `adposition_page` VARCHAR(255) NULL COMMENT '列表广告位置页码',-- new  add
     `adposition_page_row` VARCHAR(255) NULL COMMENT '列表广告位置  页码行数',-- new  add
     `score5percent` VARCHAR(255) NULL COMMENT '5星级占比',-- new  add
     `score4percent` VARCHAR(255) NULL COMMENT '4星级占比',-- new  add
     `score3percent` VARCHAR(255) NULL COMMENT '3星级占比',-- new  add
     `score2percent` VARCHAR(255) NULL COMMENT '2星级占比',-- new  add
     `score1percent` VARCHAR(255) NULL COMMENT '1星级占比',-- new  add
     `is_recently` tinyint(4) DEFAULT '0' COMMENT '是否最新更新的数据',  -- new add
     `is_log` tinyint(4) DEFAULT '0' COMMENT '是否做过变化日志',  -- new add
     `updatetime` VARCHAR(255) NULL COMMENT '更新日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='卖家商品监控表';
