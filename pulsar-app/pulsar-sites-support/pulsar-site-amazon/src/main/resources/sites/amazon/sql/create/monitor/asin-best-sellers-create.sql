-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 热卖产品排行
-- https://www.amazon.com/Best-Sellers/zgbs/ref=zg_bsms_tab

CREATE TABLE `asin_best` (
    `asin` VARCHAR(255) NULL COMMENT 'asin',
    `category` varchar(255) DEFAULT NULL COMMENT "所属分类nodeID",
    `category_url` varchar(255) DEFAULT NULL COMMENT "所属分类 url",
    `rank` int DEFAULT 0 DEFAULT NULL COMMENT '排名',
    `price` FLOAT DEFAULT 0.0 NULL COMMENT 'price',
    `title` varchar(255) DEFAULT NULL COMMENT '名称',
    `pic` VARCHAR(255) NULL COMMENT '图片URL',
    `score` FLOAT DEFAULT 0.0 COMMENT '平均评星数',
    `starnum` INT DEFAULT 0 COMMENT '评星总数',
    `is_recently` tinyint(4) DEFAULT 0 COMMENT '是否最新更新的数据',  -- new add
    `create_time` INT(11) DEFAULT 0 COMMENT '更新时间--保留到天'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='asin 热卖榜 基于销售量而定的最受欢迎商品  官网是 hourly update  我们12小时更新一次';

DROP TABLE IF EXISTS `asin_best_sellers_sync`;
CREATE TABLE `asin_best_sellers_sync` (
    `url` VARCHAR(2048) NULL COMMENT '页面 url',
    `asin` VARCHAR(512) NULL COMMENT 'asin',
    `category` varchar(512) NULL COMMENT "所属分类nodeID",
    `category_url` varchar(2048) NULL COMMENT "所属分类 url",
    `rank` varchar(512) NULL COMMENT '排名',
    `price` varchar(512) NULL COMMENT 'price',
    `title` varchar(512) NULL COMMENT '名称',
    `pic` varchar(2048) NULL COMMENT '图片URL',
    `score` varchar(512) NULL COMMENT '平均评星数',
    `starnum` varchar(512) NULL COMMENT '评星总数',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
) ENGINE=MYISAM DEFAULT CHARSET=utf8mb4;
