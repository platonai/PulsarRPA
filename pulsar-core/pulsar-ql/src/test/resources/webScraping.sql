---------------
-- baidu
---------------
CALL SETFETCHMODE('native', 1);
SELECT DOM_TITLE(DetailPage.page) AS Title, DOM_BASEURI(DetailPage.page) AS Url, DOM_TEXT(DetailPage.page) As Text
FROM (
       SELECT DOM_FETCH(DetailPageLink.href) AS page
       FROM (
              SELECT DISTINCT DOM_ABSHREF(AnchorInIndexPage.link) AS href
              FROM (
                     SELECT explode(DOM_LINKS(IndexPage.page)) AS link
                     FROM (
                            SELECT DOM_FETCH(Pagination.href) AS page
                            FROM (
                                   SELECT DISTINCT DOM_ABSHREF(PortalPage.DOM) AS href
                                   FROM DOM_links(DOM_FETCH('https://www.baidu.com/s?tn=baidurt&rtt=4&wd=马航&bsst=1&ie=utf-8'))
                                     AS PortalPage
                                   WHERE isNumeric(DOM_TEXT(PortalPage.DOM)) LIMIT 2
                                 ) AS Pagination
                          ) AS IndexPage
                   ) AS AnchorInIndexPage
              WHERE DOM_TEXTLEN(AnchorInIndexPage.link) BETWEEN 20 AND 200 LIMIT 25
            ) AS DetailPageLink
     ) AS DetailPage;

---------------
-- baidu
---------------
CREATE MEMORY TEMPORARY TABLE IF NOT EXISTS NEWS_CHUNWAN(Id IDENTITY, Title varchar, Url varchar, Text varchar, constraint C_Url unique(Url));
CALL SETFETCHMODE('NATIVE', 1);
INSERT INTO NEWS_CHUNWAN (Title, Url, Text)
  SELECT DOM_TITLE(DetailPage.page) AS Title, DOM_BASEURI(DetailPage.page) AS Url, DOM_TEXT(DetailPage.page) As Text
  FROM (
         SELECT DOM_FETCH(DetailPageLink.href) AS page
         FROM (
                SELECT DISTINCT DOM_ABSHREF(AnchorInIndexPage.link) AS href
                FROM (
                       SELECT EXPLODE(DOM_LINKS(IndexPage.page)) AS link
                       FROM (
                              SELECT DOM_FETCH(Pagination.href) AS page
                              FROM (
                                     SELECT DISTINCT DOM_ABSHREF(PortalPage.DOM) AS href
                                     FROM DOM_LINKS(DOM_FETCH('https://www.baidu.com/s?tn=baidurt&rtt=4&wd=春晚&bsst=1&ie=utf-8'))
                                       AS PortalPage
                                     WHERE ISNUMERIC(DOM_TEXT(PortalPage.DOM)) LIMIT 2
                                   ) AS Pagination
                            ) AS IndexPage
                     ) AS AnchorInIndexPage
                WHERE DOM_TEXTLEN(AnchorInIndexPage.link) BETWEEN 20 AND 200 LIMIT 25
              ) AS DetailPageLink
       ) AS DetailPage;

SELECT * FROM NEWS_CHUNWAN;

---------------
-- baidu
-- 马航最新消息（SQL 视图风格）
---------------
CREATE VIEW IF NOT EXISTS NEWS_MAHANG AS
  SELECT DOM_TITLE(DetailPage.page) AS Title, DOM_BASEURI(DetailPage.page) AS Url, DOM_TEXT(DetailPage.page) As Text
  FROM (
         SELECT DOM_FETCH(DetailPageLink.href) AS page
         FROM (
                SELECT DISTINCT DOM_ABSHREF(AnchorInIndexPage.link) AS href
                FROM (
                       SELECT EXPLODE(DOM_LINKS(IndexPage.page)) AS link
                       FROM (
                              SELECT DOM_FETCH(Pagination.href) AS page
                              FROM (
                                     SELECT DISTINCT DOM_ABSHREF(PortalPage.DOM) AS href
                                     FROM DOM_LINKS(DOM_FETCH('https://www.baidu.com/s?tn=baidurt&rtt=4&wd=马航&bsst=1&ie=utf-8'))
                                       AS PortalPage
                                     WHERE ISNUMERIC(DOM_TEXT(PortalPage.DOM)) LIMIT 2
                                   ) AS Pagination
                            ) AS IndexPage
                     ) AS AnchorInIndexPage
                WHERE DOM_TEXTLEN(AnchorInIndexPage.link) BETWEEN 20 AND 200 LIMIT 25
              ) AS DetailPageLink
       ) AS DetailPage;

CALL SETFETCHMODE('NATIVE', 1);
SELECT * FROM NEWS_MAHANG;

---------------
-- Tmail
---------------
CALL SETBROWSER('chrome', 1);
SELECT
  `_seq`, `_char`, `_txt_blk`, `_a`, `_img`, `_top`, `_left`, `_width`, `_height`,
  DOM_CSSSELECTOR(DOM) AS CssSelector,
  DOM_TEXT(DOM) AS Text
FROM
  DOM_features('https://detail.tmall.com/item.htm?id=536690467392', 'DIV', 1, 500)
WHERE
  (`_char` > 0 or `_img` > 0) and `_char` < 1000 and `_top` > 100 and `_height` > 25;

---------------
-- Try to get all index pages
-- No items, the pagination is js actions
---------------
CALL SETBROWSER('chrome', 1);
SELECT DISTINCT DOM_TEXT(PortalPage.DOM), DOM_ABSHREF(PortalPage.DOM) AS href
FROM DOM_LINKS(DOM_FETCH('http://search.jd.com/Search?keyword=长城葡萄酒&enc=utf-8&wq=长城葡萄酒'))
  AS PortalPage
WHERE ISNUMERIC(DOM_TEXT(PortalPage.DOM)) LIMIT 2;

---------------
-- Jd
-- Get all goods in the first page
---------------
CALL SETBROWSER('chrome', 1);
SELECT
  `_seq`, `_char`, `_txt-blk`, `_a`, `_img`, `_top`, `_left`, `_width`, `_height`,
  DOM_absHref(DOM_selectFirst(DOM, 'a')) AS Href,
  DOM_cssSelector(DOM) AS CssSelector,
  DOM_text(DOM) AS Text
FROM
  DOM_features('http://search.jd.com/Search?keyword=长城葡萄酒&enc=utf-8&wq=长城葡萄酒', 'DIV', 1, 10000)
WHERE
  (`_char` > 0 or `_img` > 0)
  and `_char` < 1000 and `_top` > 100
  and `_width` > 215 and `_width` < 225
  and `_height` > 420 and `_height` < 430;

---------------
-- Jd
-- Extract detail page links from the index page, and fetch each detail page
---------------
CALL SETBROWSER('chrome', 1);
SELECT DOM_FETCH(DetailPageLink.href) AS page
FROM (
       SELECT
         DOM_absHref(DOM_selectFirst(DOM, 'a')) AS Href
       FROM
         DOM_features('http://search.jd.com/Search?keyword=长城葡萄酒&enc=utf-8&wq=长城葡萄酒', 'DIV', 1, 10000)
       WHERE
         (`_char` > 0 or `_img` > 0)
         and `_char` < 1000 and `_top` > 100
         and `_width` > 215 and `_width` < 225
         and `_height` > 420 and `_height` < 430
     ) AS DetailPageLink;

---------------
-- Mogujie
-- Fetch index page, find out detail page links, parallel fetch all detail pages and than extract all
---------------
CREATE MEMORY TEMP TABLE IF NOT EXISTS MOGUJIE_ITEM_LINKS(HREF VARCHAR);

CALL SETBROWSER('chrome', 1000);
CALL SETFETCHMODE('selenium', 3);
INSERT INTO MOGUJIE_ITEM_LINKS
  SELECT
    DOM_ABSHREF(DOM_SELECTNTH(DOM, 'a', 2)) AS Href
  FROM
    DOM_FEATURES('http://list.mogujie.com/book/jiadian/10059513', 'DIV', 1, 10000)
  WHERE
    `_width` BETWEEN 210 AND 230
    AND
    `_height` BETWEEN 400 AND 420
  LIMIT 100;

SELECT GROUP_FETCH(HREF) FROM MOGUJIE_ITEM_LINKS;

SELECT
  DOM_SELECTFIRSTTEXT(DOM, 'H1') AS Title,
  DOM_SELECTFIRSTTEXT(DOM, '.price') AS Price,
  DOM_SELECTFIRSTTEXT(DOM, '#J_ParameterTable') AS Parameters
FROM(SELECT DOM_PARSE(HREF) AS DOM FROM MOGUJIE_ITEM_LINKS);

---------------
-- Miya
--
---------------
-- 使用浏览器模式
CALL SETBROWSER('chrome', 1);
-- 描述商品页特征
SELECT
  `_seq`, `_char`, `_txt-blk`, `_a`, `_img`, `_top`, `_left`, `_width`, `_height`,
  DOM_TEXT(DOM) AS `Text`
FROM
  DOM_FEATURES('https://www.mia.com/item-1501312.html', 'DIV', 1, 1000)
WHERE
  `_top` > 200 and `_height` < 1000;
