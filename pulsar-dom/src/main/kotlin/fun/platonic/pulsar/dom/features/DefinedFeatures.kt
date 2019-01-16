package `fun`.platonic.pulsar.dom.features

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
