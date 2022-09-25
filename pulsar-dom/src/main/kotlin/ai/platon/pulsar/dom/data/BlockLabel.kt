package ai.platon.pulsar.dom.data

import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * The block label
 *
 * @param value The label
 * */
open class BlockLabel(value: String) : Comparable<BlockLabel> {

    val text: String = value.toLowerCase()

    open val isBuiltin: Boolean
        get() = builtinLabels.contains(this)

    val isInheritable: Boolean
        get() = inheritableLabels.contains(this)

    override fun hashCode(): Int {
        return text.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true;
        }

        return other is BlockLabel && text == other.text
    }

    override fun compareTo(other: BlockLabel): Int {
        return text.compareTo(other.text)
    }

    override fun toString(): String {
        return text
    }

    companion object {
        val UnknownBlock = BlockLabel("")
        val BadBlock = BlockLabel("BadBlock")
        val Metadata = BlockLabel("Metadata")
        val Title = BlockLabel("Title")
        val TitleContainer = BlockLabel("TitleContainer")
        val Menu = BlockLabel("Menu")
        val Areas = BlockLabel("Areas")
        val Categories = BlockLabel("Categories")
        val Gallery = BlockLabel("Gallery")
        val SimilarEntity = BlockLabel("SimilarEntity")

        val inheritableLabels: MutableSet<BlockLabel> = HashSet()
        val builtinLabels: MutableSet<BlockLabel> = HashSet()

        init {
            builtinLabels.add(UnknownBlock)
            builtinLabels.add(BadBlock)
            builtinLabels.add(Metadata)
            builtinLabels.add(Title)
            builtinLabels.add(TitleContainer)
            builtinLabels.add(Menu)
            builtinLabels.add(Areas)
            builtinLabels.add(Categories)
            builtinLabels.add(Gallery)
            builtinLabels.add(SimilarEntity)

            inheritableLabels.add(Areas)
        }

        fun mergeLabels(incoming: Collection<String>): Set<String> {
            return builtinLabels.map { it.text }.union(incoming)
        }
    }
}

class BlockClue(value: String): BlockLabel(value) {
    companion object {
        val MANUAL = BlockClue("MANUAL")
        val SELECTOR = BlockClue("SELECTOR")
        val VARIANCE = BlockClue("VARIANCE")
        val PATTERN = BlockClue("PATTERN")
        val LABEL = BlockClue("LABEL")
    }
}

class BlockLabelTracker : ai.platon.pulsar.common.FuzzyTracker<BlockLabel>() {

    val labels: List<String>
        get() {
            return keySet().map { it.text }
        }

    operator fun set(label: String, sim: Double) {
        super.set(BlockLabel(label), sim)
    }

    operator fun set(label: String, p: ai.platon.pulsar.common.FuzzyProbability) {
        set(label, p.floor())
    }

    fun filter(sim: Double): List<BlockLabel> {
        return keySet().filter { get(it) >= sim }
    }

    val asString: String
        get() = filterToString(ai.platon.pulsar.common.FuzzyProbability.UNSURE)

    fun filterToString(p: ai.platon.pulsar.common.FuzzyProbability): String {
        var labels = ""

        for (label in keySet()) {
            if (`is`(label, p)) {
                labels += label
                labels += StringUtils.SPACE
            }
        }

        return labels
    }
}
