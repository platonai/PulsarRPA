package ai.platon.pulsar.common

import com.google.common.collect.Multiset
import com.google.common.collect.TreeMultiset
import java.util.*

class MultipleFiledLines(
        var files: Array<String>,
        val preprocessor: SingleFiledLines.Preprocessor = SingleFiledLines.TextPreprocessor(),
        val wordsComparator: Comparator<String> = kotlin.Comparator { t, t2 -> t.compareTo(t2) }
) {
    val filedLines = HashMap<String, SingleFiledLines>()

    /**
     * Load features from file. Nothing to do if the file does not exist,
     * and the there is no features available.
     */
    init {
        load()
    }

    fun lines(file: String): Multiset<String> {
        return if (filedLines.isEmpty() || !filedLines.containsKey(file))
            TreeMultiset.create()
        else filedLines[file]!!.lines()
    }

    fun firstFileLines(): Multiset<String> {
        return if (filedLines.isEmpty()) TreeMultiset.create() else filedLines.values.iterator().next().lines()

    }

    fun add(file: String, text: String): Boolean {
        val ls = lines(file) ?: return false

        return ls.add(text)
    }

    fun addAll(file: String, texts: Collection<String>): Boolean {
        val ls = lines(file) ?: return false

        return ls.addAll(texts)
    }

    fun remove(file: String, text: String): Boolean {
        val ls = lines(file) ?: return false

        return ls.remove(text)
    }

    fun clear() {
        filedLines.clear()
    }

    fun contains(file: String, text: String): Boolean {
        val conf = lines(file)
        return conf.contains(text)

    }

    fun load() {
        for (file in files) {
            filedLines[file] = SingleFiledLines(file, preprocessor, wordsComparator)
        }
    }

    fun save(file: String) {
        filedLines[file]?.save()
    }

    fun saveAll() {
        for (file in filedLines.keys) {
            save(file)
        }
    }
}
