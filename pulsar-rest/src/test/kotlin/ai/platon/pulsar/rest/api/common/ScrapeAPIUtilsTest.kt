package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.rest.api.common.ScrapeAPIUtils.isScrapeUDF
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertNull

class ScrapeAPIUtilsTest {

    /**
     * 测试空字符串或空白字符串的情况
     */
    @Test
    fun testIsScrapeUDF_NullOrBlank() {
        assertFalse(isScrapeUDF(null)) // 输入为 null，预期返回 false
        assertFalse(isScrapeUDF("")) // 输入为空字符串，预期返回 false
        assertFalse(isScrapeUDF("   ")) // 输入为空白字符串，预期返回 false
    }

    /**
     * 测试包含允许的 Scrape UDF 的情况
     */
    @Test
    fun testIsScrapeUDF_ContainsAllowedUDF() {
        assertTrue(isScrapeUDF("SELECT dom from load_and_select(https://)")) // 输入包含允许的 Scrape UDF，预期返回 true
        assertTrue(isScrapeUDF("SELECT dom from loadandselect('http://t.tt')")) // 输入包含允许的 Scrape UDF（无下划线），预期返回 true
    }

    /**
     * 测试不包含允许的 Scrape UDF 的情况
     */
    @Test
    fun testIsScrapeUDF_DoesNotContainAllowedUDF() {
        assertFalse(isScrapeUDF("SELECT other_function()")) // 输入不包含允许的 Scrape UDF，预期返回 false
    }

    /**
     * 测试包含下划线的 Scrape UDF 的情况
     */
    @Test
    fun testIsScrapeUDF_ContainsUnderscore() {
        assertTrue(isScrapeUDF("SELECT from load_and_select(http)")) // 输入包含下划线的 Scrape UDF，预期返回 true
    }

    @Test
    fun `isScrapeUDF should return false when input does not contain any allowed UDF`() {
        val result = isScrapeUDF("SELECT * FROM table")
        assertFalse(result)
    }

    @Test
    fun `isScrapeUDF should return false when input does not match the regex`() {
        val result = isScrapeUDF("SELECT scrape FROM table")
        assertFalse(result)
    }

    @Test
    fun `isScrapeUDF should return true when input contains allowed UDF and matches the regex`() {
        val result = isScrapeUDF("SELECT scrape FROM http://example.com")
        assertFalse(result)
    }

    @Test
    fun `test ScrapeAPIUtils methods`() {
        val sql = "select dom_base_uri(dom) as uri from load_and_select('{url}', ':root')"
        val request = ScrapeRequest(sql)

        assertTrue(isScrapeUDF(sql), "SQL is not a scrape UDF")
        ScrapeAPIUtils.checkSql(sql)

        val url = ScrapeAPIUtils.extractConfiguredUrl(sql)
        assertNull(url)
    }
}