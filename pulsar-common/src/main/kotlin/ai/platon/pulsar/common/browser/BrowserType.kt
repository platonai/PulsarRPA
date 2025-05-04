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
     * The PulsarRPA's browser implementation which is superfast.
     * */
    PULSAR_CHROME,

    @Deprecated("No mock chrome available")
    MOCK_CHROME,

    /**
     * The playwright's browser implementation.
     * */
    PLAYWRIGHT_CHROME;

//    override fun toString(): String {
//        return name.lowercase(Locale.getDefault())
//    }

    companion object {
        val DEFAULT = PULSAR_CHROME

        /**
         * Create a browser type from a string.
         *
         * @param name the name of the browser type.
         * @return the BrowserType.
         */
        @JvmStatic
        fun fromString(name: String?): BrowserType {
            return if (name.isNullOrEmpty()) {
                DEFAULT
            } else try {
                valueOf(name.uppercase(Locale.getDefault()))
            } catch (e: Throwable) {
                DEFAULT
            }
        }
    }
}
