-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin 所在页面 广告位 追踪
CREATE TABLE `asin_ad` (
   `asin` VARCHAR(255) NULL COMMENT '商品asin编码',
   `ad_asin_position` varchar(255) DEFAULT NULL COMMENT '广告位置',
   `ad_asin` VARCHAR(255) NULL COMMENT '广告商品asin编码',
   `ad_asin_bsr` INT DEFAULT 0 NULL COMMENT '广告商品asin rank',
   `ad_asin_url` VARCHAR(255) NULL COMMENT '广告商品URL',
   `ad_asin_title` varchar(255) DEFAULT NULL COMMENT '广告商品名称',
   `ad_asin_price` VARCHAR(255) NULL COMMENT '广告商品价格',
   `ad_asin_img` VARCHAR(255) NULL COMMENT '广告商品图片',
   `ad_asin_score` FLOAT default 0.0 COMMENT '广告商品平均评星数',
   `ad_asin_starnum` INT default 0 COMMENT '广告商品评星总数',
   `ad_asin_soldby` VARCHAR(255) NULL COMMENT '广告商品卖家',
   `ad_asin_soldby_url` VARCHAR(255) NULL COMMENT '广告商品卖家url',
   `ad_asin_shipby` VARCHAR(255) NULL COMMENT '广告商品物流',
   `ad_type` VARCHAR(255) NULL COMMENT '广告类型: Sponsored/similar/also viewed/',
   `is_recently` tinyint(4) DEFAULT '0' COMMENT '是否最新更新的数据',  -- new add
   `createtime` VARCHAR(255) NOT NULL COMMENT '更新日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品广告位表';

DROP TABLE IF EXISTS `asin_ad_sync`;
CREATE TABLE `asin_ad_sync` (
    `asin` varchar(255) DEFAULT NULL COMMENT '商品asin编码',
    `ad_asin_position` varchar(255) DEFAULT NULL COMMENT '广告位置',
    `ad_asin` varchar(255) DEFAULT NULL COMMENT '广告商品asin编码',
    `ad_asin_bsr` varchar(255) DEFAULT NULL,
    `ad_asin_url` varchar(2048) DEFAULT NULL COMMENT '广告商品URL',
    `ad_asin_title` varchar(255) DEFAULT NULL COMMENT '广告商品名称',
    `ad_asin_price` varchar(255) DEFAULT NULL COMMENT '广告商品价格',
    `ad_asin_img` varchar(2048) DEFAULT NULL COMMENT '广告商品图片',
    `ad_asin_score` varchar(255) DEFAULT NULL,
    `ad_asin_starnum` varchar(255) DEFAULT NULL,
    `ad_asin_soldby` varchar(255) DEFAULT NULL COMMENT '广告商品卖家',
    `ad_asin_soldby_url` varchar(2048) DEFAULT NULL COMMENT '广告商品卖家url',
    `ad_asin_shipby` varchar(255) DEFAULT NULL COMMENT '广告商品物流',
    `ad_type` varchar(255) DEFAULT NULL COMMENT '广告类型: Sponsored/similar/also viewed／',
    `url` varchar(2048) DEFAULT NULL COMMENT '商品asin编码',
    `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;
