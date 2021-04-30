-- noinspection SqlResolveForFile
-- noinspection SqlNoDataSourceInspectionForFile

-- 热卖产品排行

-- 品类-关键词流量圈监控表
CREATE TABLE `keyword_flow` (
    `nodeID` VARCHAR(255) NULL COMMENT '品类ID', -- new add
    `original_keyword` VARCHAR(255) NULL COMMENT '原始关键词', -- new add
    `keyword` VARCHAR(255) NULL COMMENT '关键词',
    `other_keyword` VARCHAR(255) NULL COMMENT '衍生关键词',
    `level` int DEFAULT 0 COMMENT '层级',
    `parent_id` VARCHAR(255) NULL COMMENT '父级ID',
    `sort` int DEFAULT 0 COMMENT '层级下的排序',
    `is_recently` tinyint(4) DEFAULT '0' COMMENT '是否最新更新的数据',  -- new add
    `createtime` VARCHAR(255) NULL COMMENT '更新日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='关键词广告位';
