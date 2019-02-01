package ai.platon.pulsar.dom.features.defined

import ai.platon.pulsar.dom.features.NodeFeature
import ai.platon.pulsar.dom.features.NodeFeature.Companion.incKey

const val FEATURE_VERSION: Int = 10001

enum class F(val key: Int, val alias: String = "", val isPrimary: Boolean = true, val isFloat: Boolean = false) {
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
    SEQ(incKey, "seq");

    fun toFeature(): NodeFeature {
        return NodeFeature(key, alias, isPrimary, isFloat)
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
@JvmField val SEQ = F.SEQ.key       // element depth

// the number of features
@JvmField val N = NodeFeature.currentKey
