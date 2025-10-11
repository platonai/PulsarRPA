package ai.platon.pulsar.common

import java.awt.Color
import java.util.*

const val ALPHA_MASK = -0x1000000 // = 0xffffffff

// get rgb with alpha set to be 0, which means the color is opaque
// this value is usually what passed in to construct the color
val Color.opaqueRgb get() = ALPHA_MASK xor rgb

fun Color.toHexString(): String {
    return String.format("%06x", opaqueRgb)
}

data class NamedColor(var name: String, var r: Int, var g: Int, var b: Int)

fun NamedColor.toColor(): Color {
    return Color(r, g, b)
}

/**
 * Named colors, see https://stackoverflow.com/questions/4126029/convert-rgb-values-to-color-name
 * Popular color family, see http://www.ip138.com/yanse/common.htm
 * */
object ColorFamily {

    val namedColors by lazy { initColorList().associateBy({ it.name }, { it.toColor() }) }

    val blueRgbs = arrayOf(
            0xFFFFCC, 0xFFCC99, 0x99CCCC, 0xCCCCCC, 0x3399CC,
            0xCCFFFF, 0xFFFFCC, 0xFFFFFF, 0x003366, 0x003366,
            0xFFCCCC, 0x99CCFF, 0x336699, 0x99CCFF, 0xCCCCCC)

    val greenRgbs = arrayOf(
            0x009966, 0xFFFFCC, 0x669933, 0x339933, 0x006633,
            0x99CC00, 0xCCCC66, 0xCCCCCC, 0xFFCC33, 0x990033,
            0xFFFF00, 0x336666, 0x000000, 0x336699, 0xFF9900)

    val yellowRgbs = arrayOf(
            0xFFFFCC, 0xFFFF00, 0xFFCC00, 0xFF9966, 0xFFFF99,
            0xCCFFFF, 0xFFFFFF, 0x0000CC, 0xFFFFCC, 0x99CC99,
            0xFFCCCC, 0x9933FF, 0xFFFF99, 0x99CC99, 0x666600
    )

    val redRgbs1 = arrayOf(
            0xFFFFCC, 0xFFCCCC, 0xFF6666, 0xFF6666, 0xFF0033,
            0xCCFFFF, 0xFFFF99, 0xFFFF66, 0xFFFF00, 0x333399,
            0xFFCCCC, 0xCCCCFF, 0x99CC66, 0x0066CC, 0xCCCC00
    )

    val redRgbs2 = arrayOf(
            0x99CCCC, 0x0099CC, 0xCC3333, 0xCC0033, 0xCC0033,
            0xFFCC99, 0xCCCCCC, 0xCCCCCC, 0x333333, 0x000000,
            0xFFCCCC, 0xFF6666, 0x003366, 0xCCCC00, 0x003399
    )

    val redRgbs3 = arrayOf(
            0xFF9999, 0xFF9966, 0x993333, 0x336633, 0x000000,
            0x996699, 0xFF6666, 0xCCCC00, 0x990033, 0x99CC00,
            0xFFCCCC, 0xFFCCCC, 0x663366, 0xFFCC99, 0xCC0033
    )

    val redRgbs4 = arrayOf(
            0xCC9999, 0xCC9966, 0xCCCC99, 0x993333, 0x999933,
            0xFFFFCC, 0x666666, 0x666666, 0xCC9966, 0x993333,
            0xCCCC99, 0xCC9999, 0xCC9999, 0x003300, 0x333300
    )

    val redRgbs = arrayOf(
            redRgbs1, redRgbs2, redRgbs3, redRgbs4
    )

    val lightColorRgbs = arrayOf(
            0xFFFFCC, 0xCCFFFF, 0xCCCCCC, 0xFFCCCC, 0xFFCC99,
            0xFFFFCC, 0xFFFF99, 0xFFFF00, 0xCCFFFF, 0xFFFFCC,
            0xFFCC99, 0xCC99CC, 0xFFFFCC, 0xCCCC99, 0xCCCCFF
    )

    val blueColors = blueRgbs.map { Color(it) }

    val greenColors = greenRgbs.map { Color(it) }

    val yellowColors = yellowRgbs.map { Color(it) }

    val redColors1 = redRgbs1.map { Color(it) }

    val redColors2 = redRgbs2.map { Color(it) }

    val redColors3 = redRgbs3.map { Color(it) }

    val redColors4 = redRgbs4.map { Color(it) }

    val redColors = arrayOf(redColors1, redColors2, redColors3, redColors4)

    val lightColors = lightColorRgbs.map { Color(it) }
}

private fun initColorList(): ArrayList<NamedColor> {
    val colorList = ArrayList<NamedColor>()
    colorList.add(NamedColor("AliceBlue", 0xF0, 0xF8, 0xFF))
    colorList.add(NamedColor("AntiqueWhite", 0xFA, 0xEB, 0xD7))
    colorList.add(NamedColor("Aqua", 0x00, 0xFF, 0xFF))
    colorList.add(NamedColor("Aquamarine", 0x7F, 0xFF, 0xD4))
    colorList.add(NamedColor("Azure", 0xF0, 0xFF, 0xFF))
    colorList.add(NamedColor("Beige", 0xF5, 0xF5, 0xDC))
    colorList.add(NamedColor("Bisque", 0xFF, 0xE4, 0xC4))
    colorList.add(NamedColor("Black", 0x00, 0x00, 0x00))
    colorList.add(NamedColor("BlanchedAlmond", 0xFF, 0xEB, 0xCD))
    colorList.add(NamedColor("Blue", 0x00, 0x00, 0xFF))
    colorList.add(NamedColor("BlueViolet", 0x8A, 0x2B, 0xE2))
    colorList.add(NamedColor("Brown", 0xA5, 0x2A, 0x2A))
    colorList.add(NamedColor("BurlyWood", 0xDE, 0xB8, 0x87))
    colorList.add(NamedColor("CadetBlue", 0x5F, 0x9E, 0xA0))
    colorList.add(NamedColor("Chartreuse", 0x7F, 0xFF, 0x00))
    colorList.add(NamedColor("Chocolate", 0xD2, 0x69, 0x1E))
    colorList.add(NamedColor("Coral", 0xFF, 0x7F, 0x50))
    colorList.add(NamedColor("CornflowerBlue", 0x64, 0x95, 0xED))
    colorList.add(NamedColor("Cornsilk", 0xFF, 0xF8, 0xDC))
    colorList.add(NamedColor("Crimson", 0xDC, 0x14, 0x3C))
    colorList.add(NamedColor("Cyan", 0x00, 0xFF, 0xFF))
    colorList.add(NamedColor("DarkBlue", 0x00, 0x00, 0x8B))
    colorList.add(NamedColor("DarkCyan", 0x00, 0x8B, 0x8B))
    colorList.add(NamedColor("DarkGoldenRod", 0xB8, 0x86, 0x0B))
    colorList.add(NamedColor("DarkGray", 0xA9, 0xA9, 0xA9))
    colorList.add(NamedColor("DarkGreen", 0x00, 0x64, 0x00))
    colorList.add(NamedColor("DarkKhaki", 0xBD, 0xB7, 0x6B))
    colorList.add(NamedColor("DarkMagenta", 0x8B, 0x00, 0x8B))
    colorList.add(NamedColor("DarkOliveGreen", 0x55, 0x6B, 0x2F))
    colorList.add(NamedColor("DarkOrange", 0xFF, 0x8C, 0x00))
    colorList.add(NamedColor("DarkOrchid", 0x99, 0x32, 0xCC))
    colorList.add(NamedColor("DarkRed", 0x8B, 0x00, 0x00))
    colorList.add(NamedColor("DarkSalmon", 0xE9, 0x96, 0x7A))
    colorList.add(NamedColor("DarkSeaGreen", 0x8F, 0xBC, 0x8F))
    colorList.add(NamedColor("DarkSlateBlue", 0x48, 0x3D, 0x8B))
    colorList.add(NamedColor("DarkSlateGray", 0x2F, 0x4F, 0x4F))
    colorList.add(NamedColor("DarkTurquoise", 0x00, 0xCE, 0xD1))
    colorList.add(NamedColor("DarkViolet", 0x94, 0x00, 0xD3))
    colorList.add(NamedColor("DeepPink", 0xFF, 0x14, 0x93))
    colorList.add(NamedColor("DeepSkyBlue", 0x00, 0xBF, 0xFF))
    colorList.add(NamedColor("DimGray", 0x69, 0x69, 0x69))
    colorList.add(NamedColor("DodgerBlue", 0x1E, 0x90, 0xFF))
    colorList.add(NamedColor("FireBrick", 0xB2, 0x22, 0x22))
    colorList.add(NamedColor("FloralWhite", 0xFF, 0xFA, 0xF0))
    colorList.add(NamedColor("ForestGreen", 0x22, 0x8B, 0x22))
    colorList.add(NamedColor("Fuchsia", 0xFF, 0x00, 0xFF))
    colorList.add(NamedColor("Gainsboro", 0xDC, 0xDC, 0xDC))
    colorList.add(NamedColor("GhostWhite", 0xF8, 0xF8, 0xFF))
    colorList.add(NamedColor("Gold", 0xFF, 0xD7, 0x00))
    colorList.add(NamedColor("GoldenRod", 0xDA, 0xA5, 0x20))
    colorList.add(NamedColor("Gray", 0x80, 0x80, 0x80))
    colorList.add(NamedColor("Green", 0x00, 0x80, 0x00))
    colorList.add(NamedColor("GreenYellow", 0xAD, 0xFF, 0x2F))
    colorList.add(NamedColor("HoneyDew", 0xF0, 0xFF, 0xF0))
    colorList.add(NamedColor("HotPink", 0xFF, 0x69, 0xB4))
    colorList.add(NamedColor("IndianRed", 0xCD, 0x5C, 0x5C))
    colorList.add(NamedColor("Indigo", 0x4B, 0x00, 0x82))
    colorList.add(NamedColor("Ivory", 0xFF, 0xFF, 0xF0))
    colorList.add(NamedColor("Khaki", 0xF0, 0xE6, 0x8C))
    colorList.add(NamedColor("Lavender", 0xE6, 0xE6, 0xFA))
    colorList.add(NamedColor("LavenderBlush", 0xFF, 0xF0, 0xF5))
    colorList.add(NamedColor("LawnGreen", 0x7C, 0xFC, 0x00))
    colorList.add(NamedColor("LemonChiffon", 0xFF, 0xFA, 0xCD))
    colorList.add(NamedColor("LightBlue", 0xAD, 0xD8, 0xE6))
    colorList.add(NamedColor("LightCoral", 0xF0, 0x80, 0x80))
    colorList.add(NamedColor("LightCyan", 0xE0, 0xFF, 0xFF))
    colorList.add(NamedColor("LightGoldenRodYellow", 0xFA, 0xFA, 0xD2))
    colorList.add(NamedColor("LightGray", 0xD3, 0xD3, 0xD3))
    colorList.add(NamedColor("LightGreen", 0x90, 0xEE, 0x90))
    colorList.add(NamedColor("LightPink", 0xFF, 0xB6, 0xC1))
    colorList.add(NamedColor("LightSalmon", 0xFF, 0xA0, 0x7A))
    colorList.add(NamedColor("LightSeaGreen", 0x20, 0xB2, 0xAA))
    colorList.add(NamedColor("LightSkyBlue", 0x87, 0xCE, 0xFA))
    colorList.add(NamedColor("LightSlateGray", 0x77, 0x88, 0x99))
    colorList.add(NamedColor("LightSteelBlue", 0xB0, 0xC4, 0xDE))
    colorList.add(NamedColor("LightYellow", 0xFF, 0xFF, 0xE0))
    colorList.add(NamedColor("Lime", 0x00, 0xFF, 0x00))
    colorList.add(NamedColor("LimeGreen", 0x32, 0xCD, 0x32))
    colorList.add(NamedColor("Linen", 0xFA, 0xF0, 0xE6))
    colorList.add(NamedColor("Magenta", 0xFF, 0x00, 0xFF))
    colorList.add(NamedColor("Maroon", 0x80, 0x00, 0x00))
    colorList.add(NamedColor("MediumAquaMarine", 0x66, 0xCD, 0xAA))
    colorList.add(NamedColor("MediumBlue", 0x00, 0x00, 0xCD))
    colorList.add(NamedColor("MediumOrchid", 0xBA, 0x55, 0xD3))
    colorList.add(NamedColor("MediumPurple", 0x93, 0x70, 0xDB))
    colorList.add(NamedColor("MediumSeaGreen", 0x3C, 0xB3, 0x71))
    colorList.add(NamedColor("MediumSlateBlue", 0x7B, 0x68, 0xEE))
    colorList.add(NamedColor("MediumSpringGreen", 0x00, 0xFA, 0x9A))
    colorList.add(NamedColor("MediumTurquoise", 0x48, 0xD1, 0xCC))
    colorList.add(NamedColor("MediumVioletRed", 0xC7, 0x15, 0x85))
    colorList.add(NamedColor("MidnightBlue", 0x19, 0x19, 0x70))
    colorList.add(NamedColor("MintCream", 0xF5, 0xFF, 0xFA))
    colorList.add(NamedColor("MistyRose", 0xFF, 0xE4, 0xE1))
    colorList.add(NamedColor("Moccasin", 0xFF, 0xE4, 0xB5))
    colorList.add(NamedColor("NavajoWhite", 0xFF, 0xDE, 0xAD))
    colorList.add(NamedColor("Navy", 0x00, 0x00, 0x80))
    colorList.add(NamedColor("OldLace", 0xFD, 0xF5, 0xE6))
    colorList.add(NamedColor("Olive", 0x80, 0x80, 0x00))
    colorList.add(NamedColor("OliveDrab", 0x6B, 0x8E, 0x23))
    colorList.add(NamedColor("Orange", 0xFF, 0xA5, 0x00))
    colorList.add(NamedColor("OrangeRed", 0xFF, 0x45, 0x00))
    colorList.add(NamedColor("Orchid", 0xDA, 0x70, 0xD6))
    colorList.add(NamedColor("PaleGoldenRod", 0xEE, 0xE8, 0xAA))
    colorList.add(NamedColor("PaleGreen", 0x98, 0xFB, 0x98))
    colorList.add(NamedColor("PaleTurquoise", 0xAF, 0xEE, 0xEE))
    colorList.add(NamedColor("PaleVioletRed", 0xDB, 0x70, 0x93))
    colorList.add(NamedColor("PapayaWhip", 0xFF, 0xEF, 0xD5))
    colorList.add(NamedColor("PeachPuff", 0xFF, 0xDA, 0xB9))
    colorList.add(NamedColor("Peru", 0xCD, 0x85, 0x3F))
    colorList.add(NamedColor("Pink", 0xFF, 0xC0, 0xCB))
    colorList.add(NamedColor("Plum", 0xDD, 0xA0, 0xDD))
    colorList.add(NamedColor("PowderBlue", 0xB0, 0xE0, 0xE6))
    colorList.add(NamedColor("Purple", 0x80, 0x00, 0x80))
    colorList.add(NamedColor("Red", 0xFF, 0x00, 0x00))
    colorList.add(NamedColor("RosyBrown", 0xBC, 0x8F, 0x8F))
    colorList.add(NamedColor("RoyalBlue", 0x41, 0x69, 0xE1))
    colorList.add(NamedColor("SaddleBrown", 0x8B, 0x45, 0x13))
    colorList.add(NamedColor("Salmon", 0xFA, 0x80, 0x72))
    colorList.add(NamedColor("SandyBrown", 0xF4, 0xA4, 0x60))
    colorList.add(NamedColor("SeaGreen", 0x2E, 0x8B, 0x57))
    colorList.add(NamedColor("SeaShell", 0xFF, 0xF5, 0xEE))
    colorList.add(NamedColor("Sienna", 0xA0, 0x52, 0x2D))
    colorList.add(NamedColor("Silver", 0xC0, 0xC0, 0xC0))
    colorList.add(NamedColor("SkyBlue", 0x87, 0xCE, 0xEB))
    colorList.add(NamedColor("SlateBlue", 0x6A, 0x5A, 0xCD))
    colorList.add(NamedColor("SlateGray", 0x70, 0x80, 0x90))
    colorList.add(NamedColor("Snow", 0xFF, 0xFA, 0xFA))
    colorList.add(NamedColor("SpringGreen", 0x00, 0xFF, 0x7F))
    colorList.add(NamedColor("SteelBlue", 0x46, 0x82, 0xB4))
    colorList.add(NamedColor("Tan", 0xD2, 0xB4, 0x8C))
    colorList.add(NamedColor("Teal", 0x00, 0x80, 0x80))
    colorList.add(NamedColor("Thistle", 0xD8, 0xBF, 0xD8))
    colorList.add(NamedColor("Tomato", 0xFF, 0x63, 0x47))
    colorList.add(NamedColor("Turquoise", 0x40, 0xE0, 0xD0))
    colorList.add(NamedColor("Violet", 0xEE, 0x82, 0xEE))
    colorList.add(NamedColor("Wheat", 0xF5, 0xDE, 0xB3))
    colorList.add(NamedColor("White", 0xFF, 0xFF, 0xFF))
    colorList.add(NamedColor("WhiteSmoke", 0xF5, 0xF5, 0xF5))
    colorList.add(NamedColor("Yellow", 0xFF, 0xFF, 0x00))
    colorList.add(NamedColor("YellowGreen", 0x9A, 0xCD, 0x32))
    return colorList
}
