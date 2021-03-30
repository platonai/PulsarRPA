-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE `asin_review_sync` (
   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增ID',
   `asin` VARCHAR(255) NULL COMMENT '商品父pasin编码',
   `reviews_url` VARCHAR(2047) NULL COMMENT '评论链接',
   `sku_asin` VARCHAR(255) NULL COMMENT '变体sku组合对应的asin',
   `comment_id` VARCHAR(255) NULL COMMENT '评论ID',
   `comment_time` VARCHAR(255) NULL COMMENT '评论时间',
   `comment_name` VARCHAR(16) NULL COMMENT '评论人',
   `comment_title` VARCHAR(16) NULL COMMENT '评论标题',
   `comment_name_url` VARCHAR(16) NULL COMMENT '评论人URL',
   `content` TEXT NULL COMMENT '评论内容',
   `score` int NULL COMMENT '评星',
   `ispic` tinyint(4) NULL COMMENT '是否有图片',
   `helpfulnum` int NULL COMMENT '点赞数',
   `sku` VARCHAR(255) NULL COMMENT '评论的sku :  color  size   其他组合  Color: RoseGold | Size: 42mm/44mm ',
   `is_recently` tinyint(4) DEFAULT '0' COMMENT '是否最新更新的数据',
   `createtime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新日期',
   PRIMARY KEY (`id`)
) ENGINE=MYISAM DEFAULT CHARSET=utf8mb4 COMMENT='商品评论表';
