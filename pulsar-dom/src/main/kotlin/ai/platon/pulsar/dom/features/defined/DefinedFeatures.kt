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

@JvmField val TOP = F.TOP.key
@JvmField val LEFT = F.LEFT.key
@JvmField val WIDTH = F.WIDTH.key
@JvmField val HEIGHT = F.HEIGHT.key

@JvmField val CH = F.CH.key

@JvmField val TN   = F.TN.key
@JvmField val IMG  = F.IMG.key
@JvmField val A    = F.A.key

@JvmField val SIB = F.SIB.key
@JvmField val C = F.C.key

@JvmField val DEP = F.DEP.key
@JvmField val SEQ = F.SEQ.key

@JvmField val DNS = F.DNS.key

@JvmField val N = F.entries.size