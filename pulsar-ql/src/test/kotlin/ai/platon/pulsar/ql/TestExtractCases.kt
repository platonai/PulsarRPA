package ai.platon.pulsar.ql

import ai.platon.pulsar.persist.model.WebPageFormatter
import kotlin.test.Test

class TestExtractCases : TestBase() {
    private val newsIndexUrl = TestResource.newsIndexUrl
    private val newsDetailUrl = TestResource.newsDetailUrl
    private val urlGroups = TestResource.urlGroups

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
        val session = context.createSession()
        session.load("$productIndexUrl -i 1d")
        println(WebPageFormatter(session.get(productIndexUrl)))
    }
}
