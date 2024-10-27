
package ai.platon.pulsar.skeleton.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.skeleton.crawl.index.IndexDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import java.util.*

class ScoringFilters(scoringFilters: List<ScoringFilter> = emptyList(), val conf: ImmutableConfig) : ScoringFilter {
    private val scoringFilters = ArrayList<ScoringFilter>()

    init {
        this.scoringFilters.addAll(scoringFilters)
    }

    constructor(conf: ImmutableConfig) : this(emptyList(), conf)

    override fun getParams(): Params {
        return Params().merge(scoringFilters.map { it.params })
    }

    /**
     * Calculate a new initial score, used when adding newly discovered pages.
     */
    override fun initialScore(page: WebPage) {
        scoringFilters.forEach { it.initialScore(page) }
    }

    /**
     * Calculate a new initial score, used when injecting new pages.
     */
    override fun injectedScore(page: WebPage) {
        scoringFilters.forEach { it.injectedScore(page) }
    }

    /**
     * Calculate a sort value for Generate.
     */
    override fun generatorSortValue(page: WebPage, initSort: ScoreVector): ScoreVector {
        var score = initSort

        scoringFilters.forEach {
            score = it.generatorSortValue(page, score)
        }

        return score
    }

    override fun distributeScoreToOutlinks(page: WebPage, graph: WebGraph, outgoingEdges: Collection<WebEdge>, allCount: Int) {
        scoringFilters.forEach {
            it.distributeScoreToOutlinks(page, graph, outgoingEdges, allCount)
        }
    }

    override fun updateScore(page: WebPage, graph: WebGraph, incomingEdges: Collection<WebEdge>) {
        scoringFilters.forEach {
            it.updateScore(page, graph, incomingEdges)
        }
    }

    override fun updateContentScore(page: WebPage) {
        for (filter in scoringFilters) {
            filter.updateContentScore(page)
        }
    }

    override fun indexerScore(url: String, doc: IndexDocument, page: WebPage, initScore: Float): Float {
        var score = initScore
        scoringFilters.forEach {
            score = it.indexerScore(url, doc, page, score)
        }
        return score
    }

    override fun toString(): String {
        return scoringFilters.joinToString { it.javaClass.simpleName }
    }
}
