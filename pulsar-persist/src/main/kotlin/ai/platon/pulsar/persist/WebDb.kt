package ai.platon.pulsar.persist

import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.url.Urls
import ai.platon.pulsar.common.url.Urls.reverseUrlOrNull
import ai.platon.pulsar.common.config.AppConstants.UNICODE_LAST_CODE_POINT
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.gora.db.DbIterator
import ai.platon.pulsar.persist.gora.db.DbQuery
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.commons.collections4.CollectionUtils
import org.apache.gora.filter.Filter
import org.apache.gora.store.DataStore
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicBoolean

class WebDb(val conf: ImmutableConfig): AutoCloseable {

    private val log = LoggerFactory.getLogger(WebDb::class.java)
    private val tracer = log.takeIf { it.isTraceEnabled }
    private val closed = AtomicBoolean()

    val store: DataStore<String, GWebPage> by lazy { AutoDetectStorageProvider(conf).createPageStore() }
    val schemaName: String get() = store.schemaName

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param originalUrl the original url of the page, it comes from user input, web page parsing, etc
     * @param fields the fields required in the WebPage. Pass null, to retrieve all fields
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    @JvmOverloads
    fun getOrNull(originalUrl: String, ignoreQuery: Boolean = false, fields: Array<String>? = null): WebPage? {
        val (url, key) = Urls.normalizedUrlAndKey(originalUrl, ignoreQuery)

        tracer?.trace("Getting $key")

        val page = fields?.let { store.get(key, it) } ?: store.get(key)
        if (page != null) {
            tracer?.trace("Got $key")
            return WebPage.box(url, key, page)
        }

        return null
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param originalUrl the original address of the page
     * @return the WebPage corresponding to the key or WebPage.NIL if it cannot be found
     */
    @JvmOverloads
    fun get(originalUrl: String, ignoreQuery: Boolean = false, fields: Array<String>? = null): WebPage {
        val (url, key) = Urls.normalizedUrlAndKey(originalUrl, ignoreQuery)

        val page = getOrNull(url, ignoreQuery, null)
        return page ?: WebPage.NIL
    }

    @JvmOverloads
    fun put(page: WebPage, replaceIfExists: Boolean = false) = putInternal(page, replaceIfExists)

    /**
     * Notice:
     * There are comments in gora-hbase-0.6.1, HBaseStore.java, line 259:
     * "HBase sometimes does not delete arbitrarily"
     */
    private fun putInternal(page: WebPage, replaceIfExists: Boolean): Boolean {
        // Never update NIL page
        if (page.isNil) {
            return false
        }

        val key = page.reversedUrl
        if (key.isEmpty()) {
            return false
        }

        if (replaceIfExists) {
            store.delete(key)
        }

        tracer?.trace("Putting $key")

        store.put(key, page.unbox())
        return true
    }

    fun putAll(pages: Iterable<WebPage>) = pages.forEach { put(it, false) }

    @JvmOverloads
    fun delete(originalUrl: String, ignoreQuery: Boolean = false): Boolean {
        val (url, key) = Urls.normalizedUrlAndKey(originalUrl, ignoreQuery)

        return if (key.isNotEmpty()) {
            store.delete(key)
            return true
        } else false
    }

    @JvmOverloads
    fun truncate(force: Boolean = false): Boolean {
        val schemaName = store.schemaName
        if (force) {
            store.truncateSchema()
            log.info("Schema $schemaName is truncated")
            return true
        }

        return if (schemaName.startsWith("tmp_") || schemaName.endsWith("_tmp_webpage")) {
            store.truncateSchema()
            log.info("Schema $schemaName is truncated")
            true
        } else {
            log.info("Only schema name starts with tmp_ or ends with _tmp_webpage " +
                    "can be truncated using this API, bug got $schemaName")
            false
        }
    }

    /**
     * Scan all pages who's url starts with {@param originalUrl}
     *
     * @param originalUrl The base url
     * @return The iterator to retrieve pages
     */
    fun scan(urlBase: String): Iterator<WebPage> {
        val query = store.newQuery()
        query.setKeyRange(reverseUrlOrNull(urlBase), reverseUrlOrNull(urlBase + UNICODE_LAST_CODE_POINT))

//        val filter = SingleFieldValueFilter<String, GWebPage>()
//        query.filter = filter

        val result = store.execute(query)
        return DbIterator(result)
    }

    /**
     * Scan all pages who's url starts with {@param originalUrl}
     *
     * @param originalUrl The base url
     * @return The iterator to retrieve pages
     */
    fun scan(originalUrl: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage> {
        return scan(originalUrl, fields.map { it.toString() }.toTypedArray())
    }

    /**
     * Scan all pages who's url starts with {@param originalUrl}
     *
     * @param originalUrl The base url
     * @return The iterator to retrieve pages
     */
    fun scan(originalUrl: String, fields: Array<String>): Iterator<WebPage> {
        val query = store.newQuery()
        query.setKeyRange(reverseUrlOrNull(originalUrl), reverseUrlOrNull(originalUrl + UNICODE_LAST_CODE_POINT))
        query.setFields(*fields)

        val result = store.execute(query)
        return DbIterator(result)
    }

    /**
     * Scan all pages who's url starts with {@param originalUrl}
     *
     * @param originalUrl The base url
     * @return The iterator to retrieve pages
     */
    fun scan(originalUrl: String, fields: Array<String>, filter: Filter<String, GWebPage>): Iterator<WebPage> {
        val query = store.newQuery()

        query.filter = filter
        query.setKeyRange(reverseUrlOrNull(originalUrl), reverseUrlOrNull(originalUrl + UNICODE_LAST_CODE_POINT))
        query.setFields(*fields)

        val result = store.execute(query)
        return DbIterator(result)
    }

    /**
     * Scan all pages matches the {@param query}
     *
     * @param query The query
     * @return The iterator to retrieve pages
     */
    fun query(query: DbQuery): Iterator<WebPage> {
        val goraQuery = store.newQuery()

        val startKey = query.startUrl?.let { reverseUrlOrNull(it) }
        var endKey = query.endUrl?.let { reverseUrlOrNull(it) }

        // The placeholder is used to mark the last character, it's required for serialization, especially for json format
        if (endKey != null) {
            endKey = endKey.replace("\\uFFFF".toRegex(), UNICODE_LAST_CODE_POINT.toString())
            endKey = endKey.replace("\\\\uFFFF".toRegex(), UNICODE_LAST_CODE_POINT.toString())
        }

        goraQuery.startKey = startKey
        goraQuery.endKey = endKey

        val fields = prepareFields(query.fields)
        goraQuery.setFields(*fields)
        val result = store.execute(goraQuery)

        return DbIterator(result)
    }

    fun flush() {
        try {
            store.flush()
        } catch (e: IllegalStateException) {
            log.warn(e.message)
        } catch (e: Throwable) {
            // TODO: Embedded MongoDB fails to shutdown gracefully #5487
            // see https://github.com/spring-projects/spring-boot/issues/5487
            log.error(Strings.stringifyException(e))
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            flush()
            store.close()
        }
    }

    private fun prepareFields(fields: MutableSet<String>): Array<String> {
        if (CollectionUtils.isEmpty(fields)) {
            return GWebPage._ALL_FIELDS
        }
        fields.remove("url")
        return fields.toTypedArray()
    }
}
