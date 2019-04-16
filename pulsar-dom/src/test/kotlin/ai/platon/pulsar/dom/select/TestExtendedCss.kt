package ai.platon.pulsar.dom.select

import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.FeaturedDocument.Companion.SELECTOR_IN_BOX_DEVIATION
import ai.platon.pulsar.dom.features.defined.HEIGHT
import ai.platon.pulsar.dom.features.defined.WIDTH
import ai.platon.pulsar.dom.nodes.node.ext.canonicalName
import ai.platon.pulsar.dom.nodes.node.ext.getFeature
import ai.platon.pulsar.dom.nodes.node.ext.select
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal data class Box(val width: Int, val height: Int) {
    override fun toString(): String {
        return "${width}x${height}"
    }
}

class TestExtendedCss {

    private val resourceName = "/webpages/mia.com/00f3a63c4898d201df95d6015244dd63.html"
    private val baseUri = "jar:/$resourceName"
    private val stream = javaClass.getResourceAsStream(resourceName)
    private val doc: FeaturedDocument = Documents.parse(stream, "UTF-8", baseUri)
    private val box = Box(447, 447)
    private val box2 = Box(463, 439)

    @Before
    fun teardown() {
        stream.close()
    }

    @Test
    fun testByExpression() {
        val expr = "img == 1 && width > 400 && width < 500 && height > 400 && height < 500"
        val elements = doc.select("div:expr($expr)")
        assertEquals(1, elements.size)
        assertTrue { elements.select("img").isNotEmpty() }
        assertTrue { elements.last().getFeature(WIDTH) > 400 }
        assertTrue { elements.last().getFeature(HEIGHT) < 500 }
    }

    @Test
    fun testByExpression2() {
        val expr = "width > 400 && width < 500 && height > 400 && height < 500"
        val elements = doc.select("*:expr($expr)")
        assertEquals(3, elements.size)
        assertTrue { elements.last().getFeature(WIDTH) > 400 }
        assertTrue { elements.last().getFeature(HEIGHT) < 500 }
        // elements.forEach { println("\n\n\n${it.uniqueName}\n$it") }
    }

    @Test
    fun testByBoxAccurate() {
        val box = "$box   ,$box2"
        val elements = doc.select(convertCssQuery(box))
        assertEquals(3, elements.size)
        assertEquals("div.big.rel", elements[0].canonicalName)
        assertEquals("img", elements[1].canonicalName)
        assertEquals("div.pi_attr_box", elements[2].canonicalName)
        // elements.forEach { println("\n\n\n${it.uniqueName}\n$it") }
    }

    @Test
    fun testByBoxInaccurate() {
        val box = " 450x450     , 470x440  "
        val elements = doc.select(convertCssQuery(box))
        assertEquals(3, elements.size)
        assertEquals("div.big.rel", elements[0].canonicalName)
        assertEquals("img", elements[1].canonicalName)
        assertEquals("div.pi_attr_box", elements[2].canonicalName)
    }

    @Test
    fun testByBoxInaccurate2() {
        for (i in 0..5) {
            assertTrue { i * 5 <= SELECTOR_IN_BOX_DEVIATION }

            val w = box.width + i * 5
            val h = box.height + i * 5
            val elements = doc.select(convertCssQuery("${w}x${h}"))
            // elements.forEach { println(it) }
            assertEquals(2, elements.size)
            assertEquals("div.big.rel", elements[0].canonicalName)
            assertEquals("img", elements[1].canonicalName)
        }

        val w = box.width + SELECTOR_IN_BOX_DEVIATION + 1
        val h = box.height + SELECTOR_IN_BOX_DEVIATION + 1
        val elements = doc.select("$w x $h")
        assertTrue { elements.isEmpty() }
    }
}
