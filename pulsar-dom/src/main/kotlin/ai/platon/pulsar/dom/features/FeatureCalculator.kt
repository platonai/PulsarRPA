package ai.platon.pulsar.dom.features

import org.jsoup.nodes.Document

interface FeatureCalculator {
    fun calculate(document: Document)
}

abstract class AbstractFeatureCalculator: FeatureCalculator {
    override fun calculate(document: Document) {}
}

class CombinedFeatureCalculator(
        val calculators: MutableList<FeatureCalculator> = mutableListOf()
): AbstractFeatureCalculator() {

    constructor(vararg calculator: FeatureCalculator): this() {
        calculators.addAll(calculator)
    }

    override fun calculate(document: Document) {
        calculators.forEach { it.calculate(document) }
    }
}
