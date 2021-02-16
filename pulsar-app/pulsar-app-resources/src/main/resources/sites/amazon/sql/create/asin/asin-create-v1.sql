# noinspection SqlNoDataSourceInspectionForFile

-- asin
CREATE TABLE `asin` (
  `id`  bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '自增ID',
  `category` varchar(255) DEFAULT NULL COMMENT '所属分类nodeID', -- 从 Breadcrumbs links 中找
  `col1` varchar(255) DEFAULT NULL COMMENT '预留字段',
  `col2` varchar(255) DEFAULT NULL,
  `img` varchar(255) DEFAULT NULL COMMENT '主图URL', -- Main image, TODO: 需要在浏览器中注入javascript采集
  `smallrank` INT NULL COMMENT '小类排名', --     dom_first_text(dom, '#prodDetails table tr th:contains(Best Sellers Rank) ~ td') as `Best sellers rank`,
  `bigrank` INT NULL COMMENT '大类排名', --     dom_first_text(dom, '#prodDetails table tr th:contains(Best Sellers Rank) ~ td') as `Best sellers rank`,
  `title` TEXT NULL COMMENT '商品标题', -- Title
  `desc` TEXT NULL COMMENT '商品描述', -- Feature bullets, Product Description 选一个
  `asin` VARCHAR(255) NULL COMMENT '商品asin编码', -- ASIN
  `brand` VARCHAR(255) NULL COMMENT '品牌', -- Brand, Brand link
  `soldby` VARCHAR(255) NULL COMMENT '卖家', -- Sold by, Sold by link
  `shipby` VARCHAR(255) NULL COMMENT '物流', -- TODO: 没找到，最接近的是 Ship from，同 Sold by 在同一个位置，需要在采集一批后进一步分析
  `price` VARCHAR(255) NULL COMMENT '价格', -- 看 List Price 和 Price 两个字段(挂牌价/实际价格)
  `sku_num` INT(10) NULL COMMENT '变体数量', -- 看 Variation number/Variation asins/Variation links 三个字段
  `iscoupon` tinyint(4) NULL COMMENT '是否在优惠券促销', -- TODO: 关键词 coupon #couponBadgeRegularVpc .couponFeature.regularVpc
  `isprime` tinyint(4) NULL COMMENT '是否amazon prime', -- TODO: 没找到 
  `instock`  tinyint NULL COMMENT '是否缺货 0 不缺货  1  缺货', -- 看 Availability
  `isaddcart` tinyint NULL COMMENT '加入购物车按钮', -- Add to cart
  `isbuy` tinyint NULL COMMENT '直接购买按钮', -- Buy now
  `isac` tinyint NULL COMMENT '是否amazon精选推荐', -- Label Amazon's choice
  `isbs` tinyint NULL COMMENT '释放amazon热卖推荐', -- Label Best seller
  `isa` tinyint NULL COMMENT '是否A+页面', -- 看 Big images
  `othersellernum` INT NULL COMMENT '跟卖数量', -- New & used sellers number 或者 New sellers number
  `qanum` INT NULL COMMENT 'QA问题数', -- Answered questions number
  `score` FLOAT NULL COMMENT '平均评星数',  -- Stars
  `reviews` INT NULL COMMENT '评论总数', -- TODO: 需要采集评论页
  `starnum` INT NULL COMMENT '评星总数', -- Customer reviews - number of ratings
  `score5percent` VARCHAR(255) NULL COMMENT '5星级占比', -- Customer reviews 5 star
  `score4percent` VARCHAR(255) NULL COMMENT '4星级占比', -- Customer reviews 4 star
  `score3percent` VARCHAR(255) NULL COMMENT '3星级占比', -- Customer reviews 3 star
  `score2percent` VARCHAR(255) NULL COMMENT '2星级占比', -- Customer reviews 2 star
  `score1percent` VARCHAR(255) NULL COMMENT '1星级占比', -- Customer reviews 1 star
  `weight` FLOAT NULL COMMENT '重量', -- Item weight, shipping Weight
  `volume` FLOAT NULL COMMENT '体积', -- Product dimensions
  `isad` tinyint NULL COMMENT '是否列表广告推广', -- TODO: 从列表页中提取
  `adposition` VARCHAR(255) NULL COMMENT '列表广告位置', -- TODO: 从列表页中提取
  `commenttime` VARCHAR(255) NULL COMMENT '第一条评论时间', -- TODO: 需要采集评论页
  `reviewsmention` TEXT NULL COMMENT '高频评论词', -- TODO: 需要解释
  `onsaletime` VARCHAR(255) NULL COMMENT '上架时间', -- Date on sale
  `feedbackurl` VARCHAR(255) NULL COMMENT '打开feedback页面的URL', -- Feedback links
  `sellerID` VARCHAR(255) NULL COMMENT 'sellerID', -- 包含在 Sold by link 中
  `marketplaceID` VARCHAR(255) NULL COMMENT 'marketplaceID', -- 包含在 Sold by link 中
  `reviewsurl` VARCHAR(255) NULL COMMENT '打开所有评论页面的URL', -- Customer reviews all reviews link
  `sellsameurl` VARCHAR(255) NULL COMMENT '打开跟卖信息页面的URL', -- Available sellers links, New & used sellers link, New sellers link, Renewed link, Newer version link
  `createtime` VARCHAR(255) NULL,
  `isvalid` tinyint(4) DEFAULT '1' COMMENT 'valid',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='商品表-美国';
