package ai.platon.pulsar.dom

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.dom.features.FeatureCalculator
import ai.platon.pulsar.dom.features.Level1FeatureCalculator
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class FeatureCalculatorFactory {
    private val log = LoggerFactory.getLogger(FeatureCalculatorFactory::class.java)

    private val calculatorRef = AtomicReference<FeatureCalculator>()
    val activeCalculator get() = createIfAbsent()

    private fun createIfAbsent(): FeatureCalculator {
        if (calculatorRef.get() == null) {
            synchronized(FeatureCalculatorFactory::class) {
                if (calculatorRef.get() == null) {
                    val defaultClazz = Level1FeatureCalculator::class.java
                    val className = System.getProperty(CapabilityTypes.NODE_FEATURE_CALCULATOR_CLASS)
                    val clazz: Class<out FeatureCalculator> = try {
                        ResourceLoader.loadUserClass(className)
                    } catch (e: Exception) {
                        log.warn("Configured calculator <{}: {}> is not found, fallback to default ({})",
                                CapabilityTypes.NODE_FEATURE_CALCULATOR_CLASS, System.getProperty(className),
                                defaultClazz.simpleName)
                        defaultClazz
                    }

                    calculatorRef.set(clazz.constructors.first { it.parameters.isEmpty() }.newInstance() as FeatureCalculator)
                }
            }
        }
        return calculatorRef.get()
    }
}
