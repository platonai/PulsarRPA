package ai.platon.pulsar.skeleton.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.crawl.index.IndexDocument

interface ScoringFilter : Parameterized {

    fun injectedScore(page: WebPage) {}

    fun initialScore(page: WebPage) {}

    fun generatorSortValue(page: WebPage, initSort: ScoreVector): ScoreVector {
        return ScoreVector.ZERO
    }

    fun updateContentScore(page: WebPage) {}

    fun indexerScore(url: String, doc: IndexDocument, page: WebPage, initScore: Float): Float {
        return 0.0f
    }
}