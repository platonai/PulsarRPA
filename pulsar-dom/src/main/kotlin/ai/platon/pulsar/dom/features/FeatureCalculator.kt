package ai.platon.pulsar.dom.features

import org.jsoup.nodes.Document

interface FeatureCalculator {
    fun calculate(document: Document)
}

abstract class AbstractFeatureCalculator: FeatureCalculator {
    override fun calculate(document: Document) {}
}
