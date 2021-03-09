-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- asin  跟卖列表
-- 每天的跟卖列表不同
CREATE TABLE `asin_follow` (
   `asin` VARCHAR(255) NULL COMMENT '商品asin编码',
   `soldby` VARCHAR(255) NULL COMMENT '商品卖家',
   `sellerID` VARCHAR(255) NULL COMMENT '商品卖家ID', -- new add
   `seller_url` VARCHAR(255) NULL COMMENT '商品卖家url', -- new add
   `shipby` VARCHAR(255) NULL COMMENT '商品物流',
   `price` FLOAT DEFAULT 0.0 COMMENT '商品价格',
   `score` FLOAT DEFAULT 0.0 COMMENT '店铺好评率',
   `reviews` INT DEFAULT 0 COMMENT 'feedback数量',
   `is_recently` tinyint(4) DEFAULT 0 COMMENT '是否最新更新的数据',  -- new add
   `is_out` tinyint(4) DEFAULT 0 COMMENT '是否撤出',  -- new add
   `is_log` tinyint(4) DEFAULT 0 COMMENT '是否做过变化日志',  -- new add
   `createtime` VARCHAR(255)  NOT NULL COMMENT '更新日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品跟卖位表';
