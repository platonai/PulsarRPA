package ai.platon.pulsar.skeleton.crawl

/**
 * The global event handlers.
 * */
object GlobalEventHandlers {
    /**
     * The page event handlers.
     *
     * The calling order rule:
     *
     * The more specific handlers has the opportunity to override the result of more general handlers.
     * */
    var pageEventHandlers: PageEventHandlers? = null
}
