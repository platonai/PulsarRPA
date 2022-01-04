package ai.platon.pulsar.dom.features

import org.jsoup.nodes.Document

/**
 * The feature calculator interface
 * */
interface FeatureCalculator {
    fun calculate(document: Document)
}

/**
 * The abstract feature calculator
 * */
abstract class AbstractFeatureCalculator: FeatureCalculator {
    override fun calculate(document: Document) {}
}

/**
 * The combined feature calculator
 * */
class CombinedFeatureCalculator(
        val calculators: MutableList<FeatureCalculator> = mutableListOf()
): AbstractFeatureCalculator() {

    constructor(vararg calculator: FeatureCalculator): this() {
        calculators.addAll(calculator)
    }

    /**
     * Calculate the features of the document
     * */
    override fun calculate(document: Document) {
        calculators.forEach { it.calculate(document) }
    }
}
