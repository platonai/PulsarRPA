package `fun`.platonic.pulsar.common

import java.util.*

class FrequencyTree(frequency: Frequency<String>) {
    val root: Node

    /**
     * Huffman coding requires one to repeatedly obtain the two
     * lowest-frequency trees. A priority queue makes this efficient
     * */
    init {
        val queue = frequency.entrySet()
                .filter { it.element.isNotEmpty() }
                .mapTo(PriorityQueue<Node>()) { Node(it.element, it.count) }

        while (queue.size > 1) {
            val most = queue.remove()
            val nextMost = queue.remove()
            val node = Node("", most.frequency + nextMost.frequency)
            node.left = most
            node.right = nextMost
            queue.add(node)
        }

        root = queue.remove()
    }

    fun print() {
        root.print(2)
    }

    inner class Node(
            var data: String,
            var frequency: Int,
            var left: Node? = null,
            var right: Node? = null
    ): Comparable<Node> {

        fun print(margin: Int = 2) {
            print("\t".repeat(margin))
            println(this)
            left?.print(2 + margin)
            right?.print(2 + margin)
        }

        fun copy(): Node {
            return Node(data, frequency, this.left?.copy(), this.right?.copy())
        }

        fun convert(): BinaryTreeNode<String> {
            return BinaryTreeNode(this.toString(), this.left?.convert(), this.right?.convert())
        }

        override fun compareTo(other: Node): Int {
            return frequency - other.frequency
        }

        override fun toString(): String {
            return if (data.isEmpty()) "$frequency" else "$data:$frequency"
        }
    }
}
