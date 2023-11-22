package ai.platon.pulsar.dom.select

import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeTraversor
import kotlin.test.Test
import kotlin.test.assertEquals

class TestTraversor {

    @Test
    fun filterVisit() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        NodeTraversor.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                accum.append("<" + node.nodeName() + ">")
                return NodeFilter.FilterResult.CONTINUE
            }

            override fun tail(node: Node, depth: Int): NodeFilter.FilterResult {
                accum.append("</" + node.nodeName() + ">")
                return NodeFilter.FilterResult.CONTINUE
            }
        }, doc.select("div"))
        assertEquals("<div><p><#text></#text></p></div><div><#text></#text></div>", accum.toString())
    }

    @Test
    fun filterSkipChildren() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        NodeTraversor.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                accum.append("<" + node.nodeName() + ">")
                // OMIT contents of p:
                return if ("p" == node.nodeName()) NodeFilter.FilterResult.SKIP_CHILDREN else NodeFilter.FilterResult.CONTINUE
            }

            override fun tail(node: Node, depth: Int): NodeFilter.FilterResult {
                accum.append("</" + node.nodeName() + ">")
                return NodeFilter.FilterResult.CONTINUE
            }
        }, doc.select("div"))
        assertEquals("<div><p></p></div><div><#text></#text></div>", accum.toString())
    }

    @Test
    fun filterSkipEntirely() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        NodeTraversor.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                // OMIT p:
                if ("p" == node.nodeName())
                    return NodeFilter.FilterResult.SKIP_ENTIRELY
                accum.append("<" + node.nodeName() + ">")
                return NodeFilter.FilterResult.CONTINUE
            }

            override fun tail(node: Node, depth: Int): NodeFilter.FilterResult {
                accum.append("</" + node.nodeName() + ">")
                return NodeFilter.FilterResult.CONTINUE
            }
        }, doc.select("div"))
        assertEquals("<div></div><div><#text></#text></div>", accum.toString())
    }

    @Test
    fun filterRemove() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div>There be <b>bold</b></div>")
        NodeTraversor.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                // Delete "p" in head:
                return if ("p" == node.nodeName()) NodeFilter.FilterResult.REMOVE else NodeFilter.FilterResult.CONTINUE
            }

            override fun tail(node: Node, depth: Int): NodeFilter.FilterResult {
                // Delete "b" in tail:
                return if ("b" == node.nodeName()) NodeFilter.FilterResult.REMOVE else NodeFilter.FilterResult.CONTINUE
            }
        }, doc.select("div"))
        assertEquals("<div></div>\n<div>\n There be \n</div>", doc.select("body").html())
    }

    @Test
    fun filterStop() {
        val doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>")
        val accum = StringBuilder()
        NodeTraversor.filter(object : NodeFilter {
            override fun head(node: Node, depth: Int): NodeFilter.FilterResult {
                accum.append("<" + node.nodeName() + ">")
                return NodeFilter.FilterResult.CONTINUE
            }

            override fun tail(node: Node, depth: Int): NodeFilter.FilterResult {
                accum.append("</" + node.nodeName() + ">")
                // Stop after p.
                return if ("p" == node.nodeName()) NodeFilter.FilterResult.STOP else NodeFilter.FilterResult.CONTINUE
            }
        }, doc.select("div"))
        assertEquals("<div><p><#text></#text></p>", accum.toString())
    }
}
