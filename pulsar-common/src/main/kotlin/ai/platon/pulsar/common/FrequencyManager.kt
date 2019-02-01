package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.PulsarConstants.PULSAR_DEFAULT_TMP_DIR
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A set of term counters
 */
class FrequencyManager<T : Comparable<T>> : MutableMap<String, Frequency<T>> {

    private val counters = mutableMapOf<String, Frequency<T>>()

    fun computeIfAbsent(name: String): Frequency<T> {
        return counters.computeIfAbsent(name) { Frequency(it) }
    }

    fun count(name: String, term: T): Int {
        val counter = counters[name]
        return if (counter != null) {
            counter.count(term)
        } else 0

    }

    override val size: Int get() = counters.size

    override val keys: MutableSet<String> get() = counters.keys

    override val values: MutableCollection<Frequency<T>> get() = counters.values

    override val entries: MutableSet<MutableMap.MutableEntry<String, Frequency<T>>> get() = counters.entries

    override fun isEmpty(): Boolean {
        return counters.isEmpty()
    }

    override fun containsKey(key: String): Boolean {
        return counters.containsKey(key)
    }

    override fun containsValue(value: Frequency<T>): Boolean {
        return counters.containsValue(value)
    }

    override operator fun get(key: String): Frequency<T>? {
        return counters.get(key)
    }

    override fun put(key: String, value: Frequency<T>): Frequency<T>? {
        return counters.put(key, value)
    }

    override fun remove(key: String): Frequency<T>? {
        return counters.remove(key)
    }

    override fun putAll(m: Map<out String, Frequency<T>>) {
        counters.putAll(m)
    }

    override fun clear() {
        counters.clear()
    }

    /**
     * Remove items appears more then {@param threshold} times
     *
     * @param appearance The appearance
     */
    fun removeMoreThen(appearance: Double): Int {
        var removed = 0

        for (counter in counters.values) {
            removed += counter.trimEnd(appearance)
        }

        if (removed > 0) {
            LOG.debug("Removed $removed items more then $appearance appearance")
        }

        return removed
    }

    /**
     * Remove items appears more then {@param threshold} times
     *
     * @param appearance The appearance
     */
    fun removeLessThen(appearance: Double): Int {
        var removed = 0

        for (counter in counters.values) {
            removed += counter.trimStart(appearance)
        }

        if (removed > 0) {
            LOG.debug("Removed $removed items more then $appearance appearance")
        }

        return removed
    }

    fun saveTo(directory: Path) {
        val metadata = Paths.get(directory.toString(), ".metadata")
        Files.createDirectories(metadata.parent)

        for (counter in counters.values) {
            val destFile = Paths.get(directory.toString(), counter.name)
            counter.exportTo(destFile)
        }
    }

    fun save() {
        saveTo(PULSAR_DEFAULT_TMP_DIR)
    }

    fun load() {
        // TODO : not implemented
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (counter in counters.values) {
            sb.append(counter.name).append('\n').append(counter.toString()).append('\n')
        }
        return sb.toString()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(FrequencyManager::class.java)!!
    }
}
