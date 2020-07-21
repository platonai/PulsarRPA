package ai.platon.pulsar.ql

import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.common.config.AppConstants.URL_TRACKER_HOME_URL
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.support.BasicPulsarContext
import ai.platon.pulsar.crawl.fetch.LazyFetchTaskManager.Companion.LAZY_FETCH_URLS_PAGE_BASE
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.model.WebPageFormatter
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

class TestExtractCases : TestBase() {

    private val pc = PulsarContexts.active(BasicPulsarContext())

    @Test
    fun testSavePages() {
        execute("CALL ADMIN_SAVE('$productIndexUrl', 'product.index.html')")
        execute("CALL ADMIN_SAVE('$productDetailUrl', 'product.detail.html')")
        execute("CALL ADMIN_SAVE('$newsIndexUrl', 'news.index.html')")
        execute("CALL ADMIN_SAVE('$newsDetailUrl', 'news.detail.html')")
    }

    @Test
    fun testLoadAndGetFeatures() {
        execute("SELECT * FROM LOAD_AND_GET_FEATURES('$productIndexUrl -expires 1d') LIMIT 20")
    }

    @Test
    fun testLoadAndGetLinks() {
        // val expr = "div:expr(WIDTH>=210 && WIDTH<=230 && HEIGHT>=400 && HEIGHT<=420 && SIBLING>30 ) a[href~=item]"
        val expr = "a[href~=item]"
        execute("SELECT * FROM LOAD_AND_GET_LINKS('$productIndexUrl -expires 1s', '$expr')")
    }

    @Test
    fun testLoadAndGetAnchors() {
        // val expr = "div:expr(WIDTH>=210 && WIDTH<=230 && HEIGHT>=400 && HEIGHT<=420 && SIBLING>30 ) a[href~=item]"
        val expr = "a[href~=item]"
        execute("SELECT * FROM LOAD_AND_GET_ANCHORS('$productIndexUrl -expires 1d', '$expr')")
    }

    @Test
    fun testAccumulateVividLinks() {
        val session = pc.createSession()
        session.load("$productIndexUrl -i 1d")
        println(WebPageFormatter(session.getOrNil(productIndexUrl)))
    }

    @Test
    @Ignore
    fun testBackgroundLoading() {
        val session = pc.createSession()

        val page = session.getOrNil(productIndexUrl)
        page.vividLinks.keys.map { it.toString() }
                .map { session.load(it) }
                .filter { !it.protocolStatus.isSuccess }
                .map { session.delete(it.url) }
        session.flush()

        println("Loading " + page.vividLinks.size + " out links")
        val optins = LoadOptions.create()
        session.loadAll(page.vividLinks.keys.map { it -> it.toString() }, optins)

        var page2 = session.getOrNil(URL_TRACKER_HOME_URL)
        println(WebPageFormatter(page2))
        for (i in 1..60) {
            page2 = session.getOrNil(URL_TRACKER_HOME_URL + "/" + (LAZY_FETCH_URLS_PAGE_BASE + FetchMode.BROWSER.ordinal))
            println(WebPageFormatter(page2).toMap()["linksMessage"])

            TimeUnit.SECONDS.sleep(30)
        }
    }

    @Test
    fun testLoadOutPagesForMia() {
        val url = urlGroups["mia"]!![0]
        val limit = 20
        execute("SELECT * FROM LOAD_AND_GET_FEATURES('$url --expires=1d') WHERE SIBLING > 30 LIMIT $limit")

        execute("CALL SET_PAGE_EXPIRES('1s', 10)")
        // val expr = "*:expr(width>=250 && width<=260 && height>=360 && height<=370 && sibling>30 ) a"
        val expr = "a[href~=item]"
        val sql = """
SELECT
  DOM_BASE_URI(DOM) AS BaseUri,
  DOM_FIRST_TEXT(DOM, '.brand') AS Title,
  DOM_FIRST_TEXT(DOM, '.pbox_price') AS Price,
  DOM_WIDTH(DOM_SELECT_FIRST(DOM, '.pbox_price')) AS WIDTH,
  DOM_HEIGHT(DOM_SELECT_FIRST(DOM, '.pbox_price')) AS HEIGHT,
  DOM_FIRST_TEXT(DOM, '#wrap_con') AS Parameters
FROM LOAD_OUT_PAGES('$url -i 1s', '$expr', 1, $limit)
"""
        execute(sql)
    }

    @Test
    fun testExtractSinglePageForTmall() {
        val sql = """
select
    dom_first_text(dom, 'h1') as title,
    dom_first_text(dom, '#J_PromoPrice .tm-price') as price,
    dom_first_text(dom, '#J_StrPriceModBox .tm-price') as tag_price,
    dom_first_text(dom, '.tm-price') as tag_price1,
    dom_first_text(dom, '.tm-ind-sellCount .tm-count') as sell_count,
    dom_first_text(dom, '.tm-ind-reviewCount .tm-count') as review_count,
    dom_base_uri(dom)
from
    dom_load_and_select('https://detail.tmall.com/item.htm?id=577089875457 -i 1d -sc 20', ':root');
"""
        execute(sql)
    }

    @Test
    fun testLoadOutPagesForMogujie() {
        val url = urlGroups["mogujie"]!![0]
        // val url = urlGroups["meilishuo"]!![0]
        execute("SELECT * FROM LOAD_AND_GET_FEATURES('$url --expires=1s') WHERE SIBLING > 30")

        // stat.execute("CALL SET_PAGE_EXPIRES('1d', 1)")
        // val expr = "*:expr(width>=210 && width<=230 && height>=380 && height<=420 && sibling>30 ) a[href~=detail]"
        val expr = "a[href~=detail]"
        val sql = """
SELECT
  DOM_BASE_URI(DOM) AS Uri,
  DOM_FIRST_TEXT(DOM, 'h1:expr(_char>10 && _img == 0)') AS Title,
  DOM_FIRST_TEXT(DOM, '.price') AS Price,
  DOM_FIRST_TEXT(DOM, '#J_ParameterTable') AS Parameters
FROM LOAD_OUT_PAGES_IGNORE_URL_QUERY('$url', '$expr', 1, 1000)
"""
        execute(sql)
    }

    @Test
    fun testLoadOutPagesForVipCom() {
        val url = urlGroups["vip"]!![0]
        execute("CALL SET_SCROLL_DOWN_COUNT(3, 1)")
        execute("SELECT * FROM LOAD_AND_GET_FEATURES('$url') WHERE SIBLING > 20")

        // stat.execute("CALL SET_PAGE_EXPIRES('1d', 1)")
//        val expr = "*:expr(WIDTH>=200 && WIDTH<=250 && HEIGHT>=350 && HEIGHT<=400 && _img>0 ) a[href~=detail]"
        val expr = ".goods-list-item a[href~=detail]"
        val sql = """
SELECT
  DOM_BASE_URI(DOM) AS Uri,
  DOM_FIRST_TEXT(DOM, '.pro-title-main') AS Title,
  DOM_FIRST_TEXT(DOM, '.price-sell') AS Price,
  DOM_FIRST_TEXT(DOM, '.g-pro-param') AS Parameters
FROM LOAD_OUT_PAGES_IGNORE_URL_QUERY('$url', '$expr', 1, 1000)
"""
        execute(sql)
    }

    @Test
    fun testLoadOutPagesForJd() {
        val url = urlGroups["jd"]!![0]
        execute("CALL SET_SCROLL_DOWN_COUNT(3, 1)")
        // execute("SELECT * FROM LOAD_AND_GET_FEATURES('$url') WHERE SIBLING > 20")

        // stat.execute("CALL SET_PAGE_EXPIRES('1d', 1)")
//        val restrictCss = "*:expr(IMG>0 && WIDTH>200 && HEIGHT>200 && SIBLING>30)"
//        val restrictCss = "*:expr(WIDTH>=200 && WIDTH<=250 && HEIGHT>=350 && HEIGHT<=500 && _img>0 ) a[href~=item]"
        val restrictCss = "a[href~=item]"
        val sql = """
SELECT
  DOM_FIRST_TEXT(DOM, '.sku-name') AS NAME,
  DOM_FIRST_TEXT(DOM, '.summary-price') AS PRICE,
  DOM_BASE_URI(DOM) AS URI,
  DOM_FIRST_IMG(DOM, '.main-img') AS MAIN_IMAGE,
  DOM_FIRST_IMG(DOM, '460x460') AS MAIN_IMAGE2,
  DOM_FIRST_TEXT(DOM, '.parameter2') AS PARAMETERS,
  DOM_FIRST_TEXT(DOM, '.comment-item') AS COMMENT1
FROM LOAD_OUT_PAGES('$url', '$restrictCss', 1, 20)
WHERE LOCATE('item', DOM_BASE_URI(DOM)) > 0;
"""
        execute(sql)
    }

    @Test
    fun testLoadOutPagesForJd2() {
        val sql = """
SELECT
    DOM_FIRST_TEXT(DOM, '.sku-name') AS Name,
    DOM_FIRST_NUMBER(DOM, '.p-price .price', 0.00) AS Price,
    DOM_FIRST_NUMBER(DOM, '#page_opprice', 0.00) AS Tag_Price,
    DOM_FIRST_TEXT(DOM, '#comment-count .count') AS Comments,
    DOM_FIRST_TEXT(DOM, '#summary-service') AS Logistics,
    DOM_BASE_URI(DOM) AS BaseUri
FROM LOAD_OUT_PAGES('https://list.jd.com/list.html?cat=652,12345,12349 -i 1s -ii 100d', 'a[href~=item]', 1, 100)
WHERE DOM_FIRST_NUMBER(DOM, '.p-price .price', 0.00) > 0
ORDER BY DOM_FIRST_NUMBER(DOM, '.p-price .price', 0.00);
""".trimIndent()

        execute(sql)
    }

    @Test
    @Ignore("Takes too much time to finish")
    fun testLoadAll() {
        val expr = "div:expr(WIDTH>=200 && WIDTH<=300 &&      HEIGHT>=200 && HEIGHT<=400  && SIBLING>30 )"
        val sql = """
SELECT
  DOM_FIRST_TEXT(DOM, 'h1:expr(char>     10 && img ==    0)') AS Title,
  DOM_FIRST_TEXT(DOM, '.price') AS Price,
  DOM_FIRST_TEXT(DOM, '#J_ParameterTable') AS Parameters
FROM LOAD_ALL(DOM_ALL_HREFS(DOM_LOAD('$productIndexUrl'), '$expr'))
"""
        execute(sql)
    }

    @Test
    @Ignore("Ignore via org.h2.jdbc.JdbcSQLException: General error: \"com.vividsolutions.jts.io.ParseException: Unknown geometry type: 0 (line 1)\";")
    fun testGroupFetchWithTempTable() {
        val url = productIndexUrl
        execute("CREATE MEMORY TEMP TABLE IF NOT EXISTS MOGUJIE_ITEM_LINKS(HREF VARCHAR)")
        // stat.execute("CALL SET_PAGE_EXPIRES('1s', 2)")
        // stat.execute("call setScrollDownCount(1, 1000)")
        val sql = """
INSERT INTO MOGUJIE_ITEM_LINKS
    SELECT
        DOM_ABSHREF(DOM_SELECTNTH(DOM, 'a', 2)) AS HREF
    FROM
        LOAD_AND_GET_FEATURES('$url', 'DIV', 1, 10000)
    WHERE
        IMG > 1 && SIBLING > 30
    LIMIT 100
"""
        execute(sql)

        execute("SELECT GROUP_FETCH(HREF) FROM MOGUJIE_ITEM_LINKS")

        // execute("CALL SET_PAGE_EXPIRES('30d', 1)")
        val sql2 = """
SELECT
  DOM_FIRST_TEXT(DOM, 'h1:expr(_char>10 && _img == 0)') AS Title,
  DOM_FIRST_TEXT(DOM, '.price') AS Price,
  DOM_FIRST_TEXT(DOM, '#J_ParameterTable') AS Parameters
FROM(SELECT DOM_PARSE(HREF) AS DOM FROM MOGUJIE_ITEM_LINKS)
WHERE DOM_IS_NOT_NIL(DOM)
"""
        execute(sql2)
    }
}
