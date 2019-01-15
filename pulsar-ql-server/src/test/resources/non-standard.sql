-- Web Harvest Target
SET @keyword='马航';
SET @seedUrl='https://www.baidu.com/s?tn=baidurt&rtt=4&wd='||@keyword||'&bsst=1&ie=utf-8';

-- Settings
SET @indexPageLimit=3;
SET @detailPageLimit=20;
SET @minAnchorLength=20;
SET @maxAnchorLength=100;

-- Find out pagination links
SELECT DISTINCT DOM_absHref(`dom`) AS href FROM DOM_links(DOM_load(@seedUrl))
WHERE isNumeric(DOM_text(`dom`)) LIMIT @indexPageLimit
=>
-- Load each index page
SELECT DOM_load(w.href) AS page FROM WHICH w
=>
-- Find out detail page links
SELECT explode(DOM_links(w.page)) AS link FROM WHICH w
=>
-- Extract detail page links
SELECT DISTINCT DOM_absHref(w.link) AS href FROM WHICH w
WHERE DOM_textLen(w.link) BETWEEN @minAnchorLength AND @maxAnchorLength LIMIT @detailPageLimit
=>
-- Load each detail page
SELECT BOIT_extract(DOM_load(w.href)) FROM WHICH w

-- SELECT DOM_baseUri(w.page), DOM_title(w.page), DOM_text(w.page) FROM WHICH w
;

call setFetchModeTTL('selenium', 1);
SELECT
  `_seq`, `_char`, `_txt_blk`, `_a`, `_top`, `_left`, `_width`, `_height`,
  DOM_cssSelector(DOM) AS CssSelector,
  DOM_text(DOM) AS Text
FROM
  DOM_features('https://detail.tmall.com/item.htm?id=536690467392', 'DIV', 1, 500)
WHERE
  `_char` > 50 and `_char` < 1000 and `_top` > 100 and `_height` > 25;
