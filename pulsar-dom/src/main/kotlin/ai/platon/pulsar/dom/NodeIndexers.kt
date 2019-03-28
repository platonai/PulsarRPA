package ai.platon.pulsar.dom

import ai.platon.pulsar.dom.nodes.node.ext.getFeature
import ai.platon.pulsar.dom.nodes.node.ext.sequence
import ai.platon.pulsar.dom.nodes.nodeComparator
import ai.platon.pulsar.dom.select.AbstractElementVisitor
import com.google.common.collect.TreeMultimap
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.math.roundToInt

typealias NodeIndexer = TreeMultimap<String, Node>

typealias IntNodeIndexer = TreeMultimap<Int, Node>

class MultiIntNodeIndexer(
        vararg indexerKeys: Int,
        keyComparator: Comparator<Int> = reverseOrder(),
        valueComparator: Comparator<Node> = nodeComparator
) {
    val indexes = HashMap<Int, IntNodeIndexer>()

    init {
        for (key in indexerKeys) {
            indexes[key] = IntNodeIndexer.create(keyComparator, valueComparator)
        }
    }

    operator fun get(indexerKey: Int): IntNodeIndexer? {
        return indexes[indexerKey]
    }

    fun get(indexerKey: Int, key: Int): Set<Node>? {
        val indexer = indexes[indexerKey]
        return indexer?.get(key)
    }

    fun keySet(): Set<Int> {
        return indexes.keys
    }

    fun put(indexerKey: Int, indexer: IntNodeIndexer): IntNodeIndexer? {
        return indexes.put(indexerKey, indexer)
    }

    fun put(indexerKey: Int, key: Int, ele: Node): Boolean {
        val map = indexes[indexerKey]
        return map != null && map.put(key, ele)
    }

    fun clear() {
        for (indexer in indexes.values) {
            indexer.clear()
        }
        indexes.clear()
    }
}

class MultiNodeIndexer(
        vararg indexerNames: String,
        keyComparator: Comparator<String> = naturalOrder(),
        valueComparator: Comparator<Node> = nodeComparator
) {

    val indexes = HashMap<String, NodeIndexer>()

    init {
        for (key in indexerNames) {
            indexes[key] = NodeIndexer.create(keyComparator, valueComparator)
        }
    }

    operator fun get(indexerName: String): NodeIndexer? {
        return indexes[indexerName]
    }

    fun get(indexerName: String, key: String): Set<Node>? {
        val indexer = indexes[indexerName]
        return indexer?.get(key)
    }

    fun keySet(): Set<String> {
        return indexes.keys
    }

    fun nthElements(indexerName: String, n: Int): Set<Node>? {
        var n = n
        val indexer = indexes[indexerName]

        if (indexer != null) {
            val it = indexer.keySet().iterator()
            do {
                if (it.hasNext() && n == 0) {
                    return indexer.get(it.next())
                }
            } while (it.hasNext() && n-- > 0)
        }

        return TreeSet(Comparator.comparingInt(Node::sequence))
    }

    fun put(indexerName: String, indexer: NodeIndexer): NodeIndexer? {
        return indexes.put(indexerName, indexer)
    }

    fun put(indexerName: String, key: String, ele: Node): Boolean {
        val map = indexes[indexerName]
        return map != null && map.put(key, ele)
    }

    fun clear() {
        for (indexer in indexes.values) {
            indexer.clear()
        }
        indexes.clear()
    }
}

class NodeIndexerCalculator(
        private val multiIndexer: MultiIntNodeIndexer,
        private val featuresToIndex: IntArray
) : AbstractElementVisitor() {

    override fun head(ele: Element, depth: Int) {
        for (feature in featuresToIndex) {
            val value = ele.getFeature(feature)
            if (value > 0) {
                multiIndexer.put(feature, value.roundToInt(), ele)
            }
        }
    }
}
