package `fun`.platonic.scent.dom

import `fun`.platonic.scent.dom.nodes.getFeature
import org.apache.commons.math3.linear.RealVector
import org.jsoup.nodes.Node

operator fun RealVector.set(index: Int, value: Double) { setEntry(index, value) }

operator fun RealVector.get(index: Int): Double { return getEntry(index) }

val RealVector.isEmpty: Boolean get() = dimension == 0

typealias Feature = Pair<Int, Double>

const val FEATURE_VERSION: Int = 10001

enum class F { TOP, LEFT, WIDTH, HEIGHT, CH, TN, IMG, A, SIB, C, DEP, SEQ, N }

// vision features
@JvmField val TOP = F.TOP.ordinal       // top
@JvmField val LEFT = F.LEFT.ordinal     // left
@JvmField val WIDTH = F.WIDTH.ordinal   // width
@JvmField val HEIGHT = F.HEIGHT.ordinal // height

@JvmField val CH = F.CH.ordinal         // chars, all characters of no-blank descend text nodes are accumulated

@JvmField val TN   = F.TN.ordinal       // no-blank descend text nodes
@JvmField val IMG  = F.IMG.ordinal      // images
@JvmField val A    = F.A.ordinal        // anchors (hyper links)

@JvmField val SIB = F.SIB.ordinal       // element siblings, equals to direct element children of parent
@JvmField val C = F.C.ordinal           // direct element children

@JvmField val DEP = F.DEP.ordinal       // element depth
@JvmField val SEQ = F.SEQ.ordinal       // element depth

// the last feature, it's also the number of features
@JvmField val N = F.N.ordinal

const val nTOP = "top"            // top
const val nLEFT = "left"          // left
const val nWIDTH = "width"        // width
const val nHEIGHT = "height"      // height
const val nCH = "char"            // accumulated character number of all no-blank descend text nodes

const val nTN = "txt_nd"          // no-blank text nodes
const val nIMG = "img"            // images
const val nA = "a"                // anchors

const val nSIB = "sibling"        // number of element siblings, equals to it's parent's element child count
const val nC = "child"            // children

const val nDEP = "dep"            // node depth
const val nSEQ = "seq"            // separators in text

const val nN = "N"                // number of features

@JvmField val SIMPLE_FEATURE_NAMES = F.values().map { it.name.toLowerCase() }.toTypedArray()

// must strictly keep the order consistent with enum class F
@JvmField val FEATURE_NAMES = arrayOf(
        nTOP, nLEFT, nWIDTH, nHEIGHT,
        nCH, nTN, nIMG, nA, nSIB, nC, nDEP, nSEQ
)

@JvmField val FEATURE_NAMES_TO_KEYS = FEATURE_NAMES.indices.associateBy { FEATURE_NAMES[it] }

@JvmField val FEATURE_KEYS_TO_NAMES = FEATURE_NAMES_TO_KEYS.entries.associateBy({ it.value }, { it.key })

@JvmField val PRIMARY_FEATURE_NAMES = arrayOf(
        nTOP, nLEFT, nWIDTH, nHEIGHT,
        nCH,
        nTN, nIMG, nA,
        nSIB, nC, nDEP, nSEQ
)

@JvmField val PRIMARY_FEATURE_KEYS = PRIMARY_FEATURE_NAMES.indices.associateBy { PRIMARY_FEATURE_NAMES[it]}

@JvmField val FLOAT_FEATURE_NAMES = arrayOf<String>()

@JvmField val FLOAT_FEATURE_KEYS = FEATURE_NAMES_TO_KEYS.filter { it.key in FLOAT_FEATURE_NAMES }

/*
 * affect _sep
 * can be modified in configuration
 * */
@JvmField val SEPARATORS = arrayOf(":", "：")

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
fun format(feature: Feature): String {
    val key = feature.first
    return key.toString() + ":" + formatValue(key, feature.second)
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
