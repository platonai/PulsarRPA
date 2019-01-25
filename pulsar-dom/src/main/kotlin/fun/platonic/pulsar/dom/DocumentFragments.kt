package `fun`.platonic.pulsar.dom

import `fun`.platonic.pulsar.common.FuzzyProbability
import `fun`.platonic.pulsar.common.math.vectors.get
import `fun`.platonic.pulsar.dom.data.BlockPattern.Companion.DenseLinks
import `fun`.platonic.pulsar.dom.data.BlockPattern.Companion.Dl
import `fun`.platonic.pulsar.dom.data.BlockPattern.Companion.Table
import `fun`.platonic.pulsar.dom.features.defined.SEQ
import `fun`.platonic.pulsar.dom.model.PageEntity
import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

class DocumentFragments(
        val document: FeaturedDocument = FeaturedDocument.NIL,
        val fragments: TreeMap<Int, DocumentFragment> = TreeMap(),
        val pageEntity: PageEntity = PageEntity()
) : MutableMap<Int, DocumentFragment> {

    companion object {
        val log = LoggerFactory.getLogger(DocumentFragments::class.java)
        val EMPTY = DocumentFragments()

        var blockLikelihoodThreshold = 0.95
        var defaultProbability = FuzzyProbability.MAYBE
        var analogueTags = mutableListOf("div", "ul", "ol", "dl", "nav", "table")
        var patternsWithoutChildren = mutableListOf(DenseLinks, Table, Dl)

        val globalSummaries = ConcurrentHashMap<Int, SynchronizedSummaryStatistics>()
    }

    val summaries = HashMap<Int, DoubleSummaryStatistics>()

    override val size: Int get() = fragments.size

    override fun containsKey(key: Int): Boolean {
        return fragments.containsKey(key)
    }

    override fun containsValue(value: DocumentFragment): Boolean {
        return fragments.containsValue(value)
    }

    override operator fun get(key: Int): DocumentFragment? {
        return fragments[key]
    }

    override fun isEmpty(): Boolean {
        return fragments.isEmpty()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<Int, DocumentFragment>>
        get() = fragments.entries

    override val keys: MutableSet<Int>
        get() = fragments.keys

    override val values: MutableCollection<DocumentFragment>
        get() = fragments.values

    override fun put(key: Int, value: DocumentFragment): DocumentFragment? {
        return fragments.put(key, accept(value))
    }

    override fun putAll(from: Map<out Int, DocumentFragment>) {
        from.values.forEach { accept(it) }
        fragments.putAll(from)
    }

    override fun remove(key: Int): DocumentFragment? {
        val old = fragments.remove(key)
        return old
    }

    override fun clear() {
        fragments.clear()
    }

    private fun accept(fragment: DocumentFragment): DocumentFragment {
        fragment.fragments = this
        summaries.computeIfAbsent(SEQ) { DoubleSummaryStatistics() }
                .accept(fragment.element.features[SEQ])
        globalSummaries.computeIfAbsent(SEQ) { SynchronizedSummaryStatistics() }
                .addValue(fragment.element.features[SEQ])
        return fragment
    }
}
