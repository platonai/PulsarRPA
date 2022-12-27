package ai.platon.pulsar.common.emoji

/**
 * Unicode emoji used by Pulsar
 * */
enum class PopularEmoji(val value: String) {
    CANCELLATION_X("\uD83D\uDDD9"),
    LIGHTNING("⚡"),
    CIRCLE_ARROW_1("\uD83D\uDD03"), // clockwise downwards and upwards open circle arrows, clockwise downwards and upwards open circle arrows
    HOT_BEVERAGE("☕"),
    HARD_DRIVER("\uD83D\uDDB4"),
    OPTICAL_DISC("\uD83D\uDCBF"),
    BUG("\uD83D\uDC1B"),
    SKULL_CROSSBONES("☠"),
    HUNDRED_POINTS("\uD83D\uDCAF"),
    BROKEN_HEART("\uD83D\uDC94"),
    RACING_CAR("\uD83C\uDFCE"),
    DELIVERY_TRUCK("\uD83D\uDE9A"),
    WARNING("⚠"),
    WHITE_HEAVY_CHECK("✅"),
    CHECK_MARK("✓"),
    HEAVY_MULTIPLICATION_X("✖"),
    BALLOT_X("✗"),
    FENCER("\uD83E\uDD3A"),
    FLEXED_BICEPS("\uD83D\uDCAA")
    ;

    override fun toString() = value
}
