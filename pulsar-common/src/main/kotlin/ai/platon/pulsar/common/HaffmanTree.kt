package ai.platon.pulsar.common

import java.util.*

class HuffmanTree<T: Comparable<T>>(frequency: Frequency<T>) {
    val root: Node<T>

    /**
     * Huffman coding requires one to repeatedly obtain the two
     * lowest-frequency trees. A priority queue makes this efficient
     * */
    init {
        val nodes = frequency.entrySet()
                .mapTo(PriorityQueue<Node<T>>()) { Node(it.element, it.count) }

        var i = 0
        frequency.entrySet().forEach {
            ++i
            if (nodes.size > 1) {
                val smallest = nodes.remove()
                val nextSmallest = nodes.remove()
                val node = Node(it.element, it.count)
                node.frequency = smallest.frequency + nextSmallest.frequency
                node.left = smallest
                node.right = nextSmallest
                nodes.add(node)
            }
        }

        val sz = frequency.entrySet().size
        println("$i while size is $sz")

        root = nodes.remove()
    }

    fun print() {
        root.print(2)
    }

    fun traverse(root: Node<T>?, visitor: (Node<T>, Int) -> Unit) {
        val node = root
        val depth = 0

        while (node != null) {
            visitor(node, depth)
            traverse(node.left, visitor)
            traverse(node.right, visitor)
        }
    }

    fun decode(input: String): String {
        var result = ""
        var n: Node<T>? = root

        for (i in 0 until input.length) {
            val ch = input[i]
            if (ch == '0') {
                n = n?.left
            } else {
                n = n?.right
            }

            if (n?.left == null) {
                if (n != null) {
                    result += n.data
                    n = root
                }
            }
        }

        return result
    }

    fun getEncodingMap(): Map<T, String> {
        return root.fillEncodingMap(HashMap(), "")
    }

    inner class Node<T>(
            var data: T,
            var frequency: Int,
            var left: Node<T>? = null,
            var right: Node<T>? = null
    ): Comparable<Node<T>> {

        fun fillEncodingMap(map: MutableMap<T, String>, prefix: String): Map<T, String> {
            if (left == null) {
                map[data] = prefix
            } else {
                left?.fillEncodingMap(map, prefix + "0")
                right?.fillEncodingMap(map, prefix + "1")
            }
            return map
        }

        fun print(margin: Int = 2) {
            print("\t".repeat(margin))
            println(data)
            left?.print(2 + margin)
            right?.print(2 + margin)
        }

        fun copy(): Node<T> {
            return Node(data, frequency, this.left?.copy(), this.right?.copy())
        }

        fun convert(): BinaryTreeNode<String> {
            return BinaryTreeNode(this.toString(), this.left?.convert(), this.right?.convert())
        }

        override fun compareTo(other: Node<T>): Int {
            return frequency - other.frequency
        }

        override fun toString(): String {
            return "$data:$frequency"
        }
    }
}
