package `fun`.platonic.pulsar.common

import com.google.common.collect.Multiset
import java.util.*
import java.util.regex.Pattern

data class BlackWhiteResourceConfig(
        val blackWordsFile: String = "",
        val whiteWordsFile: String = "",
        val blackRegexFile: String = "",
        val whiteRegexFile: String = ""
)

/**
 * Manage black-white list words, words are written in files line by line.
 * There are four kind of words :
 *
 *
 * 1. black word list
 * 2. white word list
 * 3. black regex list
 * 4. white regex list
 */
class BlackWhiteList(
        blackWordsFile: String = "",
        whiteWordsFile: String = "",
        blackRegexFile: String = "",
        whiteRegexFile: String = ""
) {
    enum class ListType {
        WhiteWords, BlackWords, WhiteRegex, BlackRegex
    }

    val words = EnumMap<ListType, SingleFiledLines>(ListType::class.java)

    val whitePatterns = HashSet<Pattern>()
    val blackPatterns = HashSet<Pattern>()

    constructor(conf: BlackWhiteResourceConfig)
            : this(conf.blackWordsFile, conf.whiteWordsFile, conf.blackRegexFile, conf.whiteRegexFile)

    /**
     * @param whiteWordsFile
     * @param blackWordsFile
     * @param whiteRegexFile
     * @param blackRegexFile
     */
    init {
        words[ListType.BlackWords] = SingleFiledLines(blackWordsFile)
        words[ListType.WhiteWords] = SingleFiledLines(whiteWordsFile)
        words[ListType.BlackRegex] = SingleFiledLines(blackRegexFile, SingleFiledLines.RegexPreprocessor())
        words[ListType.WhiteRegex] = SingleFiledLines(whiteRegexFile, SingleFiledLines.RegexPreprocessor())

        load()
    }

    val whiteWords: Multiset<String>
        get() {
            return words[ListType.WhiteWords]!!.lines()
        }

    val blackWords: Multiset<String>
        get() {
            return words[ListType.BlackWords]!!.lines()
        }

    fun merge(other: BlackWhiteList) {
        for (type in words.keys) {
            words[type]?.merge(other.words[type]!!)
        }

        rebuildPatterns()
    }

    fun validate(words: String): Boolean {
        return filter(words) != null
    }

    fun filter(words: String): String? {
        if (whiteWords.contains(words)) {
            return words
        }

        for (pattern in whitePatterns) {
            if (pattern.matcher(words).matches()) {
                return words
            }
        }

        if (blackWords.contains(words)) {
            return null
        }

        for (pattern in blackPatterns) {
            if (pattern.matcher(words).matches()) {
                return null
            }
        }

        return words
    }

    fun report(): String {
        val sb = StringBuilder()

        var i = 0
        for (type in words.keys) {
            if (i++ > 0) {
                sb.append(", ")
            }

            sb.append(type)
            sb.append(" : ")
            sb.append(words[type]!!.size)
        }

        return sb.toString()
    }

    fun load() {
        words.values.forEach { it.load() }
        rebuildPatterns()
    }

    private fun rebuildPatterns() {
        whitePatterns.clear()
        for (regex in words[ListType.WhiteRegex]!!.lines()) {
            whitePatterns.add(Pattern.compile(regex))
        }

        blackPatterns.clear()
        for (regex in words[ListType.BlackRegex]!!.lines()) {
            blackPatterns.add(Pattern.compile(regex))
        }
    }
}
