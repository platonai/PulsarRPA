-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE `seller_sync` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `sellerID` VARCHAR(255) NULL COMMENT 'sellerID',
  `seller_name` VARCHAR(255) NULL COMMENT '卖家名称',
  `seller_url` VARCHAR(2047) NULL COMMENT '卖家URL',
  `marketplaceID` VARCHAR(255) NULL COMMENT 'marketplaceID',
  `highstarpercent` VARCHAR(255) NULL COMMENT '好评占比',
  `middlestarpercent` VARCHAR(255) NULL COMMENT '中评占比',
  `badstarpercent` VARCHAR(255) NULL COMMENT '差评占比',
  `feedback_num_12` int DEFAULT 0 COMMENT '过去12月的评价数',
  `feedback_num` int DEFAULT 0 COMMENT '评价总数',
  `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新日期',
  PRIMARY KEY (`id`)
) ENGINE=MYISAM DEFAULT CHARSET=utf8mb4 COMMENT='卖家feedback表';
