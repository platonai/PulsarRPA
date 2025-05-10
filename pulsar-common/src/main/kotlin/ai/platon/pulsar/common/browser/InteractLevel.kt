package ai.platon.pulsar.common.browser

/**
 * Defines the level of interaction with a webpage during crawling.
 *
 * Higher levels involve more interaction (e.g., scrolling, clicking),
 * which may improve content extraction quality at the cost of speed.
 */
enum class InteractLevel(val description: String) {

    /**
     * Minimal interaction for maximum speed.
     * Suitable for fast but shallow scraping.
     */
    FASTEST("Minimal interaction, maximum speed"),

    /**
     * Slightly more interaction than FASTEST, still optimized for speed.
     */
    FASTER("Slight interaction, very fast"),

    /**
     * Balanced for speed-focused use cases.
     */
    FAST("Moderate interaction, fast"),

    /**
     * Default level: balanced between speed and data completeness.
     */
    DEFAULT("Balanced (default setting)"),

    /**
     * Prioritizes data quality over speed.
     */
    GOOD_DATA("Improved data completeness"),

    /**
     * Higher interaction level for better content extraction.
     */
    BETTER_DATA("Better content extraction"),

    /**
     * Maximum interaction for most comprehensive data extraction.
     */
    BEST_DATA("Full interaction, best data quality");

    companion object {
        /**
         * Parses a string to an InteractLevel, falling back to DEFAULT if invalid or empty.
         */
        fun from(value: String?): InteractLevel {
            return value?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { valueOf(it.uppercase()) }.getOrNull() }
                ?: DEFAULT
        }
    }
}