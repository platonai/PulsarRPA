package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.common.sql.SQLTemplate
import ai.platon.pulsar.test.TestHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

open class ScrapeControllerTestBase : IntegrationTestBase() {

    companion object {
        val urls = mapOf(
            "productListPage" to "https://www.amazon.com/b?node=1292115011",
            "productDetailPage" to "https://www.amazon.com/dp/B0C1H26C46"
        )

        @JvmStatic
        @BeforeAll
        fun `Ensure resources are prepared`() {
            TestHelper.ensurePage(requireNotNull(urls["productListPage"]))
            TestHelper.ensurePage(requireNotNull(urls["productDetailPage"]))
        }
    }

    val sqlTemplates = mapOf(
        "productListPage" to """
        select
            dom_base_uri(dom) as `url`,
            str_substring_after(dom_base_uri(dom), '&rh=') as `nodeID`,
            dom_first_text(dom, 'a span.a-price:first-child span.a-offscreen') as `price`,
            dom_first_text(dom, 'a:has(span.a-price) span:containsOwn(/Item)') as `priceperitem`,
            dom_first_text(dom, 'a span.a-price[data-a-strike] span.a-offscreen') as `listprice`,
            dom_first_text(dom, 'h2 a') as `title`,
            dom_height(dom_select_first(dom, 'a img[srcset]')) as `pic_height`
        from load_and_select(@url, 'div[class*=search-result]');
    """.trimIndent(),
        "productDetailPage" to """
            select
              llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
              dom_base_uri(dom) as url,
              dom_first_text(dom, '#productTitle') as title,
              dom_first_slim_html(dom, 'img:expr(width > 400)') as img
            from load_and_select(@url, 'body');
        """.trimIndent()
    ).entries.associate { it.key to SQLTemplate(it.value) }

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `When say hello then returns hello`() {
        assertThat(
            restTemplate.getForObject("$baseUri/api/system/hello", String::class.java)
        ).contains("hello")
    }
}
