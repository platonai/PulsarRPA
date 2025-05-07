@file:Suppress("unused")

package ai.platon.pulsar.common.emoji

/**
 * Popular Emojis used by PulsarRPA.
 *
 * @see [Emoji Frequency](https://home.unicode.org/emoji/emoji-frequency/)
 * @see [Emoji data](https://unicode.org/Public/emoji/1.0/emoji-data.txt)
 * @see [Unicode® Technical Standard #51](http://www.unicode.org/reports/tr51/)
 * @see [JEmoji ](https://github.com/felldo/JEmoji)
 * */
enum class PopularEmoji(val value: String, val alt: String) {
    CANCELLATION_X("""🗙""", "x"),
    LIGHTNING("⚡", "⚡"),
    CIRCLE_ARROW_1("""🖴""", "〰"), // clockwise downwards and upwards open circle arrows, clockwise downwards and upwards open circle arrows
    HOT_BEVERAGE("☕", "☕"),
    HARD_DRIVER("", "✉"),
    OPTICAL_DISC("""💿""", "✉"),
    BUG("""🐛""", "☹"),
    SKULL_CROSSBONES("☠", "☠"),
    HUNDRED_POINTS("""💯""", "100!"),
    SPARKLES("✨", "✨"),
    BROKEN_HEART("""💔""", "☹"),
    RACING_CAR("""🏎""", "⏭"),
    DELIVERY_TRUCK("""🚚""", "▶"),
    WARNING("⚠", "⚠"),
    WHITE_HEAVY_CHECK("✅", "✅"),
    CHECK_MARK("✓", "✓"),
    HEAVY_MULTIPLICATION_X("✖", "✖"),
    BALLOT_X("✗", "✗"),
    FENCER("""🤺""", "⚔"),
    FLEXED_BICEPS("""💪""", "✊")
    ;

    override fun toString() = if (emojiVersion() <= 1.1) alt else value
    
    private fun emojiVersion(): Double {
        return System.getProperty("emoji.version")?.toDoubleOrNull() ?: 6.0
    }
}
