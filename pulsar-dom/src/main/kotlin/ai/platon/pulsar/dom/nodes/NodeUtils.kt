package ai.platon.pulsar.dom.nodes

import ai.platon.pulsar.dom.nodes.node.ext.isAnchor
import ai.platon.pulsar.dom.nodes.node.ext.isImage
import ai.platon.pulsar.dom.nodes.node.ext.parentElement
import org.jsoup.nodes.*
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor

internal class OnlyChildElementRemoval(private val root: Element): NodeVisitor {
    val tmpNodes = mutableListOf<Node>()

    override fun head(node: Node, depth: Int) {
    }

    override fun tail(node: Node, depth: Int) {
        println(node.nodeName() + "  " + " root: " + root.nodeName()  + " | ")
        if (node != root && node is Element) {
            val parent = node.parent()
            val pParent = parent?.parent()
            if (node != root && parent != root
                && parent is Element
                && pParent is Element
                && parent.children().size == 1
            ) {
                tmpNodes.add(parent)
            }
        }
    }
}

internal fun removeElementsWithOnlyChild(root: Element) {
    // remove elements whose have only one child

    val visitor = OnlyChildElementRemoval(root)
    NodeTraversor.traverse(OnlyChildElementRemoval(root), root)

    visitor.tmpNodes.forEach { node ->
        if (node is Element) {
            node.childNodes().forEach { node.parentElement.appendChild(it) }
        }
        node.remove()
    }
}

internal fun shouldRemoveToSimplify(node: Node): Boolean {
    val tagName = node.nodeName().lowercase()

    if (tagName in listOf("style", "script", "input")) {
        return true
    }

    if (node is Comment || node is CDataNode || node is XmlDeclaration) {
        return true
    }

    if (node is Element && !node.isImage && !node.isAnchor) {
        val text = node.text().trim()
        if (text.isEmpty()) {
            return true
        }
    }

    return false
}

internal fun simplifyDOM(root: Element): Element {
    val tmpNodes = mutableListOf<Node>()

    root.forEach { node ->
        if (shouldRemoveToSimplify(node)) {
            tmpNodes.add(node)
        }
    }
    tmpNodes.forEach { it.remove() }

    return root
}
