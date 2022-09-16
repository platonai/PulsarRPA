package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.persist.WebPage
import java.time.Duration

interface Crawler: AutoCloseable {
    /**
     * The crawl id
     * */
    val id: Int
    /**
     * The crawl name
     * */
    val name: String
    /**
     * The default load options
     * */
    @Deprecated("No need to set default options")
    val defaultOptions: LoadOptions
    /**
     * The default load arguments
     * */
    @Deprecated("No need to set default args")
    val defaultArgs: String get() = defaultOptions.toString()
    /**
     * Delay policy for retry tasks
     * */
    var retryDelayPolicy: (Int, UrlAware?) -> Duration
    /**
     * Await for all tasks be done
     * */
    fun await()

    fun onWillLoad(handler: (UrlAware) -> Unit)

    fun offWillLoad(handler: (UrlAware) -> Unit)

    fun onLoaded(handler: (UrlAware, WebPage) -> Unit)

    fun offLoaded(handler: (UrlAware, WebPage) -> Unit)
}
