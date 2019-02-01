package ai.platon.pulsar.dom.features

import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.dom.features.NodeFeature.Companion.featureNames
import ai.platon.pulsar.dom.features.NodeFeature.Companion.isFloating
import ai.platon.pulsar.dom.nodes.node.ext.getFeature
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Node
import java.util.concurrent.atomic.AtomicInteger

data class FeatureEntry(val key: Int, val value: Double)

data class NodeFeature(val key: Int, val name: String, val isPrimary: Boolean = true, val isFloat: Boolean = false) {

    var value: Double = 0.0

    val toEntry = FeatureEntry(key, value)

    companion object {

        private val keyGen = AtomicInteger(0)
        /**
         * The key should return by incKey
         * */
        val currentKey get() = keyGen.get()
        /**
         * The key must start with 0, it might be an array index later
         * */
        val incKey: Int get() = keyGen.getAndIncrement()

        /*
         * affect _sep
         * can be modified in configuration
         * */
        val SEPARATORS = arrayOf(":", "：")

        var registeredFeatures: Set<NodeFeature> = setOf()
            private set
        val dimension get() = registeredFeatures.size

        var primaryFeatures: List<NodeFeature> = listOf()
            private set
        var floatFeatures: List<NodeFeature> = listOf()
            private set

        var featureKeys: List<Int> = listOf()
            private set
        var primaryFeatureKeys: List<Int> = listOf()
            private set
        var floatFeatureKeys: List<Int> = listOf()
            private set

        var featureNames: List<String> = listOf()
            private set
        var primaryFeatureNames: List<String> = listOf()
            private set
        var floatFeatureNames: List<String> = listOf()
            private set

        var featureNamesToKeys: Map<String, Int> = mapOf()
            private set
        var featureKeysToNames: Map<Int, String> = mapOf()
            private set

        var primaryFeatureNamesToKeys: Map<String, Int> = mapOf()
            private set
        var primaryFeatureKeysToNames : Map<Int, String> = mapOf()
            private set

        var floatFeatureNamesToKeys: Map<String, Int> = mapOf()
            private set
        var floatFeatureKeysToNames: Map<Int, String> = mapOf()
            private set

        fun register(features: Iterable<NodeFeature>) {
            registeredFeatures = features.toSet()

            primaryFeatures = registeredFeatures.filter { it.isPrimary }
            floatFeatures = registeredFeatures.filter { it.isFloat }

            featureKeys = registeredFeatures.map { it.key }
            primaryFeatureKeys = primaryFeatures.map { it.key }
            floatFeatureKeys = floatFeatures.map { it.key }

            featureNames = registeredFeatures.map { it.name }
            primaryFeatureNames = primaryFeatures.map { it.name }
            floatFeatureNames = floatFeatures.map { it.name }

            featureNamesToKeys = registeredFeatures.associateBy({ it.name }, { it.key })
            featureKeysToNames = registeredFeatures.associateBy({ it.key }, { it.name })

            primaryFeatureNamesToKeys = primaryFeatures.associateBy({ it.name }, { it.key })
            primaryFeatureKeysToNames = primaryFeatures.associateBy({ it.key }, { it.name })

            floatFeatureNamesToKeys = floatFeatures.associateBy({ it.name }, { it.key })
            floatFeatureKeysToNames = floatFeatures.associateBy({ it.key }, { it.name })
        }

        fun getKey(name: String): Int {
            return featureNamesToKeys[name.toLowerCase()]?:
                throw IllegalArgumentException("Unknown feature name $name")
        }

        fun getValue(name: String, node: Node): Double {
            return node.getFeature(name)
        }
        
        fun isFloating(name: String): Boolean {
            return floatFeatureNames.contains(name)
        }

        fun isFloating(key: Int): Boolean {
            return floatFeatureKeys.contains(key)
        }
    }
}

object FeatureFormatter {

    /**
     * Get the string representation of this feature
     *
     * @return string
     */
    fun format(feature: FeatureEntry): String {
        val key = feature.key
        return key.toString() + ":" + formatValue(key, feature.value)
    }

    fun format(features: RealVector, vararg featureKeys: Int, sb: StringBuilder = StringBuilder()): StringBuilder {
        if (featureKeys.isEmpty()) {
            for (i in featureNames.indices) {
                val value = features[i]
                if (value != 0.0) {
                    sb.append(featureNames[i]).append(":").append(formatValue(i, value)).append(' ')
                }
            }
        } else {
            for (i in featureKeys) {
                val value = features.getEntry(i)
                if (value != 0.0) {
                    sb.append(featureNames[i]).append(":").append(formatValue(i, value)).append(' ')
                }
            }
        }

        return if (sb.endsWith(' ')) sb.deleteCharAt(sb.length - 1) else sb
    }

    fun format(variables: Map<String, Any>, sb: StringBuilder = StringBuilder()): StringBuilder {
        return format(variables, listOf(), sb)
    }

    fun format(variables: Map<String, Any>, names: Collection<String>, sb: StringBuilder = StringBuilder()): StringBuilder {
        variables.forEach { (name, value) ->
            if (names.isEmpty() || name in names) {
                val s = if (value is Double) formatValue(value) else value.toString()
                sb.append(name).append(':').append(s).append(' ')
            }
        }
        return if (sb.endsWith(' ')) sb.deleteCharAt(sb.length - 1) else sb
    }

    fun formatValue(key: Int, value: Double): String {
        return formatValue(value, isFloating(key))
    }

    fun formatValue(value: Double, isFloating: Boolean = true): String {
        return if (isFloating) {
            String.format("%.2f", value)
        } else {
            String.format("%d", value.toInt())
        }
    }
}
