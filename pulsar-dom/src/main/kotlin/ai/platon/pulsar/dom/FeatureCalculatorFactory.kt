package ai.platon.pulsar.dom

import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.dom.features.FeatureCalculator
import ai.platon.pulsar.dom.features.Level1FeatureCalculator
import org.slf4j.LoggerFactory

class FeatureCalculatorFactory {
    val activeCalculator get() = createIfAbsent()

    companion object {
        private val log = LoggerFactory.getLogger(FeatureCalculatorFactory::class.java)

        @Volatile
        private var calculatorRef: FeatureCalculator? = null

        private fun createIfAbsent(): FeatureCalculator {
            if (calculatorRef == null) {
                val defaultClazz = Level1FeatureCalculator::class.java
                val className = System.getProperty(CapabilityTypes.NODE_FEATURE_CALCULATOR_CLASS)

                val clazz: Class<out FeatureCalculator> = try {
                    defaultClazz.takeIf { className == null } ?: ResourceLoader.loadUserClass(className)
                } catch (e: Exception) {
                    defaultClazz.also { logFailure(className, defaultClazz) }
                }

                val calculator = clazz.constructors
                        .first { it.parameters.isEmpty() }.newInstance() as FeatureCalculator
                calculatorRef = calculator
            }

            return calculatorRef!!
        }

        private fun logFailure(className: String, defaultClazz: Class<*>) {
            log.warn("Configured calculator <{}: {}> is not found, fallback to default ({})",
                    CapabilityTypes.NODE_FEATURE_CALCULATOR_CLASS, className,
                    defaultClazz.simpleName)
        }
    }
}
