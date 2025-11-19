@file:Suppress("unused")

package ai.platon.pulsar.common.emoji

import ai.platon.pulsar.common.emoji.PopularEmoji.N0
import ai.platon.pulsar.common.emoji.PopularEmoji.N1
import ai.platon.pulsar.common.emoji.PopularEmoji.N2
import ai.platon.pulsar.common.emoji.PopularEmoji.N3
import ai.platon.pulsar.common.emoji.PopularEmoji.N4
import ai.platon.pulsar.common.emoji.PopularEmoji.N5
import ai.platon.pulsar.common.emoji.PopularEmoji.N6
import ai.platon.pulsar.common.emoji.PopularEmoji.N7
import ai.platon.pulsar.common.emoji.PopularEmoji.N8
import ai.platon.pulsar.common.emoji.PopularEmoji.N9

/**
 * Popular Emojis used by Browser4.
 *
 * @see [Emoji Frequency](https://home.unicode.org/emoji/emoji-frequency/)
 * @see [Emoji data](https://unicode.org/Public/emoji/1.0/emoji-data.txt)
 * @see [Unicode® Technical Standard #51](http://www.unicode.org/reports/tr51/)
 * @see [JEmoji ](https://github.com/felldo/JEmoji)
 * */
enum class PopularEmoji(val value: String, val alt: String = value) {
    CANCELLATION_X("""🗙""", "x"),
    LIGHTNING("⚡", "⚡"),
    CIRCLE_ARROW_1("""🖴""", "〰"), // clockwise downwards and upwards open circle arrows, clockwise downwards and upwards open circle arrows
    HOT_BEVERAGE("☕", "☕"),
    HARD_DRIVER("", "✉"),
    OPTICAL_DISC("""💿""", "✉"),
    BUG("""🐛""", "☹"),
    SKULL_CROSSBONES("☠", "☠"),
    SPARKLES("✨", "✨"),
    RACING_CAR("""🏎""", "⏭"),
    DELIVERY_TRUCK("""🚚""", "▶"),
    WARNING("⚠", "⚠"),
    WHITE_HEAVY_CHECK("✅", "✅"),
    CHECK_MARK("✓", "✓"),
    HEAVY_MULTIPLICATION_X("✖", "✖"),
    BALLOT_X("✗", "✗"),
    FENCER("""🤺""", "⚔"),
    FLEXED_BICEPS("""💪""", "✊"),

    // Face-smiling
    GRINNING_FACE("😀"),
    GRINNING_FACE_WITH_BIG_EYES("😃"),
    GRINNING_FACE_WITH_SMILING_EYES("😄"),
    BEAMING_FACE_WITH_SMILING_EYES("😁"),
    GRINNING_SQUINTING_FACE("😆"),
    GRINNING_FACE_WITH_SWEAT("😅"),
    ROLLING_ON_THE_FLOOR_LAUGHING("🤣"),
    FACE_WITH_TEARS_OF_JOY("😂"),
    SLIGHTLY_SMILING_FACE("🙂"),
    UPSIDE_DOWN_FACE("🙃"),
    WINKING_FACE("😉"),
    SMILING_FACE_WITH_SMILING_EYES("😊"),
    SMILING_FACE_WITH_HALO("😇"),

    // Hands
    WAVING_HAND("👋"),
    OK_HAND("👌"),
    PEACE_SIGN("✌️"),
    CLAPPING_HANDS("👏"),
    FOLDED_HANDS("🙏"),
    THUMBS_UP("👍"),
    THUMBS_DOWN("👎"),
    RAISED_HANDS("🙌"),

    // Plant-flower
    BOUQUET("💐"),
    ROSE("🌹"),
    WILTED_FLOWER("🥀"),
    HIBISCUS("🌺"),
    TULIP("🌷"),
    CHERRY_BLOSSOM("🌸"),
    WHITE_FLOWER("💮"),
    ROSETTE("🏵️"),
    SUNFLOWER("🌻"),
    BLOSSOM("🌼"),

    // Emotion
    KISS_MARK("💋"),
    LOVE_LETTER("💌"),
    HEART_WITH_ARROW("💘"),
    HEART_WITH_RIBBON("💝"),
    SPARKLING_HEART("💖"),
    GROWING_HEART("💗"),
    BEATING_HEART("💓"),
    REVOLVING_HEARTS("💞"),
    TWO_HEARTS("💕"),
    HEART_DECORATION("💟"),
    HEART_EXCLAMATION("❣"),
    BROKEN_HEART("💔"),
    RED_HEART("❤"),
    ORANGE_HEART("🧡"),
    YELLOW_HEART("💛"),
    GREEN_HEART("💚"),
    BLUE_HEART("💙"),
    PURPLE_HEART("💜"),
    BROWN_HEART("🤎"),
    BLACK_HEART("🖤"),
    WHITE_HEART("🤍"),
    HUNDRED_POINTS("💯"),
    ANGER_SYMBOL("💢"),
    COLLISION("💥"),
    DIZZY("💫"),
    SWEAT_DROPLETS("💦"),
    DASHING_AWAY("💨"),
    HOLE("🕳"),
    BOMB("💣"),
    SPEECH_BALLOON("💬"),
    EYE_IN_SPEECH_BUBBLE("👁️‍🗨️"),
    LEFT_SPEECH_BUBBLE("🗨"),
    RIGHT_ANGER_BUBBLE("🗯"),
    THOUGHT_BALLOON("💭"),
    ZZZ("💤"),

    // Objects/Symbols requested additions
    DIRECT_HIT("🎯"),
    GOAL_NET("🥅"),
    TRIANGULAR_FLAG("🚩"),
    CHEQUERED_FLAG("🏁"),
    ROUND_PUSHPIN("📍"),
    TELESCOPE("🔭"),
    LIGHT_BULB("💡"),
    CLIPBOARD("📋"),
    CARD_INDEX_DIVIDERS("🗂️"),
    MEMO("📝"),
    RECEIPT("🧾"),
    PUZZLE_PIECE("🧩"),
    HAMMER_AND_WRENCH("🛠️"),
    BRICK("🧱"),
    LADDER("🪜"),
    ROCKET("🚀"),
    GEAR("⚙️"),
    COUNTERCLOCKWISE_ARROWS_BUTTON("🔄"),
    REPEAT_BUTTON("🔁"),
    UPWARDS_BUTTON("🔼"),
    CHART_INCREASING("📈"),
    BAR_CHART("📊"),
    COMPASS("🧭"),
    HOURGLASS_NOT_DONE("⏳"),
    THREE_OCLOCK("🕒"),
    SOON_ARROW("🔜"),
    JOYSTICK("🕹️"),
    BALLOT_BOX_WITH_CHECK("☑️"),
    CROSS_MARK("❌"),
    YELLOW_CIRCLE("🟡"),
    GREEN_CIRCLE("🟢"),
    BLUE_CIRCLE("🔵"),
    WHITE_CIRCLE("⚪"),
    BRAIN("🧠"),
    MAGNIFYING_GLASS_TILTED_LEFT("🔍"),
    ABACUS("🧮"),
    WRENCH("🔧"),
    PERSON_IN_LOTUS_POSITION("🧘"),

    // Numbers
    N0("0️⃣", "0"),
    N1("1️⃣", "1"),
    N2("2️⃣", "2"),
    N3("3️⃣", "3"),
    N4("4️⃣", "4"),
    N5("5️⃣", "5"),
    N6("6️⃣", "6"),
    N7("7️⃣", "7"),
    N8("8️⃣", "8"),
    N9("9️⃣", "9"),
    N10("🔟", "10"),
    ;

    constructor(value: String) : this(value, value)

    override fun toString() = value
}

typealias Emo = PopularEmoji

fun emo(n: Int): String {
    return n.toString().map { d ->
        when (d) {
            '0' -> N0
            '1' -> N1
            '2' -> N2
            '3' -> N3
            '4' -> N4
            '5' -> N5
            '6' -> N6
            '7' -> N7
            '8' -> N8
            '9' -> N9
        }
    }.joinToString("")
}

/**
 * 常用来表示 输出 / 数据 / 结果 / 导出 的 emoji 👇
 * */
const val DATA_EMOJIS = """📤📄📊📈🧾💾🗃️🧮📝📥📦📰"""
