-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 心愿产品排行
-- https://www.amazon.com/gp/most-wished-for/ref=zg_bsms_tab

CREATE TABLE `asin_most_wished` (
    `asin` VARCHAR(255) NULL COMMENT 'asin',
    `category` varchar(255) DEFAULT NULL COMMENT "所属分类nodeID",
    `category_url` varchar(255) DEFAULT NULL COMMENT "所属分类 url",
    `rank` INT  DEFAULT 0  COMMENT '自然排名',
    `price` FLOAT  DEFAULT 0.0 COMMENT 'price',
    `title` varchar(255) DEFAULT NULL COMMENT '名称',
    `pic` VARCHAR(255) NULL COMMENT '图片URL',
    `score` FLOAT  DEFAULT 0.0 COMMENT '平均评星数',
    `starnum` INT  DEFAULT 0 COMMENT '评星总数',
    `create_time` INT(11) DEFAULT 0 COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Our products most often added to Wishlists and Registries. Updated daily.';

DROP TABLE IF EXISTS `asin_most_wished_sync`;
DROP TABLE IF EXISTS `asin_most_wished_for_sync`;
CREATE TABLE `asin_most_wished_for_sync` (
    `url` VARCHAR(2048) NULL COMMENT '页面 url',
    `asin` VARCHAR(512) NULL COMMENT 'asin',
    `category` varchar(512) DEFAULT NULL COMMENT "所属分类nodeID",
    `category_url` varchar(2047) DEFAULT NULL COMMENT "所属分类 url",
    `rank` varchar(512) NULL COMMENT '自然排名',
    `price` varchar(512) NULL COMMENT 'price',
    `title` varchar(512) DEFAULT NULL COMMENT '名称',
    `pic` VARCHAR(2048) NULL COMMENT '图片URL',
    `score` VARCHAR(512) NULL COMMENT '平均评星数',
    `starnum` VARCHAR(512) NULL COMMENT '评星总数',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
) ENGINE=MYISAM DEFAULT CHARSET=utf8mb4;
