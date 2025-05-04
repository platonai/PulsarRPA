package ai.platon.pulsar.common.browser

enum class BrowserContextMode {
    DEFAULT,
    SYSTEM_DEFAULT,
    PROTOTYPE,
    SEQUENTIAL,
    TEMPORARY;

    companion object {
        @JvmStatic
        fun fromString(name: String?): BrowserContextMode {
            return if (name.isNullOrEmpty()) {
                DEFAULT
            } else try {
                BrowserContextMode.valueOf(name.uppercase())
            } catch (e: Throwable) {
                DEFAULT
            }
        }
    }
}
