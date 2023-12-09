package ai.platon.pulsar.dom.features.defined

import ai.platon.pulsar.dom.features.NodeFeature
import ai.platon.pulsar.dom.features.NodeFeature.Companion.incKey

const val FEATURE_VERSION: Int = 10001

/**
 * The level 1 features definition.
 * */
enum class F(
    val key: Int,
    val alias: String = "",
    val isPrimary: Boolean = true,
    /**
     * Precision is the number of digits in a number.
     * Scale is the number of digits to the right of the decimal point in a number.
     * For example, the number 123.45 has a precision of 5 and a scale of 2.
     * */
    val scale: Int = 0,
) {
    TOP(incKey, "top"),
    LEFT(incKey, "left"),
    WIDTH(incKey, "width"),
    HEIGHT(incKey, "height"),
    CH(incKey, "char"),
    TN(incKey, "txt_nd"),
    IMG(incKey, "img"),
    A(incKey, "a"),
    SIB(incKey, "sibling"),
    C(incKey, "child"),
    DEP(incKey, "dep"),
    SEQ(incKey, "seq"),
    DNS(incKey, "txt_dns", scale = 4);
    
    val isFloat: Boolean get() = scale > 0
    
    fun toFeature(): NodeFeature {
        return NodeFeature(key, alias, isPrimary, scale)
    }
}

// vision features
@JvmField val TOP = F.TOP.key       // top
@JvmField val LEFT = F.LEFT.key     // left
@JvmField val WIDTH = F.WIDTH.key   // width
@JvmField val HEIGHT = F.HEIGHT.key // height

@JvmField val CH = F.CH.key         // chars, all characters of no-blank descend text nodes are accumulated

@JvmField val TN   = F.TN.key       // no-blank descend text nodes
@JvmField val IMG  = F.IMG.key      // images
@JvmField val A    = F.A.key        // anchors (hyper links)

@JvmField val SIB = F.SIB.key       // element siblings, equals to direct element children of parent
@JvmField val C = F.C.key           // direct element children

@JvmField val DEP = F.DEP.key       // element depth
@JvmField val SEQ = F.SEQ.key       // element sequence

@JvmField val DNS = F.DNS.key       //

// the number of features
@JvmField val N = F.entries.size
