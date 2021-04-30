-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 热卖产品排行

-- 卖家 主营品牌表
CREATE TABLE `seller_brand` (
    `sellerID` VARCHAR(255) NULL COMMENT 'sellerID',
    `marketplaceID` VARCHAR(255) NULL COMMENT 'marketplaceID',
    `brand` VARCHAR(255) NULL COMMENT '品牌名称',
    `brand_url` VARCHAR(255) NULL COMMENT '品牌URL',
    `listing_num` int DEFAULT 0  COMMENT '卖家该品牌下listing数量',
    `is_recently` tinyint(4) DEFAULT '0' COMMENT '是否最新更新的数据',  -- new add
    `createtime` VARCHAR(255) NOT NULL COMMENT '更新日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='卖家-主营品类表';
