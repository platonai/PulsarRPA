package ai.platon.pulsar.browser.driver.chrome.dom

import ai.platon.pulsar.browser.driver.chrome.dom.model.DOMTreeNodeEx
import ai.platon.pulsar.browser.driver.chrome.dom.model.NodeType
import ai.platon.pulsar.browser.driver.chrome.dom.util.CSSSelectorUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CSSSelectorUtilsTest {

    @Test
    fun `uses id when valid css identifier`() {
        val node = DOMTreeNodeEx(
            nodeName = "DIV",
            attributes = mapOf("id" to "main")
        )
        val sel = CSSSelectorUtils.generateCSSSelector(node)
        assertEquals("#main", sel)
    }

    @Test
    fun `uses attribute selector when id is not a valid css identifier and escapes`() {
        val node = DOMTreeNodeEx(
            nodeName = "DIV",
            attributes = mapOf("id" to "a\"b\\c")
        )
        val sel = CSSSelectorUtils.generateCSSSelector(node)
        // Expect: div[id="a\"b\\c"]
        assertEquals("div[id=\"a\\\"b\\\\c\"]", sel)
    }

    @Test
    fun `uses up to three stable classes in order`() {
        val node = DOMTreeNodeEx(
            nodeName = "div",
            attributes = mapOf(
                "class" to "btn  x__ y.. hashed-123 primary card"
            )
        )
        // Heuristics:
        // - "btn" -> letters >= 2? yes (b,t,n) => keep
        // - "x__" -> letters = 1 => drop
        // - "y.." -> contains '.' => drop
        // - "hashed-123" -> letters >= 2 => keep
        // - "primary" -> keep
        // - "card" -> would be 4th stable but we only take first 3
        val sel = CSSSelectorUtils.generateCSSSelector(node)
        assertEquals("div.btn.hashed-123.primary", sel)
    }

    @Test
    fun `class selector without tag when tag is wildcard`() {
        val node = DOMTreeNodeEx(
            nodeName = "", // will become "*"
            attributes = mapOf("class" to "btn primary")
        )
        val sel = CSSSelectorUtils.generateCSSSelector(node)
        assertEquals(".btn.primary", sel)
    }

    @Test
    fun `falls back to preferred attributes`() {
        val node = DOMTreeNodeEx(
            nodeName = "SPAN",
            attributes = mapOf("aria-label" to "Close")
        )
        val sel = CSSSelectorUtils.generateCSSSelector(node)
        assertEquals("span[aria-label=\"Close\"]", sel)
    }

    @Test
    fun `input prefers value attribute`() {
        val node = DOMTreeNodeEx(
            nodeName = "INPUT",
            attributes = mapOf("value" to "Search")
        )
        val sel = CSSSelectorUtils.generateCSSSelector(node)
        assertEquals("input[value=\"Search\"]", sel)
    }

    @Test
    fun `last resort is lowercase tag`() {
        val node = DOMTreeNodeEx(
            nodeName = "Section",
            attributes = emptyMap()
        )
        val sel = CSSSelectorUtils.generateCSSSelector(node)
        assertEquals("section", sel)
    }

    @Test
    fun `non-element returns name lowercased or star when blank`() {
        val textWithName = DOMTreeNodeEx(
            nodeType = NodeType.TEXT_NODE,
            nodeName = "#TEXT",
            nodeValue = "hello"
        )
        assertEquals("#text", CSSSelectorUtils.generateCSSSelector(textWithName))

        val textBlankName = DOMTreeNodeEx(
            nodeType = NodeType.TEXT_NODE,
            nodeName = "",
            nodeValue = "hello"
        )
        assertEquals("*", CSSSelectorUtils.generateCSSSelector(textBlankName))
    }

    // New tests for tree scenarios

    @Test
    fun `selectors in a simple tree`() {
        val li1 = DOMTreeNodeEx(
            nodeName = "LI",
            attributes = mapOf("class" to "item primary abcd1234"),
            children = listOf(
                DOMTreeNodeEx(nodeType = NodeType.TEXT_NODE, nodeName = "#text", nodeValue = "Home")
            )
        )
        val li2 = DOMTreeNodeEx(
            nodeName = "LI",
            attributes = mapOf("class" to "item secondary"),
            children = listOf(
                DOMTreeNodeEx(nodeType = NodeType.TEXT_NODE, nodeName = "#text", nodeValue = "About")
            )
        )
        val ul = DOMTreeNodeEx(
            nodeName = "UL",
            attributes = mapOf("class" to "menu main"),
            children = listOf(li1, li2)
        )
        val root = DOMTreeNodeEx(
            nodeName = "DIV",
            attributes = mapOf("id" to "root"),
            children = listOf(ul)
        )

        // Root prefers id
        assertEquals("#root", CSSSelectorUtils.generateCSSSelector(root))
        // UL uses classes
        assertEquals("ul.menu.main", CSSSelectorUtils.generateCSSSelector(ul))
        // LI picks up to three stable classes (here two)
        assertEquals("li.item.primary.abcd1234", CSSSelectorUtils.generateCSSSelector(li1))
        assertEquals("li.item.secondary", CSSSelectorUtils.generateCSSSelector(li2))
    }

    @Test
    fun `deeply nested node selector remains based on itself`() {
        val deepChild = DOMTreeNodeEx(
            nodeName = "SPAN",
            attributes = mapOf("aria-label" to "Badge")
        )
        val mid = DOMTreeNodeEx(
            nodeName = "DIV",
            attributes = mapOf("class" to "container hashed__999"),
            children = listOf(deepChild)
        )
        val root = DOMTreeNodeEx(
            nodeName = "SECTION",
            attributes = mapOf("id" to "profile"),
            children = listOf(mid)
        )
        // Even within a tree, selector uses node's own traits only
        assertEquals("#profile", CSSSelectorUtils.generateCSSSelector(root))
        // Heuristic keeps both stable classes
        assertEquals("div.container.hashed__999", CSSSelectorUtils.generateCSSSelector(mid))
        assertEquals("span[aria-label=\"Badge\"]", CSSSelectorUtils.generateCSSSelector(deepChild))
    }

    @Test
    fun `shadow root descendants are handled as regular nodes for selector`() {
        val insideShadow = DOMTreeNodeEx(
            nodeName = "INPUT",
            attributes = mapOf("id" to "a b", "value" to "Ignored because id present"))
        val host = DOMTreeNodeEx(
            nodeName = "DIV",
            attributes = mapOf("class" to "host"),
            shadowRoots = listOf(
                DOMTreeNodeEx(nodeName = "#SHADOW-ROOT", children = listOf(insideShadow))
            )
        )
        // Host uses classes
        assertEquals("div.host", CSSSelectorUtils.generateCSSSelector(host))
        // Inside shadow uses attribute selector for non-identifier id
        assertEquals("input[id=\"a b\"]", CSSSelectorUtils.generateCSSSelector(insideShadow))
    }

    @Test
    fun `wildcard tag in tree yields class-only selector`() {
        val child = DOMTreeNodeEx(
            nodeName = "",
            attributes = mapOf("class" to "chip primary"))
        val parent = DOMTreeNodeEx(
            nodeName = "DIV",
            attributes = mapOf("class" to "container"),
            children = listOf(child)
        )
        // Parent uses classes
        assertEquals("div.container", CSSSelectorUtils.generateCSSSelector(parent))
        // Child has wildcard tag -> class-only
        assertEquals(".chip.primary", CSSSelectorUtils.generateCSSSelector(child))
    }
}
