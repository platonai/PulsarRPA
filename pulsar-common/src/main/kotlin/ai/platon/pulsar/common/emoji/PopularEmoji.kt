package ai.platon.pulsar.common.emoji

/**
 * Popular Emojis used by PulsarRPA.
 *
 * @see [Emoji data](https://unicode.org/Public/emoji/1.0/emoji-data.txt)
 * @see [Unicode® Technical Standard #51](http://www.unicode.org/reports/tr51/)
 * @see [JEmoji](https://github.com/felldo/JEmoji)
 * */
enum class PopularEmoji(val value: String, val alt: String) {
    CANCELLATION_X("\uD83D\uDDD9", "x"),
    LIGHTNING("⚡", "⚡"),
    CIRCLE_ARROW_1("\uD83D\uDD03", "〰"), // clockwise downwards and upwards open circle arrows, clockwise downwards and upwards open circle arrows
    HOT_BEVERAGE("☕", "☕"),
    HARD_DRIVER("\uD83D\uDDB4", "✉"),
    OPTICAL_DISC("\uD83D\uDCBF", "✉"),
    BUG("\uD83D\uDC1B", "☹"),
    SKULL_CROSSBONES("☠", "☠"),
    HUNDRED_POINTS("\uD83D\uDCAF", "100!"),
    SPARKLES("✨", "✨"),
    BROKEN_HEART("\uD83D\uDC94", "☹"),
    RACING_CAR("\uD83C\uDFCE", "⏭"),
    DELIVERY_TRUCK("\uD83D\uDE9A", "▶"),
    WARNING("⚠", "⚠"),
    WHITE_HEAVY_CHECK("✅", "✅"),
    CHECK_MARK("✓", "✓"),
    HEAVY_MULTIPLICATION_X("✖", "✖"),
    BALLOT_X("✗", "✗"),
    FENCER("\uD83E\uDD3A", "⚔"),
    FLEXED_BICEPS("\uD83D\uDCAA", "✊")
    ;

    override fun toString() = if (emojiVersion() <= 1.1) alt else value
    
    private fun emojiVersion(): Double {
        return System.getProperty("emoji.version")?.toDoubleOrNull() ?: 6.0
    }
}
