package ai.platon.pulsar.common.sql

import kotlin.test.*
import kotlin.test.assertEquals

class TestSQLTemplate {
    private val url = "https://www.amazon.com/s?k=Baby+Girls'+One-Piece+Footies&rh=node:2475809011&page=1"
    private val sanitizedUrl = "https://www.amazon.com/s?k=Baby+Girls^27+One-Piece+Footies&rh=node:2475809011&page=1"

    private val templates = mapOf(
        "set style" to
                " select" +
                " dom_base_uri(dom) as uri" +
                " from load_and_select(@url, ':root')",
        "double parentheses" to
                " select" +
                " dom_base_uri(dom) as uri" +
                " from load_and_select('{{url}}', ':root')",
        "metabase style" to
                " select" +
                " dom_base_uri(dom) as uri" +
                " from load_and_select({{snippet: url}}, ':root')",
    )

    private val expectedSQL = " select" +
            " dom_base_uri(dom) as uri" +
            " from load_and_select('$sanitizedUrl', ':root')"

    @Test
    fun testSanitizeUrl() {
        assertEquals(sanitizedUrl, SQLUtils.sanitizeUrl(url))
        assertEquals(url, SQLUtils.unsanitizeUrl(sanitizedUrl))
        assertEquals(url, SQLUtils.unsanitizeUrl(SQLUtils.sanitizeUrl(url)))
    }

    @Test
    fun testCreateInstance() {
        val sqlTemplates = templates.entries.associate { it.key to SQLTemplate(it.value).createSQL(url) }
        sqlTemplates.forEach { (name, template) ->
            assertEquals(expectedSQL, template, name)
        }
    }
}
