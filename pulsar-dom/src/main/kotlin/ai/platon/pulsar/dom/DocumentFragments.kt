package ai.platon.pulsar.dom

import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.dom.data.BlockPattern.Companion.DenseLinks
import ai.platon.pulsar.dom.data.BlockPattern.Companion.Dl
import ai.platon.pulsar.dom.data.BlockPattern.Companion.Table
import ai.platon.pulsar.dom.features.defined.SEQ
import ai.platon.pulsar.dom.model.PageEntity
import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

class DocumentFragments(
        val document: ai.platon.pulsar.dom.FeaturedDocument = ai.platon.pulsar.dom.FeaturedDocument.Companion.NIL,
        val fragments: TreeMap<Int, ai.platon.pulsar.dom.DocumentFragment> = TreeMap(),
        val pageEntity: PageEntity = PageEntity()
) : MutableMap<Int, ai.platon.pulsar.dom.DocumentFragment> {

    companion object {
        val log = LoggerFactory.getLogger(ai.platon.pulsar.dom.DocumentFragments::class.java)
        val EMPTY = ai.platon.pulsar.dom.DocumentFragments()

        var blockLikelihoodThreshold = 0.95
        var defaultProbability = ai.platon.pulsar.common.FuzzyProbability.MAYBE
        var analogueTags = mutableListOf("div", "ul", "ol", "dl", "nav", "table")
        var patternsWithoutChildren = mutableListOf(DenseLinks, Table, Dl)

        val globalSummaries = ConcurrentHashMap<Int, SynchronizedSummaryStatistics>()
    }

    val summaries = HashMap<Int, DoubleSummaryStatistics>()

    override val size: Int get() = fragments.size

    override fun containsKey(key: Int): Boolean {
        return fragments.containsKey(key)
    }

    override fun containsValue(value: ai.platon.pulsar.dom.DocumentFragment): Boolean {
        return fragments.containsValue(value)
    }

    override operator fun get(key: Int): ai.platon.pulsar.dom.DocumentFragment? {
        return fragments[key]
    }

    override fun isEmpty(): Boolean {
        return fragments.isEmpty()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<Int, ai.platon.pulsar.dom.DocumentFragment>>
        get() = fragments.entries

    override val keys: MutableSet<Int>
        get() = fragments.keys

    override val values: MutableCollection<ai.platon.pulsar.dom.DocumentFragment>
        get() = fragments.values

    override fun put(key: Int, value: ai.platon.pulsar.dom.DocumentFragment): ai.platon.pulsar.dom.DocumentFragment? {
        return fragments.put(key, accept(value))
    }

    override fun putAll(from: Map<out Int, ai.platon.pulsar.dom.DocumentFragment>) {
        from.values.forEach { accept(it) }
        fragments.putAll(from)
    }

    override fun remove(key: Int): ai.platon.pulsar.dom.DocumentFragment? {
        val old = fragments.remove(key)
        return old
    }

    override fun clear() {
        fragments.clear()
    }

    private fun accept(fragment: ai.platon.pulsar.dom.DocumentFragment): ai.platon.pulsar.dom.DocumentFragment {
        fragment.fragments = this
        summaries.computeIfAbsent(SEQ) { DoubleSummaryStatistics() }
                .accept(fragment.element.features[SEQ])
        ai.platon.pulsar.dom.DocumentFragments.Companion.globalSummaries.computeIfAbsent(SEQ) { SynchronizedSummaryStatistics() }
                .addValue(fragment.element.features[SEQ])
        return fragment
    }
}
