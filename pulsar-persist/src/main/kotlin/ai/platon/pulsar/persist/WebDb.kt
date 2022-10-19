package ai.platon.pulsar.persist

import ai.platon.pulsar.common.config.AppConstants.UNICODE_LAST_CODE_POINT
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.urls.UrlUtils.reverseUrlOrNull
import ai.platon.pulsar.persist.gora.db.DbIterator
import ai.platon.pulsar.persist.gora.db.DbQuery
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.commons.collections4.CollectionUtils
import org.apache.gora.filter.Filter
import org.apache.gora.store.DataStore
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class WebDb(
    val conf: ImmutableConfig
): AutoCloseable {
    companion object {
        val dbGetCount = AtomicLong()
        val accumulateGetNanos = AtomicLong()
        val dbGetAveMillis get() = TimeUnit.MILLISECONDS.convert(
            accumulateGetNanos.get(),  TimeUnit.NANOSECONDS) / dbGetCount.get().coerceAtLeast(1)

        val dbPutCount = AtomicLong()
        val accumulatePutNanos = AtomicLong()
        val dbPutAveMillis get() = TimeUnit.MILLISECONDS.convert(
            accumulatePutNanos.get(),  TimeUnit.NANOSECONDS) / dbPutCount.get().coerceAtLeast(1)
    }

    private val log = LoggerFactory.getLogger(WebDb::class.java)
    private val tracer = log.takeIf { it.isTraceEnabled }
    private val closed = AtomicBoolean()

    var specifiedDataStore: DataStore<String, GWebPage>? = null

    private val dataStoreDelegate = lazy { specifiedDataStore ?: AutoDetectStorageProvider(conf).createPageStore() }

    val dataStore: DataStore<String, GWebPage> by dataStoreDelegate
    val dataStoreOrNull: DataStore<String, GWebPage>? get() = if (dataStoreDelegate.isInitialized()) dataStore else null
    val schemaName: String get() = dataStoreOrNull?.schemaName?:"(unknown, not initialized)"

    fun getOrNull(originalUrl: String, field: GWebPage.Field): MutableWebPage? {
        return getOrNull(originalUrl, field.toString())
    }

    fun getOrNull(originalUrl: String, fields: Iterable<GWebPage.Field>): MutableWebPage? {
        return getOrNull(originalUrl, false, fields.map { it.toString() }.toTypedArray())
    }

    fun getOrNull(originalUrl: String, field: String): MutableWebPage? {
        return getOrNull(originalUrl, false, arrayOf(field))
    }

    /**
     * Returns the MutableWebPage corresponding to the given url.
     *
     * @param originalUrl the original url of the page, it comes from user input, web page parsing, etc
     * @param fields the fields required in the MutableWebPage. Pass null, to retrieve all fields
     * @return the MutableWebPage corresponding to the key or null if it cannot be found
     */
    fun getOrNull(originalUrl: String, norm: Boolean = false, fields: Array<String>? = null): MutableWebPage? {
        val (url, key) = UrlUtils.normalizedUrlAndKey(originalUrl, norm)

        tracer?.trace("Getting $key")

        val startTime = System.nanoTime()
        val page = fields?.let { dataStore.get(key, it) } ?: dataStore.get(key)
        dbGetCount.incrementAndGet()
        accumulateGetNanos.addAndGet(System.nanoTime() - startTime)

        if (page != null) {
            val p = MutableWebPage.box(url, key, page, conf.toVolatileConfig()).also { it.isLoaded = true }

            tracer?.trace("Got ${p.fetchCount} ${p.prevFetchTime} ${p.fetchTime} $key")

            return p
        }

        return null
    }

    /**
     * Returns the MutableWebPage corresponding to the given url.
     *
     * @param originalUrl the original address of the page
     * @return the MutableWebPage corresponding to the key or MutableWebPage.NIL if it cannot be found
     */
    fun get(originalUrl: String, field: GWebPage.Field): MutableWebPage =
        getOrNull(originalUrl, field) ?: MutableWebPage.NIL

    fun get(originalUrl: String, fields: Iterable<GWebPage.Field>): MutableWebPage =
        getOrNull(originalUrl, fields) ?: MutableWebPage.NIL

    fun get(originalUrl: String, field: String): MutableWebPage = getOrNull(originalUrl, field) ?: MutableWebPage.NIL

    fun get(originalUrl: String, norm: Boolean = false, fields: Array<String>? = null): MutableWebPage {
        return getOrNull(originalUrl, norm, fields) ?: MutableWebPage.NIL
    }

    fun exists(originalUrl: String, norm: Boolean = false): Boolean {
        val requiredField = GWebPage.Field.CREATE_TIME.toString()
        return getOrNull(originalUrl, norm, arrayOf(requiredField)) != null
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

        if (page !is MutableWebPage) {
            return false
        }

        val key = page.reversedUrl
        if (key.isEmpty()) {
            return false
        }

        if (replaceIfExists) {
            dataStore.delete(key)
        }

        tracer?.trace("Putting ${page.fetchCount} ${page.prevFetchTime} ${page.fetchTime} $key")

        val startTime = System.nanoTime()
        dataStore.put(key, page.unbox())
        dbPutCount.incrementAndGet()
        accumulatePutNanos.addAndGet(System.nanoTime() - startTime)

        return true
    }

    fun putAll(pages: Iterable<MutableWebPage>) = pages.forEach { put(it, false) }

    @JvmOverloads
    fun delete(originalUrl: String, norm: Boolean = false): Boolean {
        val (url, key) = UrlUtils.normalizedUrlAndKey(originalUrl, norm)

        return if (key.isNotEmpty()) {
            dataStore.delete(key)
            return true
        } else false
    }

    @JvmOverloads
    fun truncate(force: Boolean = false): Boolean {
        val schemaName = dataStore.schemaName
        if (force) {
            dataStore.truncateSchema()
            log.info("Schema $schemaName is truncated")
            return true
        }

        return if (schemaName.startsWith("tmp_") || schemaName.endsWith("_tmp_webpage")) {
            dataStore.truncateSchema()
            log.info("Schema $schemaName is truncated")
            true
        } else {
            log.info("Only schema name starts with tmp_ or ends with _tmp_webpage " +
                    "can be truncated using this API, bug got $schemaName")
            false
        }
    }

    /**
     * Scan all pages whose url starts with {@param originalUrl}
     *
     * @param urlBase The base url
     * @return The iterator to retrieve pages
     */
    fun scan(urlBase: String): Iterator<WebPage> {
        val query = dataStore.newQuery()
        query.setKeyRange(reverseUrlOrNull(urlBase), reverseUrlOrNull(urlBase + UNICODE_LAST_CODE_POINT))

        val result = dataStore.execute(query)
        return DbIterator(result, conf)
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
        val query = dataStore.newQuery()
        query.setKeyRange(reverseUrlOrNull(originalUrl), reverseUrlOrNull(originalUrl + UNICODE_LAST_CODE_POINT))
        query.setFields(*fields)

        val result = dataStore.execute(query)
        return DbIterator(result, conf)
    }

    /**
     * Scan all pages who's url starts with {@param originalUrl}
     *
     * @param originalUrl The base url
     * @return The iterator to retrieve pages
     */
    fun scan(originalUrl: String, fields: Array<String>, filter: Filter<String, GWebPage>): Iterator<WebPage> {
        val query = dataStore.newQuery()

        query.filter = filter
        query.setKeyRange(reverseUrlOrNull(originalUrl), reverseUrlOrNull(originalUrl + UNICODE_LAST_CODE_POINT))
        query.setFields(*fields)

        val result = dataStore.execute(query)
        return DbIterator(result, conf)
    }

    /**
     * Scan all pages matches the {@param query}
     *
     * @param query The query
     * @return The iterator to retrieve pages
     */
    fun query(query: DbQuery): Iterator<WebPage> {
        val goraQuery = dataStore.newQuery()

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
        val result = dataStore.execute(goraQuery)

        return DbIterator(result, conf)
    }

    fun flush() {
        if (!dataStoreDelegate.isInitialized()) {
            return
        }

        try {
            dataStore.flush()
        } catch (e: IllegalStateException) {
            log.warn(e.message)
        } catch (e: Throwable) {
            // TODO: Embedded MongoDB fails to shutdown gracefully #5487
            // see https://github.com/spring-projects/spring-boot/issues/5487
            log.error(e.stringify())
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (dataStoreDelegate.isInitialized()) {
                // flush()
                // Note: mongo store does not close actually
                dataStore.close()
            }
            // GoraStorage.close()
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
