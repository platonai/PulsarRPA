package ai.platon.pulsar.common

import com.google.common.collect.Multiset
import com.google.common.collect.TreeMultiset
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*

class SingleFiledLines(
        val file: String = "",
        val preprocessor: Preprocessor = TextPreprocessor(),
        val wordsComparator: Comparator<String> = kotlin.Comparator { t, t2 -> t.compareTo(t2) }
) {
    private val lines = TreeMultiset.create<String>()

    /**
     * Load features from file. Nothing to do if the file does not exist,
     * and the there is no features available.
     *
     *
     * Null file name means load nothing
     */
    init {
        load()
    }

    fun merge(other: SingleFiledLines) {
        lines.addAll(other.lines())
    }

    operator fun contains(text: String): Boolean {
        return lines.contains(text)
    }

    fun lines(): Multiset<String> {
        return lines
    }

    val size: Int
        get() = lines.size

    val isEmpty: Boolean
        get() = lines.isEmpty()

    val isNotEmpty: Boolean
        get() = lines.isNotEmpty()

    /**
     * Load features from file. Nothing to do if the file does not exist
     */
    fun load() {
        if (file.isBlank()) {
            return
        }

        ResourceLoader().readAllLines(file)
                .map { preprocessor.process(it) }
                .filter { it.isNotBlank() }
                .toCollection(lines)
    }

    fun saveTo(destFile: String) {
        PrintWriter(FileWriter(destFile)).use { pw ->
            lines.forEach { line -> pw.println(line) }
        }
    }

    fun save() {
        if (!lines.isEmpty()) {
            saveTo(file)
        }
    }

    interface Preprocessor {
        fun process(line: String): String
    }

    class TextPreprocessor : Preprocessor {
        override fun process(line: String): String {
            var line = line
            line = if (line.startsWith("#")) "" else line.trim()
            return line
        }
    }

    class RegexPreprocessor : Preprocessor {
        override fun process(line: String): String {
            var line = line
            line = if (line.startsWith("#")) "" else line.trim()
            return line
        }
    }
}
