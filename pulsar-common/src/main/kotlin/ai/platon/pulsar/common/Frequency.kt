package ai.platon.pulsar.common

import com.google.common.collect.ConcurrentHashMultiset
import com.google.common.collect.HashMultiset
import com.google.common.collect.Multiset.Entry
import org.apache.commons.collections4.CollectionUtils
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Maintains a frequency distribution.
 *
 * @see {https://en.wikipedia.org/wiki/Tf-idf}
 * @see {http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/stat/Frequency.html}
 */
class Frequency<T : Comparable<T>>(val name: String = "#F$nextId"): MutableCollection<T> {
    /**
     * The underlying term counter
     * */
    private val counter = ConcurrentHashMultiset.create<T>()
    /**
     * The unique elements count
     * */
    override val size: Int get() = entrySet().size
    /**
     * Total elements being added, i.e. the sum of all frequencies
     */
    val totalFrequency: Int get() = counter.size
    /**
     * The entry with the most frequency
     * */
    val mostEntry: Entry<T> get() = entrySet().maxByOrNull { it.count }?:throw NoSuchElementException("Collection is empty.")
    /**
     * The entry with the least frequency
     * */
    val leastEntry: Entry<T> get() = entrySet().minByOrNull { it.count }?:throw NoSuchElementException("Collection is empty.")
    /**
     * The mode value
     * The mode of a sample is the element that occurs most often in the collection.
     * */
    val mode: T get() = mostEntry.element
    /**
     * The mode values, a list containing the value(s) which appear most often.
     * The mode of a sample is the element that occurs most often in the collection.
     * */
    val modes: List<T> get() = entrySet().sortedByDescending { it.count }.map { it.element }
    /**
     * The mode value
     * The mode of a sample is the element that occurs most often in the collection.
     * */
    val modePercentage: Double get() = mostEntry.count.toDouble() / totalFrequency

    override fun add(element: T): Boolean {
        return counter.add(element)
    }

    override fun remove(element: T): Boolean {
        return counter.remove(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return counter.containsAll(elements)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        return counter.addAll(elements)
    }

    fun addAll(elements: Array<T>) {
        elements.forEach { counter.add(it) }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        return counter.removeAll(elements)
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        return counter.retainAll(elements)
    }

    override fun clear() {
        counter.clear()
    }

    override fun isEmpty(): Boolean {
        return counter.isEmpty()
    }

    override operator fun contains(element: T): Boolean {
        return counter.contains(element)
    }

    fun count(element: T): Int {
        return counter.count(element)
    }

    fun entrySet(): Set<Entry<T>> {
        return counter.entrySet()
    }

    fun elementSet(): Set<T> {
        return counter.elementSet()
    }

    override fun iterator(): MutableIterator<T> {
        return counter.iterator()
    }

    /**
     * The ordinal for each element in the element set.
     * */
    fun ordinalMap(): Map<T, Int> {
        val map = LinkedHashMap<T, Int>()
        elementSet().forEachIndexed { i, e -> map[e] = i }
        return map
    }

    /**
     * Returns the percentage of values that are equal to v
     * (as a proportion between 0 and 1).
     *
     * Returns `Double.NaN` if no values have been added.
     *
     * @param v the value to lookup
     * @return the proportion of values equal to v
     */
    fun percentageOf(v: T): Double {
        return if (totalFrequency == 0) {
            Double.NaN
        } else count(v) / totalFrequency.toDouble()
    }

    /**
     * Returns the cumulative percentage of values less than or equal to v
     * (as a proportion between 0 and 1).
     *
     * Returns `Double.NaN` if no values have been added.
     * Returns 0 if at least one value has been added, but v is not comparable
     * to the values set.
     *
     * @param v the value to lookup
     * @return the proportion of values less than or equal to v
     */
    fun cumulativePercentageOf(v: T): Double {
        return if (totalFrequency == 0) {
            Double.NaN
        } else cumulativeFrequencyOf(v).toDouble() / totalFrequency
    }

    /**
     * Returns the cumulative frequency of values less than or equal to v.
     *
     * Cumulative frequency analysis is the analysis of the frequency of occurrence
     * of values of a phenomenon less than a reference value.
     * The phenomenon may be time- or space-dependent. Cumulative frequency is also
     * called frequency of non-exceedance.
     *
     * @param v the value to lookup.
     * @return the proportion of values equal to v
     */
    fun cumulativeFrequencyOf(v: T): Int {
        if (totalFrequency == 0) {
            return 0
        }

        // TODO: Can we avoid the new TreeSet?
        val elements = TreeSet(elementSet())
        if (v < elements.first()) {
            return 0 // less than first value
        }

        if (v >= elements.last()) {
            return totalFrequency // greater than or equal to last value
        }

        var freq = 0
        for (ele in elements) {
            if (ele <= v) {
                freq += count(ele)
            } else {
                return freq
            }
        }
        return freq
    }

    /**
     * Remove elements that are more than n, where
     * n = [freqThreshold] if [freqThreshold] > 1 or n = [freqThreshold] * [size] if [freqThreshold] < 1
     * */
    fun trimEnd(freqThreshold: Double): Int {
        var a = freqThreshold
        if (a <= 0) return 0
        if (a < 1) {
            a *= size.toDouble()
        }

        val removal = HashSet<T>()
        for (entry in counter.entrySet()) {
            if (entry.count > a) {
                removal.add(entry.element)
            }
        }

        counter.removeAll(removal)

        return removal.size
    }

    /**
     * Remove elements that are more than n, where
     * n = [freqThreshold] if [freqThreshold] > 1 or n = [freqThreshold] * [size] if [freqThreshold] < 1
     * */
    fun trimStart(freqThreshold: Double): Int {
        var a = freqThreshold
        if (a <= 0) {
            return 0
        }
        if (a < 1) {
            a *= size.toDouble()
        }

        val removal = HashSet<T>()
        for (entry in counter.entrySet()) {
            if (entry.count < a) {
                removal.add(entry.element)
            }
        }
        counter.removeAll(removal)

        return removal.size
    }

    fun exportTo(path: Path) {
        val pw = PrintWriter(FileWriter(path.toFile()))

        for (entry in counter.entrySet()) {
            pw.print(entry.count)
            pw.print('\t')
            pw.print(entry.element)
            pw.println()
        }

        pw.close()
    }

    @JvmOverloads
    fun toPString(prefix: String = "", postfix: String = "", delimiter: String = "\t"): String {
        return entrySet().joinTo(StringBuilder(), delimiter, prefix, postfix) {
            String.format("%s:%4.2f", it.element, 1.0 * it.count / totalFrequency)
        }.toString()
    }

    @JvmOverloads
    fun toReport(prefix: String = "", postfix: String = ""): String {
        val sb = StringBuilder(prefix)

        var maxLength = entrySet().map { it.element.toString().length }.maxOrNull()?:return ""
        maxLength += 2

        sb.append(String.format("%-10s%${maxLength}s%10s%10s%10s\n", "", "Value", "Freq", "Pct", "Cum Pct"))
        for ((i, e) in entrySet().withIndex()) {
            val value = e.element
            sb.append(String.format("%-10d%${maxLength}s%10s%10.2f%%%10.2f%%\n",
                    i + 1, value, e.count, 100 * percentageOf(value), 100 * cumulativePercentageOf(value)))
        }
        sb.append("totalFrequency: $totalFrequency\tmode: $mode")
        sb.append(postfix)

        return sb.toString()
    }

    /**
     * Return a string representation of this frequency distribution.
     *
     * @return a string representation.
     */
    override fun toString(): String {
        return entrySet().joinToString { "${it.element}: ${it.count}" }
    }

    override fun hashCode(): Int {
        return counter.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Frequency<*> && counter == other
    }

    companion object {
        private val idGenerator = AtomicInteger(0)
        private val nextId get() = idGenerator.incrementAndGet()
    }
}
