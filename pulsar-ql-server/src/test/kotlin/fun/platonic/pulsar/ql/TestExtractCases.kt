package `fun`.platonic.pulsar.ql

import `fun`.platonic.pulsar.common.PulsarContext
import `fun`.platonic.pulsar.common.config.PulsarConstants.URL_TRACKER_HOME_URL
import `fun`.platonic.pulsar.crawl.fetch.TaskStatusTracker.LAZY_FETCH_URLS_PAGE_BASE
import `fun`.platonic.pulsar.persist.WebPageFormatter
import `fun`.platonic.pulsar.persist.metadata.FetchMode
import `fun`.platonic.pulsar.ql.utils.ResultSetFormatter
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

class TestExtractCases : TestBase() {

    @Test
    fun testSavePages() {
        execute("SELECT ADMIN_SAVE('$productIndexUrl', 'product.index.html')")
        execute("SELECT ADMIN_SAVE('$productDetailUrl', 'product.detail.html')")
        execute("SELECT ADMIN_SAVE('$newsIndexUrl', 'news.index.html')")
        execute("SELECT ADMIN_SAVE('$newsDetailUrl', 'news.detail.html')")
    }

    /**
     * Extract text using a rectangle box
     * */
    @Test
    fun testExtractByRegex() {
        val sql = "SELECT DOM_FIRST_RE1(DOM_LOAD('$productDetailUrl'), '*:in-box(530, 32)', '~\\p{Sc}(\\d+(\\.\\d{0,2})?)')"
        val rs = executeQuery(sql)

        // assertEquals("1599.00", rs.getString(1))
    }

    @Test
    fun testExtractOutLinks() {
        val url = urlGroups["baidu"]!![0]
        val sql = """
SELECT DISTINCT
    DOM_ABSHREF(DOM) AS href,
    LENGTH(DOM_TEXT(DOM)) AS anchorLength,
    DOM_TEXT(DOM) AS anchorText
FROM
    DOM_LOAD_AND_SELECT('$url', '.result a')
WHERE
    REGEXP_LIKE(DOM_TEXT(DOM), '^[1-9]{1,2}$')
"""
        execute(sql)
    }

    @Test
    fun testExtractOutLinks2() {
        val sql = """
SELECT
  DOM_ABSHREF(DOM_SELECTNTH(DOM, 'a', 2)) AS Href
FROM
  DOM_LOAD_AND_GET_FEATURES('${productIndexUrl}', 'DIV', 1, 10000)
WHERE
  WIDTH BETWEEN 210 AND 230
  AND
  HEIGHT BETWEEN 400 AND 420
LIMIT 100
"""
        execute(sql)
    }

    @Test
    fun testGroupFetchWithTempTable() {
        val url = productIndexUrl
        stat.execute("CREATE MEMORY TEMP TABLE IF NOT EXISTS MOGUJIE_ITEM_LINKS(HREF VARCHAR UNIQUE)")
        // stat.execute("CALL SET_PAGE_EXPIRES('1s', 2)")
        // stat.execute("call setScrollDownCount(1, 1000)")
        val sql = """
INSERT INTO MOGUJIE_ITEM_LINKS
SELECT
  DOM_ABSHREF(DOM_SELECTNTH(DOM, 'a', 2)) AS Href
FROM
  DOM_LOAD_AND_GET_FEATURES('${url}', 'DIV', 1, 10000)
WHERE
  WIDTH BETWEEN 210 AND 230
  AND
  height BETWEEN 400 AND 420
LIMIT 100
"""
        stat.execute(sql)

        var rs = stat.executeQuery("SELECT GROUP_FETCH(HREF) FROM MOGUJIE_ITEM_LINKS")
        println(ResultSetFormatter(rs))

        // stat.execute("CALL SET_PAGE_EXPIRES('30d', 1)")
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

    @Test
    fun testLoadAll() {
        val expr = "div:expr(WIDTH>=210 && WIDTH<=230 &&      HEIGHT>=400 && HEIGHT<=420  && SIBLING>30 )"
        val sql = """
SELECT
  DOM_FIRST_TEXT(DOM, 'h1:expr(_char>     10 && _img ==    0)') AS Title,
  DOM_FIRST_TEXT(DOM, '.price') AS Price,
  DOM_FIRST_TEXT(DOM, '#J_ParameterTable') AS Parameters
FROM LOAD_ALL(DOM_ALL_HREFS(DOM_LOAD('$productIndexUrl'), '$expr'))
"""
        execute(sql)
    }

    @Test
    fun testLoadAndGetFeatures() {
        execute("SELECT * FROM LOAD_AND_GET_FEATURES('$productIndexUrl --expires=1s') LIMIT 20")
    }

    @Test
    fun testAccumulateVividLinks() {
        val expr = "div:expr(WIDTH>=210 && WIDTH<=230 && HEIGHT>=400 && HEIGHT<=420 && SIBLING>30 ) a[href~=detail]"
        val rs = stat.executeQuery("SELECT * FROM DOM_LOAD_AND_GET_OUT_LINKS('$productIndexUrl --expires=1s', '$expr')")
        println(ResultSetFormatter(rs))
        val session = PulsarContext.createSession()
        println(WebPageFormatter(session.getOrNil(productIndexUrl)))
    }

    @Test
    @Ignore
    fun testBackgroundLoading() {
        val session = PulsarContext.createSession()

        val page = session.getOrNil(productIndexUrl)
        page.vividLinks.keys.map { it.toString() }
                .map { session.load(it) }
                .filter { !it.protocolStatus.isSuccess }
                .map { session.delete(it.url) }
        session.flush()

        System.out.println("Loading " + page.vividLinks.size + " out links")
        session.loadAll(page.vividLinks.keys.map { it -> it.toString() })

        var page2 = session.getOrNil(URL_TRACKER_HOME_URL)
        println(WebPageFormatter(page2))
        for (i in 1..60) {
            page2 = session.getOrNil(URL_TRACKER_HOME_URL + "/" + (LAZY_FETCH_URLS_PAGE_BASE + FetchMode.SELENIUM.ordinal))
            println(WebPageFormatter(page2).toMap()["linksMessage"])

            TimeUnit.SECONDS.sleep(30)
        }
    }

    @Test
    fun testLoadOutPagesForMia() {
        val url = urlGroups["mia"]!![0]
        val limit = 100
        execute("SELECT * FROM DOM_LOAD_AND_GET_FEATURES('$url --expires=1s') WHERE SIBLING > 30 LIMIT $limit")

//        execute("CALL SET_PAGE_EXPIRES('1s', 10)")
//        execute("CALL SET_FETCH_MODE('SELENIUM', 10)")
//        execute("CALL SET_BROWSER('CHROME', 10)")
        val expr = "*:expr(width>=250 && width<=260 && height>=360 && height<=370 && sibling>30 ) a"
        val sql = """
SELECT
  DOM_BASE_URI(DOM) AS BaseUri,
  DOM_FIRST_TEXT(DOM, '.brand') AS Title,
  DOM_FIRST_TEXT(DOM, '.pbox_price') AS Price,
  DOMWIDTH(DOM_SELECT_FIRST(DOM, '.pbox_price')) AS WIDTH,
  DOMHEIGHT(DOM_SELECT_FIRST(DOM, '.pbox_price')) AS HEIGHT,
  DOM_FIRST_TEXT(DOM, '#wrap_con') AS Parameters
FROM DOM_LOAD_OUT_PAGES_IGNORE_URL_QUERY('$url', '$expr', 1, $limit)
"""
        execute(sql)

        val sql2 = """
SELECT
  DOM_BASE_URI(DOM) AS URI,
  DOM_FIRST_TEXT(DOM, '.brand') AS TITLE,
  DOM_OUTER_HTML(DOM_SELECT_FIRST(DOM, '#ScrapingMetaInformation')) AS META_INFO
FROM DOM_LOAD_OUT_PAGES_IGNORE_URL_QUERY('$url', '$expr', 1, $limit)
"""
        execute(sql2)
    }

    @Test
    fun testLoadOutPagesForMogujie() {
        val url = urlGroups["mogujie"]!![0]
        // val url = urlGroups["meilishuo"]!![0]
        execute("SELECT * FROM DOM_LOAD_AND_GET_FEATURES('$url --expires=1s') WHERE SIBLING > 30")

        // stat.execute("CALL SET_PAGE_EXPIRES('1d', 1)")
        val expr = "*:expr(width>=210 && width<=230 && height>=380 && height<=420 && sibling>30 ) a[href~=detail]"
        val sql = """
SELECT
  DOM_BASE_URI(DOM) AS Uri,
  DOM_FIRST_TEXT(DOM, 'h1:expr(_char>10 && _img == 0)') AS Title,
  DOM_FIRST_TEXT(DOM, '.price') AS Price,
  DOM_FIRST_TEXT(DOM, '#J_ParameterTable') AS Parameters
FROM DOM_LOAD_OUT_PAGES_IGNORE_URL_QUERY('$url', '$expr', 1, 1000)
"""
        execute(sql)
    }

    @Test
    fun testLoadOutPagesForMeilishuo() {
        val url = urlGroups["meilishuo"]!![0]
        execute("SELECT * FROM DOM_LOAD_AND_GET_FEATURES('$url --expires=1s') WHERE SIBLING > 100")

        // stat.execute("CALL SET_PAGE_EXPIRES('1d', 1)")
        val expr = "*:expr(width>=200 && width<=250 && height>=350 && height<=400 && sibling>100 ) a[href~=detail]"
        val sql = """
SELECT
  DOM_BASE_URI(DOM) AS BaseUri,
  DOM_FIRST_TEXT(DOM, 'h1:expr(char>10 && img == 0)') AS Title,
  DOM_FIRST_TEXT(DOM, '.price') AS Price,
  DOM_FIRST_TEXT(DOM, '#J_ParameterTable') AS Parameters
FROM DOM_LOAD_OUT_PAGES_IGNORE_URL_QUERY('$url', '$expr', 1, 1000)
"""
        execute(sql)
    }

    @Test
    fun testLoadOutPagesForVipCom() {
        val url = urlGroups["vip"]!![0]
        execute("CALL SET_SCROLL_DOWN_COUNT(3, 1)")
        execute("SELECT * FROM DOM_LOAD_AND_GET_FEATURES('$url') WHERE SIBLING > 20")

        // stat.execute("CALL SET_PAGE_EXPIRES('1d', 1)")
//        val expr = "*:expr(WIDTH>=200 && WIDTH<=250 && HEIGHT>=350 && HEIGHT<=400 && _img>0 ) a[href~=detail]"
        val expr = ".goods-list-item a[href~=detail]"
        val sql = """
SELECT
  DOM_BASE_URI(DOM) AS Uri,
  DOM_FIRST_TEXT(DOM, '.pro-title-main') AS Title,
  DOM_FIRST_TEXT(DOM, '.price-sell') AS Price,
  DOM_FIRST_TEXT(DOM, '.g-pro-param') AS Parameters
FROM DOM_LOAD_OUT_PAGES_IGNORE_URL_QUERY('$url', '$expr', 1, 1000)
"""
        execute(sql)
    }

    @Test
    fun testLoadOutPagesForJd() {
        val url = urlGroups["jd"]!![0]
        execute("CALL SET_SCROLL_DOWN_COUNT(3, 1)")
        execute("SELECT * FROM DOM_LOAD_AND_GET_FEATURES('$url') WHERE SIBLING > 20")

        // stat.execute("CALL SET_PAGE_EXPIRES('1d', 1)")
        val restrictCss = "*:expr(IMG>0 && WIDTH>200 && HEIGHT>200 && SIBLING>30)"
//        val restrictCss = "*:expr(WIDTH>=200 && WIDTH<=250 && HEIGHT>=350 && HEIGHT<=500 && _img>0 ) a[href~=item]"
//        val restrictCss = "li.gl-item a[href~=item]"
        val sql = """
SELECT
  DOM_FIRST_TEXT(DOM, '.sku-name') AS NAME,
  DOM_FIRST_PRICE(DOM, '.summary-price') AS PRICE,
  DOM_BASE_URI(DOM) AS URI,
  DOM_FIRST_IMG(DOM, '.main-img') AS MAIN_IMAGE,
  DOM_FIRST_IMG(DOM, '460x460') AS MAIN_IMAGE2,
  DOM_KVS(DOM, '.parameter2') AS PARAMETERS,
  DOM_FIRST_TEXT(DOM, '.comment-item') AS COMMENT1
FROM DOM_LOAD_OUT_PAGES('$url', '$restrictCss', 1, 20)
WHERE LOCATE('item', DOM_BASE_URI(DOM)) > 0;
"""
        execute(sql)
    }
}
