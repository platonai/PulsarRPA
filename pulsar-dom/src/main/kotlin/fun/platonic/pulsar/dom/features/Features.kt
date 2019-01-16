package `fun`.platonic.pulsar.dom.features

import `fun`.platonic.pulsar.common.math.vectors.get
import `fun`.platonic.pulsar.dom.nodes.node.ext.getFeature
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Node

data class NodeFeature(val key: Int, val value: Double) {
    companion object {
        fun getKey(name: String): Int {
            return FeatureFormatter.FEATURE_NAMES_TO_KEYS[name.toLowerCase()]?:throw IllegalArgumentException("Unknown feature name $name")
        }

        fun getValue(name: String, node: Node): Double {
            return node.getFeature(name)
        }
    }
}

object FeatureFormatter {

    val SIMPLE_FEATURE_NAMES = F.values().map { it.name.toLowerCase() }.toTypedArray()

    // must strictly keep the order consistent with enum class F
    val FEATURE_NAMES = arrayOf(
            nTOP, nLEFT, nWIDTH, nHEIGHT,
            nCH, nTN, nIMG, nA, nSIB, nC, nDEP, nSEQ
    )

    val FEATURE_NAMES_TO_KEYS = FEATURE_NAMES.indices.associateBy { FEATURE_NAMES[it] }

    val FEATURE_KEYS_TO_NAMES = FEATURE_NAMES_TO_KEYS.entries.associateBy({ it.value }, { it.key })

    val PRIMARY_FEATURE_NAMES = arrayOf(
            nTOP, nLEFT, nWIDTH, nHEIGHT,
            nCH,
            nTN, nIMG, nA,
            nSIB, nC, nDEP, nSEQ
    )

    val PRIMARY_FEATURE_KEYS = PRIMARY_FEATURE_NAMES.indices.associateBy { PRIMARY_FEATURE_NAMES[it]}

    val FLOAT_FEATURE_NAMES = arrayOf<String>()

    val FLOAT_FEATURE_KEYS = FEATURE_NAMES_TO_KEYS.filter { it.key in FLOAT_FEATURE_NAMES }

    /*
     * affect _sep
     * can be modified in configuration
     * */
    val SEPARATORS = arrayOf(":", "：")

    fun getKey(name: String): Int {
        return FEATURE_NAMES_TO_KEYS[name.toLowerCase()]?:throw IllegalArgumentException("Unknown feature name $name")
    }

    fun getValue(name: String, node: Node): Double {
        return node.getFeature(name)
    }

    fun isFloating(name: String): Boolean {
        return FLOAT_FEATURE_NAMES.contains(name)
    }

    fun isFloating(key: Int): Boolean {
        return FLOAT_FEATURE_KEYS.containsValue(key)
    }

    /**
     * Get the string representation of this feature
     *
     * @return string
     */
    fun format(feature: NodeFeature): String {
        val key = feature.key
        return key.toString() + ":" + formatValue(key, feature.value)
    }

    fun format(features: RealVector, vararg featureKeys: Int, sb: StringBuilder = StringBuilder()): StringBuilder {
        if (featureKeys.isEmpty()) {
            for (i in FEATURE_NAMES.indices) {
                val value = features[i]
                if (value != 0.0) {
                    sb.append(FEATURE_NAMES[i]).append(":").append(formatValue(i, value)).append(' ')
                }
            }
        } else {
            for (i in featureKeys) {
                val value = features.getEntry(i)
                if (value != 0.0) {
                    sb.append(FEATURE_NAMES[i]).append(":").append(formatValue(i, value)).append(' ')
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
