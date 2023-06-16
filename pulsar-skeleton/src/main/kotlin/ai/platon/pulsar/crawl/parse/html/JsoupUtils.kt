package ai.platon.pulsar.crawl.parse.html

import com.google.common.collect.Sets
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.NodeTraversor
import java.util.*
import java.util.function.Consumer

/**
 * Created by vincent on 17-8-9.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
object JsoupUtils {
    fun sanitize(doc: Document, pithy: Boolean): Document {
        val unsafeNodes: Set<String> = Sets.newHashSet(
                "title", "base", "script", "meta", "iframe", "link[ref=icon]", "link[ref=\"shortcut icon\"]")
        val obsoleteNodeNames: Set<String> = Sets.newHashSet("style", "link", "head")
        val obsoleteNodes: MutableSet<Node> = HashSet()
        NodeTraversor.traverse({ node: Node, depth: Int ->
            val nodeName = node.nodeName()
            if (unsafeNodes.contains(nodeName)) {
                obsoleteNodes.add(node)
            }
            if (pithy) {
                node.removeAttr("style")
                if (obsoleteNodeNames.contains(nodeName)) {
                    obsoleteNodes.add(node)
                }
            }
        }, doc)

        obsoleteNodes.forEach { obj: Node -> obj.remove() }

        NodeTraversor.traverse({ node: Node?, _: Int ->
            if (node !is Element) {
                return@traverse
            }
            val ele = node
            if (ele.id().isEmpty() && ele.className().isEmpty()) {
                return@traverse
            }
            val selector = ele.cssSelector()
            ele.attr("warpsselector", selector)
            ele.addClass("warpsselector") // Deprecated
            ele.addClass("has-selector")
        }, doc)
        for (ele in doc.select("html,head,body")) { // ele.clearAttrs();
            ele.attr("id", "pulsar" + StringUtils.capitalize(ele.nodeName()))
        }
        return doc
    }

    fun toHtmlPiece(doc_: Document, pithy: Boolean): String {
        var doc = doc_
        doc = sanitize(doc, pithy)
        var content = doc.toString()
        val pos = StringUtils.indexOf(content, "<html")
        if (pos > 0) {
            content = content.substring(pos)
        }
        content = content
                .replaceFirst("<html".toRegex(), "<div")
                .replaceFirst("<body".toRegex(), "<div")
                .replaceFirst("<head".toRegex(), "<div")
                .replace("</html|</body|</head".toRegex(), "</div")
        return content
    }
}
