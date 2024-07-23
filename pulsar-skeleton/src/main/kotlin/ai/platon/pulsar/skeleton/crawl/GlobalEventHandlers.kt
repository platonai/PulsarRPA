package ai.platon.pulsar.skeleton.crawl

/**
 * The global event handlers.
 * */
object GlobalEventHandlers {
    /**
     * The page event handlers.
     *
     * Calling rules:
     * 1. preprocessors will be called before the page specific handlers.
     * 2. postprocessors will be called after the page specific handlers.
     * 3. if the event handler is neither a preprocessor nor a postprocessor, the calling order is undefined.
     * */
    val pageEventHandlers: PageEventHandlers? = null
}
