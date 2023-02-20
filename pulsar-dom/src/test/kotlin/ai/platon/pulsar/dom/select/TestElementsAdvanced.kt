package ai.platon.pulsar.dom.select

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for ElementList.
 */
class TestElementsAdvanced {
    val h1 = """
<a class="a-link-normal feedback-detail-description no-text-decoration" href="#">
    <i class="a-icon a-icon-star a-star-4-5 feedback-detail-stars">
        <span class="a-icon-alt">4.5 out of 5 stars</span></i>
    <i class="a-icon a-icon-text-separator" role="img" aria-label="|"></i>
    <b>91% positive</b> in the last 12 months (1708 ratings)
</a>
        """.trimIndent()
    val h2 = """
<a class="a-link-normal feedback-detail-description no-text-decoration" href="#"><i class="a-icon a-icon-star a-star-4-5 feedback-detail-stars"><span class="a-icon-alt">4.5 out of 5 stars</span></i><i class="a-icon a-icon-text-separator" role="img" aria-label="|"></i><b>91% positive</b> in the last 12 months (1708 ratings)</a>
        """.trimIndent()

    @Test
    fun text() {
        var text = Jsoup.parse(h1).text()
        assertEquals("4.5 out of 5 stars 91% positive in the last 12 months (1708 ratings)", text)

        text = Jsoup.parse(h2).text()
        assertEquals("4.5 out of 5 stars91% positive in the last 12 months (1708 ratings)", text)
    }
}
