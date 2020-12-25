package ai.platon.pulsar.dom.parsers

import ai.platon.pulsar.dom.select.selectFirstOrNull
import org.jsoup.nodes.Element

data class TreeNode(
        val url: String,
        val text: String,
        @Transient
        var parent: TreeNode? = null,
        @Transient
        var prev: TreeNode? = null,
        @Transient
        var next: TreeNode? = null
) {
    val children: MutableList<TreeNode> = mutableListOf()
    var indent = 0

    override fun toString(): String {
        return "($text)[$url]"
    }
}

/**
 * TODO: not an general solution
 * */
class TreeParser1(val root: Element) {

    fun parse(): TreeNode {
        val rootNode = TreeNode("", "root")
        parse(root, rootNode)
        return rootNode
    }

    private fun parse(rootElement: Element, rootTree: TreeNode): TreeNode {
        var prev: TreeNode? = null
        rootElement.select("li").forEachIndexed { i, item ->
            val url = item.selectFirstOrNull("a")?.absUrl("href")?:""
            val text = item.text()
            if (url.isNotBlank() || text.isNotBlank()) {
                val node = TreeNode(url, text)
                val classes = item.attr("class")
                node.indent = when {
                    "indent-1" in classes -> 1
                    "indent-2" in classes -> 2
                    "indent-3" in classes -> 3
                    else -> 0
                }

                rootTree.children.add(node)
                node.parent = rootTree
                node.prev = prev
                prev?.next = node
                prev = node
            }
        }

        var tail = rootTree.children.lastOrNull()
        while (tail != null && tail.prev?.indent?:1000 < tail.indent) {
            rootTree.children.remove(tail)
            val newParent = tail.prev
            if (newParent != null) {
                tail.parent = newParent
                tail.prev = newParent.children.lastOrNull()
                newParent.children.add(tail)
            }
            tail = rootTree.children.lastOrNull()
        }

        return rootTree
    }
}
