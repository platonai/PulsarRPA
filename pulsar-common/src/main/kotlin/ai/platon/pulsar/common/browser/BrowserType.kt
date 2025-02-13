package ai.platon.pulsar.common.browser

import java.util.*

/**
 * The supported browser types
 */
enum class BrowserType {
    /**
     * Every request will be sent via raw http client.
     * */
    NATIVE,
    /**
     * The main browser type available.
     * */
    PULSAR_CHROME,
    @Deprecated("No mock chrome available")
    MOCK_CHROME,
    /**
     * Not implemented yet.
     * */
    PLAYWRIGHT_CHROME;

//    override fun toString(): String {
//        return name.lowercase(Locale.getDefault())
//    }

    companion object {
        /**
         * Create a browser type from a string.
         *
         * @param name the name of the browser type.
         * @return the BrowserType.
         */
        @JvmStatic
        fun fromString(name: String?): BrowserType {
            return if (name.isNullOrEmpty()) {
                PULSAR_CHROME
            } else try {
                valueOf(name.uppercase(Locale.getDefault()))
            } catch (e: Throwable) {
                PULSAR_CHROME
            }
        }
    }
}
