
package ai.platon.pulsar.skeleton.crawl.scoring

import ai.platon.pulsar.common.ScoreVector
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.skeleton.crawl.index.IndexDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.graph.WebEdge
import ai.platon.pulsar.persist.graph.WebGraph
import org.slf4j.LoggerFactory

interface ScoringFilter : Parameterized {

    fun injectedScore(page: WebPage) {}

    fun initialScore(page: WebPage) {}

    fun generatorSortValue(page: WebPage, initSort: ScoreVector): ScoreVector {
        return ScoreVector.ZERO
    }

    fun distributeScoreToOutlinks(page: WebPage, graph: WebGraph, outgoingEdges: Collection<WebEdge>, allCount: Int) {}

    fun updateScore(page: WebPage, graph: WebGraph, incomingEdges: Collection<WebEdge>) {}

    fun updateContentScore(page: WebPage) {}

    fun indexerScore(url: String, doc: IndexDocument, page: WebPage, initScore: Float): Float {
        return 0.0f
    }
}