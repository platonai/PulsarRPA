package `fun`.platonic.pulsar.dom.features

import `fun`.platonic.pulsar.common.math.vectors.get
import `fun`.platonic.pulsar.dom.features.NodeFeature.Companion.featureNames
import `fun`.platonic.pulsar.dom.features.NodeFeature.Companion.featureNamesToKeys
import `fun`.platonic.pulsar.dom.features.NodeFeature.Companion.floatFeatureNames
import `fun`.platonic.pulsar.dom.features.NodeFeature.Companion.floatFeatureNamesToKeys
import `fun`.platonic.pulsar.dom.nodes.node.ext.getFeature
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
        val currentKey = keyGen.get()
        /**
         * The key must start with 0, it might be an array index later
         * */
        val incKey: Int get() = keyGen.getAndIncrement()

        /*
         * affect _sep
         * can be modified in configuration
         * */
        val SEPARATORS = arrayOf(":", "：")

        val registeredFeatures = mutableSetOf<NodeFeature>()

        val primaryFeatures get() = registeredFeatures.filter { it.isPrimary }
        val floatFeatures get() = registeredFeatures.filter { it.isFloat }

        val featureKeys get() = registeredFeatures.map { it.key }
        val primaryFeatureKeys get() = primaryFeatures.map { it.key }
        val floatFeatureKeys get() = floatFeatures.map { it.key }

        val featureNames get() = registeredFeatures.map { it.name }
        val primaryFeatureNames get() = primaryFeatures.map { it.name }
        val floatFeatureNames get() = floatFeatures.map { it.name }

        val featureNamesToKeys get() = registeredFeatures.associateBy({ it.name }, { it.key })
        val featureKeysToNames get() = registeredFeatures.associateBy({ it.key }, { it.name })

        val primaryFeatureNamesToKeys get() = primaryFeatures.associateBy({ it.name }, { it.key })
        val primaryFeatureKeysToNames get() = primaryFeatures.associateBy({ it.key }, { it.name })

        val floatFeatureNamesToKeys get() = floatFeatures.associateBy({ it.name }, { it.key })
        val floatFeatureKeysToNames get() = floatFeatures.associateBy({ it.key }, { it.name })

        fun clear() {
            registeredFeatures.clear()
        }

        fun register(feature: NodeFeature) {
            registeredFeatures.add(feature)
        }

        fun register(features: Iterable<NodeFeature>) {
            registeredFeatures.addAll(features)
        }

        fun getKey(name: String): Int {
            return featureNamesToKeys[name.toLowerCase()]?:
                throw IllegalArgumentException("Unknown feature name $name")
        }

        fun getValue(name: String, node: Node): Double {
            return node.getFeature(name)
        }
    }
}

object FeatureFormatter {

    fun getKey(name: String): Int {
        return featureNamesToKeys[name.toLowerCase()]?:throw IllegalArgumentException("Unknown feature name $name")
    }

    fun getValue(name: String, node: Node): Double {
        return node.getFeature(name)
    }

    fun isFloating(name: String): Boolean {
        return floatFeatureNames.contains(name)
    }

    fun isFloating(key: Int): Boolean {
        return floatFeatureNamesToKeys.containsValue(key)
    }

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
