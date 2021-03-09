package ai.platon.pulsar.common.url

import java.util.*

class UrlTree() {
    val root: Node = Node()

    inner class Node(val path: String = "", val depth: Int = 0) {
        val children = ArrayList<Node>()
        val leaves = ArrayList<Node>()

        fun add(first: String, more: List<String>) {
            val newPath = "$first/${more.first()}".trimStart { it == '/' }
            val child = Node(newPath, depth.inc())

            if (more.size == 1) {
                leaves.add(child)
            } else {
                val index = children.indexOf(child)
                if (index == -1) {
                    children.add(child)
                    child.add(child.path, more.drop(1))
                } else {
                    val nextChild = children[index]
                    nextChild.add(child.path, more.drop(1))
                }
            }
        }

        fun print() {
            print("\t".repeat(depth))
            println(path)
            children.forEach { it.print() }
            leaves.forEach { it.print() }
        }

        override fun toString(): String {
            return path
        }

        override fun equals(other: Any?): Boolean {
            return other is Node && other.path == path
        }

        override fun hashCode(): Int {
            return path.hashCode()
        }
    }

    fun add(url: String) {
        val parts = url.split("/".toRegex()).dropLastWhile { it.isEmpty() }
        root.add("", parts)
    }

    fun print() {
        root.print()
        // commonRoot.print()
    }
}
