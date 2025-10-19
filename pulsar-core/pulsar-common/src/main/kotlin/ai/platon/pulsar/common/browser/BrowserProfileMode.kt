package ai.platon.pulsar.common.browser

enum class BrowserProfileMode {
    DEFAULT,
    SYSTEM_DEFAULT,
    PROTOTYPE,
    SEQUENTIAL,
    TEMPORARY;

    companion object {
        @JvmStatic
        fun fromString(name: String?): BrowserProfileMode {
            return if (name.isNullOrEmpty()) {
                DEFAULT
            } else try {
                BrowserProfileMode.valueOf(name.uppercase())
            } catch (e: Throwable) {
                DEFAULT
            }
        }
    }
}
