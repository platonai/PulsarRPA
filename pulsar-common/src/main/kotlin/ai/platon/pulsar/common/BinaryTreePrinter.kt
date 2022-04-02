package ai.platon.pulsar.common

object BTreePrinter {

    const val maxFloor = 1

    fun <T: Comparable<*>> print(root: ai.platon.pulsar.common.BinaryTreeNode<T>) {
        ai.platon.pulsar.common.BTreePrinter.printBinaryTreeNodeInternal(listOf(root), 1, ai.platon.pulsar.common.BTreePrinter.depthOf(root))
    }

    private fun <T : Comparable<*>> printBinaryTreeNodeInternal(nodes: List<ai.platon.pulsar.common.BinaryTreeNode<T>?>, level: Int, depth: Int) {
        if (nodes.isEmpty() || nodes.all { it == null }) {
            return
        }

        val floor = depth - level
//        val edgeLines = if (floor > maxFloor) 1 else Math.pow(2.0, Math.max(floor - 1.0, 0.0)).toInt()
//        val firstSpaces = if (floor > maxFloor) 1 else Math.pow(2.0, floor.toDouble()).toInt() - 1
//        val betweenSpaces = if (floor > maxFloor) 1 else Math.pow(2.0, (floor + 1).toDouble()).toInt() - 1
        val edgeLines = 1
        val firstSpaces = 1
        val betweenSpaces = 1

        ai.platon.pulsar.common.BTreePrinter.printWhitespaces(firstSpaces)

        val newBinaryTreeNodes = ArrayList<ai.platon.pulsar.common.BinaryTreeNode<T>?>()
        for (node in nodes) {
            if (node != null) {
                print(node.data.toString())
                ai.platon.pulsar.common.BTreePrinter.printWhitespaces(2)
                newBinaryTreeNodes.add(node.left)
                newBinaryTreeNodes.add(node.right)
            } else {
                newBinaryTreeNodes.add(null)
                newBinaryTreeNodes.add(null)
                // print(" ")
            }
            // printWhitespaces(betweenSpaces)
        }

        for (i in 1..edgeLines) {
            for (j in nodes.indices) {
                // printWhitespaces(firstSpaces - i)
                if (nodes[j] == null) {
                    // printWhitespaces(edgeLines + edgeLines + i + 1)
                    continue
                }

                if (nodes[j]?.left != null) {
                    print("/")
                }
                else {
                    ai.platon.pulsar.common.BTreePrinter.printWhitespaces(1)
                }

                // printWhitespaces(i + i - 1)

                if (nodes[j]?.right != null) {
                    print("\\")
                }
                else {
                    ai.platon.pulsar.common.BTreePrinter.printWhitespaces(1)
                }

                // printWhitespaces(edgeLines + edgeLines - i)
            }
        }

        ai.platon.pulsar.common.BTreePrinter.printBinaryTreeNodeInternal(newBinaryTreeNodes, level + 1, depth)
    }

    private fun printWhitespaces(count: Int) {
        if (count > 0) {
            print(" ".repeat(count))
        }
    }

    private fun <T : Comparable<*>> depthOf(node: ai.platon.pulsar.common.BinaryTreeNode<T>?): Int {
        return if (node == null) 0 else Math.max(ai.platon.pulsar.common.BTreePrinter.depthOf(node.left), ai.platon.pulsar.common.BTreePrinter.depthOf(node.right)) + 1
    }
}
