package ai.platon.pulsar.dom

import ai.platon.pulsar.common.FuzzyProbability
import ai.platon.pulsar.dom.data.BlockLabel
import ai.platon.pulsar.dom.data.BlockLabelTracker
import ai.platon.pulsar.dom.data.BlockPattern
import ai.platon.pulsar.dom.nodes.node.ext.*
import org.apache.commons.codec.digest.DigestUtils
import org.jsoup.nodes.Element
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class DocumentFragment(
        val element: Element = Element("div"),
        var fragments: DocumentFragments = DocumentFragments.EMPTY
) {
    var parent: DocumentFragment? = null
    val children = LinkedList<DocumentFragment>()

    var defaultPassmark = FuzzyProbability.MAYBE
    private val requirePatterns = AtomicBoolean(true)
    var primaryLabel: String = ""
    var labels = HashSet<String>()
        private set
    val patterns = HashSet<String>()
    val clues = BlockLabelTracker()
    var definedSelector: String = ""
    var definedName: String = ""
    val keywords = TreeMap<String, Int>()
    val phrases = TreeSet<String>()

    init {
        element.getLabels().forEach { addLabel(it) }
    }

    val baseUri: String
        get() = element.baseUri()

    val elementName: String
        get() = element.uniqueName

    val name: String
        get() = if (definedName.isNotEmpty()) definedName else element.uniqueName

    val baseSequence: Int
        get() = element.sequence

    val textDigest: String
        get() = DigestUtils.md5Hex(text)

    val outerHtml: String
        get() = element.outerHtml()

    val html: String
        get() = element.html()

    val text: String
        get() = element.text()

    val natureSelector: String
        get() = element.cssSelector()

    val selector: String
        get() = if (definedSelector.isNotEmpty()) definedSelector else natureSelector

    fun hasParent(): Boolean {
        return this.parent != null
    }

    fun hasChild(): Boolean {
        return children.isNotEmpty()
    }

    /**
     * append the specified segment to the children list, make it's parent be this segment
     */
    fun appendChild(child: DocumentFragment) {
        this.children.add(child)
        child.parent = this
    }

    /**
     * remove this fragment and all child fragments from the tree, make it's parent be null
     */
    fun remove() {
        parent?.removeChild(this)
        this.children.forEach { it.parent = this.parent }
    }

    /**
     * remove the specified segment from the children, make it's parent be null
     */
    fun removeChild(child: DocumentFragment) {
        child.parent = null
        this.children.remove(child)
    }

    fun addLabel(label: String) {
        if (label.isEmpty()) return

        labels.add(label)
        if (BlockPattern(label).isBuiltin) {
            patterns.add(label)
        }
    }

    fun addLabels(labels: Iterable<String>) {
        labels.forEach { addLabel(it) }
    }

    fun hasLabel(label: String): Boolean {
        return labels.contains(label)
    }

    fun removeLabel(label: String) {
        element.removeLabel(label)
        labels.remove(label)
        patterns.remove(label)
    }

    fun clearLabels() {
        element.clearLabels()
        labels.clear()
        patterns.clear()
    }

    fun initPatterns() {
        if (requirePatterns.getAndSet(false)) {
            BlockPattern.patterns.filter { it.matches(element) }.forEach { addLabel(it.text) }
        }
    }

    // 打出及格分，使用自定义及格线
    fun markPass(label: BlockLabel, passmark: FuzzyProbability = defaultPassmark): Double {
        // 如果已经及格，则直接返回当前分数
        return if (matches(label, passmark)) {
            clues.get(label)
        } else clues.inc(label, passmark.floor())

        // 如果没有及格，则打出及格分
    }

    // 1分制
    fun grade(label: BlockLabel, score: Double): Double {
        return clues.inc(label, score)
    }

    // 10分制
    fun grade10(label: BlockLabel, score: Double): Double {
        return grade(label, score / 10.0)
    }

    fun matches(label: BlockLabel, p: FuzzyProbability = FuzzyProbability.VERY_LIKELY): Boolean {
        return clues.`is`(label, p)
    }

    fun matches(pattern: BlockPattern, p: FuzzyProbability = FuzzyProbability.VERY_LIKELY): Boolean {
        return clues.`is`(pattern, p)
    }

    override fun toString(): String {
        return element.name
    }

    override fun hashCode(): Int {
        return element.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is DocumentFragment && element == other.element
    }
}
