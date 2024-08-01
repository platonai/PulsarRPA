package ai.platon.pulsar.skeleton.crawl.scoring

import ai.platon.pulsar.common.ScoreEntry
import ai.platon.pulsar.common.ScoreVector

/**
 * All available parameters to calculate a page's score, eg, score to evaluate the most important N pages.
 * We use enum ordinal as the priority for simplification (smaller ordinal means higher priority).
 * To change one parameter's priority, just change it's order in the following enum definition.
 */
enum class Name {
    priority,  // bigger better
    distance,  // smaller better
    createTime,  // bigger better, limited
    contentScore,  // bigger better
    webGraphScore,  // bigger better
    refFetchErrDensity,  // smaller better
    refParseErrDensity,  // smaller better
    refExtractErrDensity,  // smaller better
    refIndexErrDensity,  // smaller better
    modifyTime,  // bigger better
    anchorOrder  // smaller better
}

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class NamedScoreVector(entries: List<ScoreEntry>) : ScoreVector(entries.size, entries) {
    constructor(): this(createSortedScoreEntries())

    operator fun get(name: Name): ScoreEntry {
        return get(name.ordinal)
    }

    fun setValue(name: Name, value: Int) {
        get(name).value = value
    }

    fun setValue(name: Name, value: Long) {
        get(name).value = value.toInt()
    }

    fun setValue(name: Name, value: Float) {
        setValue(name, value.toInt())
    }

    companion object {
        val PRIORITY = createScoreEntry(Name.priority)
        val DISTANCE = createScoreEntry(Name.distance)
        val CREATE_TIME = createScoreEntry(Name.createTime)
        val CONTENT_SCORE = createScoreEntry(Name.contentScore)
        val WEB_GRAPH_SCORE = createScoreEntry(Name.webGraphScore)
        val REF_FETCH_ERROR_DENSITY = createScoreEntry(Name.refFetchErrDensity)
        val REF_PARSE_ERROR_DENSITY = createScoreEntry(Name.refParseErrDensity)
        val REF_EXTRACT_ERROR_DENSITY = createScoreEntry(Name.refExtractErrDensity)
        val REF_INDEX_ERROR_DENSITY = createScoreEntry(Name.refIndexErrDensity)
        val MODIFY_TIME = createScoreEntry(Name.modifyTime)
        val ANCHOR_ORDER = createScoreEntry(Name.anchorOrder)
        val DEFAULT_SCORE_ENTRIES = arrayOf(
                PRIORITY,
                DISTANCE,
                CREATE_TIME,
                CONTENT_SCORE,
                WEB_GRAPH_SCORE,
                REF_FETCH_ERROR_DENSITY,
                REF_PARSE_ERROR_DENSITY,
                REF_EXTRACT_ERROR_DENSITY,
                REF_INDEX_ERROR_DENSITY,
                MODIFY_TIME,
                ANCHOR_ORDER)

        val ZERO = NamedScoreVector(createSortedScoreEntries(0))
        val ONE = NamedScoreVector(createSortedScoreEntries(1))

        fun createSortedScoreEntries(): List<ScoreEntry> {
            return createSortedScoreEntries(0)
        }

        fun createSortedScoreEntries(defaultValue: Int): List<ScoreEntry> {
            return DEFAULT_SCORE_ENTRIES.map { it.clone().also { it.value = defaultValue } }.sortedBy { it.priority }
        }

        /**
         * Enum ordinal is the priority in reversed order
         */
        fun createScoreEntry(name: Name): ScoreEntry {
            return ScoreEntry(name.name, name.ordinal)
        }

        fun createScoreEntry(name: Name, value: Int): ScoreEntry {
            return ScoreEntry(name.name, name.ordinal, value)
        }

        fun createScoreEntry(name: Name, value: Int, digits: Int): ScoreEntry {
            return ScoreEntry(name.name, name.ordinal, value, digits)
        }
    }
}
