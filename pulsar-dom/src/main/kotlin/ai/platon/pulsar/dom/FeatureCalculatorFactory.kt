package ai.platon.pulsar.dom

import ai.platon.pulsar.dom.features.FeatureCalculator
import ai.platon.pulsar.dom.features.Level1FeatureCalculator

object FeatureCalculatorFactory {
    var calculator: FeatureCalculator = Level1FeatureCalculator()
}
