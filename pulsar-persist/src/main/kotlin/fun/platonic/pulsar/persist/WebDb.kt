package `fun`.platonic.pulsar.persist

import `fun`.platonic.pulsar.common.UrlUtil
import `fun`.platonic.pulsar.common.UrlUtil.reverseUrlOrNull
import `fun`.platonic.pulsar.common.config.ImmutableConfig
import `fun`.platonic.pulsar.common.config.PulsarConstants.UNICODE_LAST_CODE_POINT
import `fun`.platonic.pulsar.persist.gora.db.DbIterator
import `fun`.platonic.pulsar.persist.gora.db.DbQuery
import `fun`.platonic.pulsar.persist.gora.generated.GWebPage
import org.apache.commons.collections4.CollectionUtils
import org.apache.gora.store.DataStore
import org.slf4j.LoggerFactory
import java.util.*

class WebDb(
        val conf: ImmutableConfig,
        val storeService: AutoDetectedStorageService
): AutoCloseable {

    val log = LoggerFactory.getLogger(WebDb::class.java)

    val store: DataStore<String, GWebPage> get() = storeService.pageStore
    val schemaName: String get() = store.schemaName

    // required by Jvm language
    constructor(conf: ImmutableConfig): this(conf, AutoDetectedStorageService(conf))

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param url    the url of the WebPage
     * @param fields the fields required in the WebPage. Pass null, to retrieve all fields
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    @JvmOverloads
    operator fun get(url: String, fields: Array<String>? = null): WebPage? {
        Objects.requireNonNull(url)
        val key = UrlUtil.reverseUrlOrEmpty(url)

        if (log.isTraceEnabled) {
            log.trace("Getting $key")
        }
        val goraWebPage = store.get(key, fields)
        if (goraWebPage != null) {
            if (log.isTraceEnabled) {
                log.trace("Got $key")
            }

            return WebPage.box(url, key, goraWebPage)
        }

        return null
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param url the url of the WebPage
     * @return the WebPage corresponding to the key or WebPage.NIL if it cannot be found
     */
    @JvmOverloads
    fun getOrNil(url: String, fields: Array<String>? = null): WebPage {
        val page = get(url, null)
        return page ?: WebPage.NIL
    }

    @JvmOverloads
    fun put(url: String, page: WebPage, replaceIfExists: Boolean = false): Boolean {
        if (url != page.url) {
            log.warn("Url and page.getUrl() does not match. {} <-> {}", url, page.url)
        }

        return put(page, replaceIfExists)
    }

    /**
     * Notice:
     * There are comments in gora-hbase-0.6.1, HBaseStore.java, line 259:
     * "HBase sometimes does not delete arbitrarily"
     */
    private fun put(page: WebPage, replaceIfExists: Boolean): Boolean {
        Objects.requireNonNull(page)

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

        if (log.isTraceEnabled) {
            log.trace("Putting $key")
        }

        store.put(key, page.unbox())
        return true
    }

    fun putAll(pages: Iterable<WebPage>) {
        pages.forEach { page -> put(page.url, page) }
    }

    fun delete(url: String): Boolean {
        val reversedUrl = reverseUrlOrNull(url)
        return if (reversedUrl != null) {
            store.delete(reversedUrl)
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

        if (schemaName.startsWith("tmp_") || schemaName.endsWith("_tmp_webpage")) {
            store.truncateSchema()
            log.info("Schema $schemaName is truncated")
            return true
        } else {
            log.info("Only schema name starts with tmp_ or ends with _tmp_webpage can be truncated using this API, " +
                    "bug got " + schemaName)
            return false
        }
    }

    /**
     * Scan all pages who's url starts with {@param baseUrl}
     *
     * @param baseUrl The base url
     * @return The iterator to retrieve pages
     */
    fun scan(baseUrl: String): Iterator<WebPage> {
        val query = store.newQuery()
        query.setKeyRange(reverseUrlOrNull(baseUrl), reverseUrlOrNull(baseUrl + UNICODE_LAST_CODE_POINT))

        val result = store.execute(query)
        return DbIterator(result)
    }

    /**
     * Scan all pages who's url starts with {@param baseUrl}
     *
     * @param baseUrl The base url
     * @return The iterator to retrieve pages
     */
    fun scan(baseUrl: String, fields: Array<String>): Iterator<WebPage> {
        val query = store.newQuery()
        query.setKeyRange(reverseUrlOrNull(baseUrl), reverseUrlOrNull(baseUrl + UNICODE_LAST_CODE_POINT))
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

        val startKey: String? = reverseUrlOrNull(query.startUrl)
        var endKey: String? = reverseUrlOrNull(query.endUrl)

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
        } catch (e: Throwable) {
            // TODO: Embedded MongoDB fails to shutdown gracefully #5487
            // see https://github.com/spring-projects/spring-boot/issues/5487
            log.error(e.message)
        }
    }

    override fun close() {
        flush()
    }

    private fun prepareFields(fields: MutableSet<String>): Array<String> {
        if (CollectionUtils.isEmpty(fields)) {
            return GWebPage._ALL_FIELDS
        }
        fields.remove("url")
        return fields.toTypedArray()
    }
}
