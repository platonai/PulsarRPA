package ai.platon.pulsar.ql.udfs

import ai.platon.pulsar.ql.TestBase
import kotlin.test.*
import kotlin.test.assertTrue

class SelectFunctionTests: TestBase() {

    @Test
    fun testAllHrefs() {
        val page = session.load(productUrl)
        if (!page.protocolStatus.isSuccess) {
            logger.warn("Failed to load page | {}", productUrl)
            return
        }

        val sql = """
select
    dom_all_hrefs(dom, '#averageCustomerReviews a') as links
from load_and_select('$productUrl', ':root');
        """.trimIndent()
        val rs = query(sql)
        assertTrue(rs.next())
    }

    @Test
    fun testAllHrefs2() {
        val url = "https://www.amazon.de/dp/B08Y5SQTCX"
        val page = session.load(url)
        if (!page.protocolStatus.isSuccess) {
            logger.warn("Failed to load page | {}", url)
            return
        }

        val document = session.parse(page)
        document.select("#wayfinding-breadcrumbs_container ul li a").forEach {
            println("href: " + it.attr("abs:href"))
        }

        val sql = """
select
    str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), '&node=') as `category_node_id`,
    dom_all_hrefs(dom, '#wayfinding-breadcrumbs_container ul li a') as `category_link_path`,
    dom_first_text(dom, '#wayfinding-breadcrumbs_container ul li:last-child a') as `category_name`,
    array_join_to_string(dom_all_texts(dom, '#wayfinding-breadcrumbs_container ul li a'), ' -:> ') as `category_name_path`
from load_and_select('$url', ':root');
        """.trimIndent()
        val rs = query(sql)
        assertTrue(rs.next())
    }
}
