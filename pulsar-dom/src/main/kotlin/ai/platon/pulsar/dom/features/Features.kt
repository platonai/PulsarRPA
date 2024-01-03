package ai.platon.pulsar.dom.features

import ai.platon.pulsar.common.math.geometric.str
import ai.platon.pulsar.common.math.vectors.get
import ai.platon.pulsar.common.toHexString
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.V_OWNER_BODY
import ai.platon.pulsar.dom.nodes.node.ext.getFeature
import ai.platon.pulsar.dom.nodes.node.ext.name
import com.google.common.collect.Iterables
import org.apache.commons.lang3.StringUtils
import org.apache.commons.math3.linear.RealVector
import org.apache.commons.math3.util.Precision
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import java.awt.Color
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicInteger

/**
 * The feature entry
 * @param key The feature key
 * @param value The feature value
 * */
data class FeatureEntry(val key: Int, val value: Double) {
    constructor(key: Int, value: Int): this(key, value.toDouble())
    constructor(key: Int, value: Boolean): this(key, if (value) 1.0 else 0.0)
}

/**
 * The feature registry
 * */
object FeatureRegistry {

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

    /**
     * Register features
     * */
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

        // featureNamesToKeys = registeredFeatures.associate { it.name to it.key }
        featureNamesToKeys = registeredFeatures.associateBy({ it.name }, { it.key })
        featureKeysToNames = registeredFeatures.associateBy({ it.key }, { it.name })

        primaryFeatureNamesToKeys = primaryFeatures.associateBy({ it.name }, { it.key })
        primaryFeatureKeysToNames = primaryFeatures.associateBy({ it.key }, { it.name })

        floatFeatureNamesToKeys = floatFeatures.associateBy({ it.name }, { it.key })
        floatFeatureKeysToNames = floatFeatures.associateBy({ it.key }, { it.name })
    }
}

data class NodeFeature(
    val key: Int,
    val name: String,
    val isPrimary: Boolean = true,
    /**
     * Precision is the number of digits in a number.
     * Scale is the number of digits to the right of the decimal point in a number.
     * For example, the number 123.45 has a precision of 5 and a scale of 2.
     * */
    val scale: Int = 0
) {

    var value: Double = 0.0

    val toEntry = FeatureEntry(key, value)

    val isFloat: Boolean get() = scale > 0
    
    /**
     * TODO: avoid object
     * */
    companion object {
        /*
         * affect _sep
         * can be modified in configuration
         * */
        val SEPARATORS = arrayOf(":", "：")

        private val keyGen = AtomicInteger(0)
        /**
         * The key should return by incKey
         * */
        val currentKey get() = keyGen.get()
        /**
         * The key must start with 0, it might be an array index later
         * */
        val incKey: Int get() = keyGen.getAndIncrement()

        fun getKey(name: String): Int {
            return FeatureRegistry.featureNamesToKeys[name.toLowerCase()]?:
            throw IllegalArgumentException("Unknown feature name $name")
        }

        fun getValue(name: String, node: Node): Double {
            return node.getFeature(name)
        }

        fun isFloating(name: String): Boolean {
            return FeatureRegistry.floatFeatureNames.contains(name)
        }

        fun isFloating(key: Int): Boolean {
            return FeatureRegistry.floatFeatureKeys.contains(key)
        }
    }
}

object FeatureFormatter {
    val commonVariables = arrayOf(V_OWNER_BODY)

    /**
     * Get the string representation of this feature
     *
     * @return string
     */
    fun format(feature: FeatureEntry): String {
        val key = feature.key
        return key.toString() + ":" + formatValue(key, feature.value)
    }

    fun format(features: RealVector, sb: StringBuilder = StringBuilder(), eps: Double = 0.001): StringBuilder {
        return format(features, listOf(), sb, eps)
    }

    /**
     * Get the string representation of the features
     *
     * @features The feature vector
     * @featureKeys The feature keys to format
     * @sb The string builder
     * @eps The amount of allowed absolute error to judge if a double value is zero which is ignored
     * @return string
     */
    fun format(features: RealVector,
               featureKeys: Iterable<Int>, sb: StringBuilder = StringBuilder(), eps: Double = 0.001): StringBuilder {
        val size = Iterables.size(featureKeys)
        if (size == 0) {
            for (i in FeatureRegistry.featureNames.indices) {
                val value = features[i]
                if (!Precision.equals(value, 0.0, eps)) {
                    sb.append(FeatureRegistry.featureNames[i]).append(":").append(formatValue(i, value, eps)).append(' ')
                }
            }
        } else {
            for (i in featureKeys) {
                val value = features.getEntry(i)
                if (!Precision.equals(value, 0.0, eps)) {
                    sb.append(FeatureRegistry.featureNames[i]).append(":").append(formatValue(i, value, eps)).append(' ')
                }
            }
        }

        return if (sb.endsWith(' ')) sb.deleteCharAt(sb.length - 1) else sb
    }

    fun format(variables: Map<String, Any>, sb: StringBuilder = StringBuilder(), eps: Double = 0.001): StringBuilder {
        return format(variables, listOf(), sb, eps)
    }

    fun format(variables: Map<String, Any>, names: Collection<String>): StringBuilder {
        return FeatureFormatter.format(variables, names, StringBuilder(), 0.001)
    }

    fun format(variables: Map<String, Any>, names: Collection<String>, sb: StringBuilder = StringBuilder(), eps: Double = 0.001): StringBuilder {
        variables.filter { it.key !in commonVariables }.forEach { (name, value) ->
            if (names.isEmpty() || name in names) {
                var s = when (value) {
                    is Double -> formatValue(value, true, eps)
                    is Color -> value.toHexString()
                    is Rectangle -> value.str
                    is Element -> value.name
                    is FeaturedDocument -> value.location
                    else -> value.toString()
                }
                if (s.isNotEmpty()) {
                    s = StringUtils.abbreviate(s, 20)
                    sb.append(name).append(':').append(s).append(' ')
                }
            }
        }
        return if (sb.endsWith(' ')) sb.deleteCharAt(sb.length - 1) else sb
    }

    fun formatValue(key: Int, value: Double, eps: Double = 0.001): String {
        return formatValue(value, NodeFeature.isFloating(key))
    }

    fun formatValue(value: Double, isFloating: Boolean = true, eps: Double = 0.001): String {
        return if (isFloating) {
            if (Precision.equals(value, 0.0, eps)) "" else String.format("%.2f", value)
        } else {
            String.format("%d", value.toInt())
        }
    }
}
