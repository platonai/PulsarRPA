package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.Variables

/**
 * The core web page structure.
 *
 * This class uses the Delegation pattern to wrap a [KWebAsset] object and add some extra properties.
 *
 * About delegate properties, see: https://kotlinlang.org/docs/delegation.html
 * The Delegation pattern has proven to be a good alternative to implementation inheritance, and Kotlin supports it
 * natively requiring zero boilerplate code.
 *
 * @param page the page to be wrapped
 */
open class KWebPage(
    page: KWebAsset
) : KWebAsset by page, WebAssetState {
    /**
     * If this page is fetched from internet
     */
    override var isCached = false
    
    /**
     * If this page is loaded from database or is created and fetched from the web
     */
    override var isLoaded = false
    
    /**
     * If this page is fetched from internet
     */
    override var isFetched = false
    /**
     * If a page is canceled, it remains unchanged
     */
    /**
     * If a page is canceled, it remains unchanged
     */
    /**
     * If this page is canceled
     */
    override var isCanceled = false
    
    /**
     * If this page is fetched and updated
     */
    override val isContentUpdated = false
    
    val variables: Variables = Variables()
    
    var conf: VolatileConfig? = null

    /**
     * Check if the page scope temporary variable with name {@name} exist
     *
     * @param name The variable name to check
     * @return true if the variable exist
     */
    fun hasVal(name: String) = variables.contains(name)
    
    /**
     * Get a page scope temporary variable
     *
     * @param name a [String] object.
     * @return a Object or null.
     */
    fun getVar(name: String) = variables[name]
}
