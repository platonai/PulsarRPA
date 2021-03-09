-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 新品产品排行
-- https://www.amazon.com/gp/new-releases/ref=zg_bs_tab

CREATE TABLE `asin_new` (
    `asin` VARCHAR(255) NULL COMMENT 'asin',
    `category` varchar(255) DEFAULT NULL COMMENT "所属分类nodeID",
    `category_url` varchar(255) DEFAULT NULL COMMENT "所属分类 url",
    `rank` INT  DEFAULT 0  COMMENT '自然排名',
    `price` FLOAT DEFAULT 0.0 NULL COMMENT 'price',
    `title` varchar(255) DEFAULT NULL COMMENT '名称',
    `pic` VARCHAR(255) NULL COMMENT '图片URL',
    `score` FLOAT DEFAULT 0.0 COMMENT '平均评星数',
    `starnum` INT DEFAULT 0 COMMENT '评星总数',
    `create_time` INT(11) DEFAULT 0 COMMENT '更新时间',
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='asin 新品榜 我们最新推出和将要推出的畅销商品  官网是 hourly update  我们1小时更新一次';

DROP TABLE IF EXISTS `asin_new_releases_sync`;
CREATE TABLE `asin_new_releases_sync` (
    `url` VARCHAR(2048) NULL COMMENT '页面 url',
    `asin` VARCHAR(512) NULL COMMENT 'asin',
    `category` varchar(512) NULL COMMENT "所属分类nodeID",
    `category_url` varchar(2048) NULL COMMENT "所属分类 url",
    `rank` varchar(512) NULL COMMENT '排名',
    `offers` varchar(512) NULL COMMENT 'offers from',
    `price` varchar(512) NULL COMMENT 'price',
    `title` varchar(512) NULL COMMENT '名称',
    `pic` varchar(2048) NULL COMMENT '图片URL',
    `score` varchar(512) NULL COMMENT '平均评星数',
    `starnum` varchar(512) NULL COMMENT '评星总数',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
) ENGINE=MYISAM DEFAULT CHARSET=utf8mb4;
