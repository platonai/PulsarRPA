-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 潜力产品排行
-- https://www.amazon.com/gp/movers-and-shakers/ref=zg_bsnr_tab

CREATE TABLE `asin_movers_shakers` (
    `asin` VARCHAR(255) NULL COMMENT 'asin',
    `category` varchar(255) DEFAULT NULL COMMENT "所属分类nodeID",
    `category_url` varchar(255) DEFAULT NULL COMMENT "所属分类 url",
    `rank` INT  DEFAULT 0  COMMENT '自然排名',
    `price` FLOAT  DEFAULT 0.0 COMMENT 'price',
    `title` varchar(255) DEFAULT NULL COMMENT '名称',
    `pic` VARCHAR(255) NULL COMMENT '图片URL',
    `score` FLOAT  DEFAULT 0.0 COMMENT '平均评星数',
    `starnum` INT  DEFAULT 0 COMMENT '评星总数',
    `bsr_old` INT  DEFAULT 0 COMMENT '之前bsr排名',
    `bsr_now` INT  DEFAULT 0 COMMENT '现在bsr排名',
    `bsr_change_rate`  VARCHAR(255) NULL COMMENT '现在bsr排名',
    `create_time` INT(11) DEFAULT 0 COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='asin 潜力榜 在过去的 24 小时之内更高的销售冠军  官网是 hourly update  我们1小时更新一次';

DROP TABLE IF EXISTS `asin_movers_shakers_async`;
DROP TABLE IF EXISTS `asin_movers_and_shakers_sync`;
CREATE TABLE `asin_movers_and_shakers_sync` (
    `url` VARCHAR(2048) NULL COMMENT '页面 url',
    `asin` VARCHAR(512) NULL COMMENT 'asin',
    `category` varchar(512) NULL COMMENT "所属分类nodeID",
    `category_url` varchar(2048) NULL COMMENT "所属分类 url",
    `rank` varchar(512) NULL COMMENT '自然排名',
    `price` varchar(512) NULL COMMENT 'price',
    `title` varchar(255) NULL COMMENT '名称',
    `pic` VARCHAR(2048) NULL COMMENT '图片URL',
    `score` varchar(255) NULL COMMENT '平均评星数',
    `starnum` varchar(255) NULL COMMENT '评星总数',
    `sales_rank` varchar(255) NULL COMMENT 'bsr排名',
    `sales_rank_change` varchar(255) NULL COMMENT '现在bsr排名',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
) ENGINE=MYISAM DEFAULT CHARSET=utf8mb4;
