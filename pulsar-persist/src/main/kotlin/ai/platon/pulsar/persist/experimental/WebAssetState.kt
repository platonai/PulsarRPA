package ai.platon.pulsar.persist.experimental

/**
 * The core web page structure
 */
interface WebAssetState {

    /**
     * If this page is fetched from internet
     */
    val isCached: Boolean

    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    val isLoaded: Boolean

    /**
     * If this page is fetched from internet
     */
    val isFetched: Boolean
    /**
     * If a page is canceled, it remains unchanged
     */
    /**
     * If a page is canceled, it remains unchanged
     */
    /**
     * If this page is canceled
     */
    val isCanceled: Boolean

    /**
     * If this page is fetched and updated
     */
    val isContentUpdated: Boolean
}
