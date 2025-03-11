package ai.platon.pulsar.persist

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.AppConstants.UNICODE_LAST_CODE_POINT
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.urls.UrlUtils.reverseUrlOrNull
import ai.platon.pulsar.persist.gora.db.DbIterator
import ai.platon.pulsar.persist.gora.db.DbQuery
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.filter.Filter
import org.apache.gora.filter.FilterOp
import org.apache.gora.filter.SingleFieldValueFilter
import org.apache.gora.store.DataStore
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * A simple interface to query and store web pages.
 * */
class WebDb(
    val conf: ImmutableConfig,
): AutoCloseable {
    companion object {
        val dbGetCount = AtomicLong()
        val accumulateGetNanos = AtomicLong()
        val dbContinousFailureCount = AtomicLong()
        val dbGetAveMillis get() = TimeUnit.MILLISECONDS.convert(
            accumulateGetNanos.get(),  TimeUnit.NANOSECONDS) / dbGetCount.get().coerceAtLeast(1)

        val dbPutCount = AtomicLong()
        val accumulatePutNanos = AtomicLong()
        val dbPutAveMillis get() = TimeUnit.MILLISECONDS.convert(
            accumulatePutNanos.get(),  TimeUnit.NANOSECONDS) / dbPutCount.get().coerceAtLeast(1)
    }

    private val logger = LoggerFactory.getLogger(WebDb::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }
    private val closed = AtomicBoolean()

    var specifiedDataStore: DataStore<String, GWebPage>? = null

    private val dataStoreDelegate = lazy { specifiedDataStore ?: AutoDetectStorageProvider(conf).createPageStore() }

    val dataStore: DataStore<String, GWebPage> by dataStoreDelegate
    val dataStoreOrNull: DataStore<String, GWebPage>? get() = if (dataStoreDelegate.isInitialized()) dataStore else null
    val schemaName: String get() = dataStoreOrNull?.schemaName?:"(unknown, not initialized)"

    /**
     * Test if the WebDB can be connected.
     * @return true if the WebDB can be connected.
     * */
    fun canConnect() = dataStore.runCatching { schemaExists() }.isSuccess

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param originalUrl the original url of the page, it comes from user input, webpage parsing, etc
     * @param field the field required in the WebPage.
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    @Throws(WebDBException::class)
    fun getOrNull(originalUrl: String, field: GWebPage.Field): WebPage? {
        return getOrNull(originalUrl, field.toString())
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param originalUrl the original url of the page, it comes from user input, webpage parsing, etc
     * @param fields the fields required in the WebPage. Pass null to retrieve all fields
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    @Throws(WebDBException::class)
    fun getOrNull(originalUrl: String, fields: Iterable<GWebPage.Field>): WebPage? {
        return getOrNull(originalUrl, false, fields.map { it.toString() }.toTypedArray())
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param originalUrl the original url of the page, it comes from user input, webpage parsing, etc
     * @param field the fields required in the WebPage. Pass null to retrieve all fields
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    @Throws(WebDBException::class)
    fun getOrNull(originalUrl: String, field: String): WebPage? {
        return getOrNull(originalUrl, false, arrayOf(field))
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param originalUrl the original url of the page, it comes from user input, webpage parsing, etc
     * @param fields the fields required in the WebPage. Pass null to retrieve all fields
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    @Throws(WebDBException::class)
    fun getOrNull(originalUrl: String, norm: Boolean = false, fields: Array<String>? = null): WebPage? {
        // TODO: consider the design again whether we need normalize the url here
        val (url, key) = UrlUtils.normalizedUrlAndKey(originalUrl, norm)

        val page = getOrNull0(originalUrl, norm, fields)

        if (page != null) {
            val p = WebPage.box(url, key, page, conf.toVolatileConfig()).also { it.isLoaded = true }
            tracer?.trace("Got {} {} {} {}", p.fetchCount, p.prevFetchTime, p.fetchTime, key)
            return p
        }

        return null
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param originalUrl the original address of the page
     * @return the WebPage corresponding to the key or [WebPage.NIL] if it cannot be found
     */
    @Throws(WebDBException::class)
    fun get(originalUrl: String, field: GWebPage.Field) = getOrNull(originalUrl, field) ?: WebPage.NIL

    @Throws(WebDBException::class)
    fun get(originalUrl: String, fields: Iterable<GWebPage.Field>) =
        getOrNull(originalUrl, fields) ?: WebPage.NIL

    @Throws(WebDBException::class)
    fun get(originalUrl: String, field: String) = getOrNull(originalUrl, field) ?: WebPage.NIL

    @Throws(WebDBException::class)
    fun get(originalUrl: String, norm: Boolean = false, fields: Array<String>? = null): WebPage {
        return getOrNull(originalUrl, norm, fields) ?: WebPage.NIL
    }

    @Throws(WebDBException::class)
    fun get0(originalUrl: String, norm: Boolean = false, fields: Array<String>? = null): GWebPage? {
        return getOrNull0(originalUrl, norm, fields)
    }

    @Throws(WebDBException::class)
    fun exists(originalUrl: String, norm: Boolean = false): Boolean {
//        val key = reverseUrlOrNull(originalUrl)
//        return dataStore.exists(key)

        val requiredField = GWebPage.Field.CREATE_TIME.toString()
        return getOrNull(originalUrl, norm, arrayOf(requiredField)) != null
    }

    @Throws(WebDBException::class)
    fun getContent(originalUrl: String): ByteBuffer? {
        val fields = arrayOf(GWebPage.Field.CONTENT.toString())
        return getOrNull0(originalUrl, false, fields)?.content
    }

    @Throws(WebDBException::class)
    fun getContentAsString(originalUrl: String): String? {
        val buffer = getContent(originalUrl) ?: return null

        return when {
            buffer.remaining() == 0 -> ""
            else -> String(buffer.array(), buffer.arrayOffset(), buffer.limit())
        }
    }

    @Throws(WebDBException::class)
    @JvmOverloads
    fun put(page: WebPage, replaceIfExists: Boolean = false) = putInternal(page, replaceIfExists)

    @Throws(WebDBException::class)
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
            performDSAction("put") { dataStore.delete(key) }
        }
        
        tracer?.trace("Putting {} {} {} {}", page.fetchCount, page.prevFetchTime, page.fetchTime, key)

        val startTime = System.nanoTime()
        performDSAction("put") { dataStore.put(key, page.unbox()) }
        dbPutCount.incrementAndGet()
        accumulatePutNanos.addAndGet(System.nanoTime() - startTime)

        return true
    }

    @Throws(WebDBException::class)
    fun putAll(pages: Iterable<WebPage>) = pages.forEach { put(it, false) }

    @JvmOverloads
    @Throws(WebDBException::class)
    fun delete(originalUrl: String, norm: Boolean = false): Boolean {
        val (_, key) = UrlUtils.normalizedUrlAndKey(originalUrl, norm)
        if (key.isBlank()) {
            return false
        }

        return performDSAction("delete", originalUrl) { dataStore.delete(key) }
    }

    @JvmOverloads
    @Throws(WebDBException::class)
    fun truncate(force: Boolean = false): Boolean {
        val schemaName = dataStore.schemaName
        if (force) {
            performDSAction("truncate") { dataStore.truncateSchema() }
            logger.info("Schema $schemaName is truncated")
            return true
        }

        return if (schemaName.startsWith("tmp_") || schemaName.endsWith("_tmp_webpage")) {
            performDSAction("truncate") { dataStore.truncateSchema() }
            logger.info("Schema $schemaName is truncated")
            true
        } else {
            logger.info("Only schema name starts with tmp_ or ends with _tmp_webpage " +
                    "can be truncated using this API")
            false
        }
    }

    /**
     * Scan all pages whose url starts with {@param urlBase}
     *
     * @param urlBase The base url to start with
     * @return The iterator to retrieve pages
     */
    @Throws(WebDBException::class)
    fun scan(urlBase: String): Iterator<WebPage> {
        val query = dataStore.newQuery()
        // TODO: key range does not working in MongoStore
        query.setKeyRange(reverseUrlOrNull(urlBase), reverseUrlOrNull(urlBase + UNICODE_LAST_CODE_POINT))

        val result = dataStore.execute(query)
        return DbIterator(result, conf)
    }

    /**
     * Scan all pages whose url starts with {@param urlBase}
     *
     * @param urlBase The base url to start with
     * @return The iterator to retrieve pages
     */
    @Throws(WebDBException::class)
    fun scan(urlBase: String, fields: Iterable<GWebPage.Field>): Iterator<WebPage> {
        return scan(urlBase, fields.map { it.toString() }.toTypedArray())
    }

    /**
     * Scan all pages whose url starts with {@param urlBase}
     *
     * @param urlBase The base url to start with
     * @return The iterator to retrieve pages
     */
    @Throws(WebDBException::class)
    fun scan(urlBase: String, fields: Array<String>): Iterator<WebPage> {
        val query = dataStore.newQuery()
        // TODO: key range does not working in MongoStore
        query.setKeyRange(reverseUrlOrNull(urlBase), reverseUrlOrNull(urlBase + UNICODE_LAST_CODE_POINT))
        query.setFields(*fields)

        val result = dataStore.execute(query)
        return DbIterator(result, conf)
    }

    /**
     * Scan all pages whose url starts with {@param urlBase}
     *
     * @param urlBase The base url to start with
     * @return The iterator to retrieve pages
     */
    @Throws(WebDBException::class)
    fun scan(urlBase: String, fields: Array<String>, filter: Filter<String, GWebPage>): Iterator<WebPage> {
        val query = dataStore.newQuery()

        query.filter = filter
        // TODO: key range does not working in MongoStore
        query.setKeyRange(reverseUrlOrNull(urlBase), reverseUrlOrNull(urlBase + UNICODE_LAST_CODE_POINT))
        query.setFields(*fields)

        val result = dataStore.execute(query)
        return DbIterator(result, conf)
    }

    /**
     * Scan all pages matches the {@param query}.
     *
     * @param query The query
     * @return The iterator to retrieve pages
     */
    @Throws(WebDBException::class)
    fun query(query: DbQuery): Iterator<WebPage> {
        val goraQuery = dataStore.newQuery()

        val startKey = query.startUrl?.let { reverseUrlOrNull(it) }
        var endKey = query.endUrl?.let { reverseUrlOrNull(it) }

        // The placeholder is used to mark the last character, it's required for serialization, especially for json format
        if (endKey != null) {
            endKey = endKey.replace("\\uFFFF".toRegex(), UNICODE_LAST_CODE_POINT.toString())
            endKey = endKey.replace("\\\\uFFFF".toRegex(), UNICODE_LAST_CODE_POINT.toString())
        }

        // TODO: key range does not working in MongoStore
        goraQuery.startKey = startKey
        goraQuery.endKey = endKey
        val batchId = query.batchId
        if (batchId == null && query.filterNullBatchId) {
            goraQuery.filter = createBatchIdFilter(query.batchId, query.filterIfMissing)
        } else if (batchId != null) {
            goraQuery.filter = createBatchIdFilter(query.batchId, query.filterIfMissing)
        }

        goraQuery.setFields(*prepareFields(query.fields))

        val result = performDSAction("query") { dataStore.execute(goraQuery) }

        return DbIterator(result, conf)
    }

    @Throws(WebDBException::class)
    fun flush() {
        if (!dataStoreDelegate.isInitialized()) {
            return
        }

        try {
            performDSAction("flush") { dataStore.flush() }
        } catch (e: IllegalStateException) {
            logger.warn(e.message)
        } catch (e: Exception) {
            // TODO: Embedded MongoDB fails to shutdown gracefully #5487
            // see https://github.com/spring-projects/spring-boot/issues/5487
            logger.error(e.stringify())
            throw WebDBException("Failed to flush", e)
        }
    }

    @Throws(WebDBException::class)
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (dataStoreDelegate.isInitialized()) {
                // flush()
                // Note: mongo store does not close actually
                performDSAction("close") { dataStore.close() }
            }
            // GoraStorage.close()
        }
    }

    /**
     * Returns the WebPage corresponding to the given url.
     *
     * @param originalUrl the original url of the page, it comes from user input, webpage parsing, etc
     * @param fields the fields required in the WebPage. Pass null to retrieve all fields
     * @return the WebPage corresponding to the key or null if it cannot be found
     */
    @Throws(WebDBException::class)
    private fun getOrNull0(originalUrl: String, norm: Boolean = false, fields: Array<String>? = null): GWebPage? {
        val (_, key) = UrlUtils.normalizedUrlAndKey(originalUrl, norm)

        tracer?.trace("Getting $key")

        val startTime = System.nanoTime()

        val page = performDSAction("get", originalUrl) {
            fields?.let { dataStore.get(key, it) } ?: dataStore.get(key)
        }

        dbGetCount.incrementAndGet()
        accumulateGetNanos.addAndGet(System.nanoTime() - startTime)

        return page
    }

    private fun createBatchIdFilter(
        batchId: CharSequence?, filterIfMissing: Boolean = false
    ): SingleFieldValueFilter<String, GWebPage> {
        return SingleFieldValueFilter<String, GWebPage>().also {
            it.fieldName = GWebPage.Field.BATCH_ID.toString()
            it.filterOp = FilterOp.EQUALS
            if (batchId != null) {
                it.operands = listOf(batchId)
            } else {
                it.operands = listOf(null)
            }
            it.isFilterIfMissing = filterIfMissing
        }
    }

    private fun prepareFields(fields: MutableSet<String>): Array<String> {
        if (fields.isEmpty()) {
            return GWebPage._ALL_FIELDS
        }

        fields.remove("url")
        return fields.toTypedArray()
    }
    
    @Throws(WebDBException::class)
    private fun <T : Any> performDSAction(name: String, url: String? = null, action: () -> T): T {
//        if (!AppContext.isActive) {
//            throw IllegalApplicationContextStateException("")
//        }

        try {
            return action().also { dbContinousFailureCount.decrementAndGet() }
        } catch (e: Exception) {
            var message = "Data storage failure | [$name]"
            if (url.isNullOrBlank()) {
                message = "$message | $url"
            }

            dbContinousFailureCount.incrementAndGet()
            if (dbContinousFailureCount.get() < 5) {
                logger.warn(e.stringify("$message - "))
            } else {
                logger.warn(e.brief("$message - "))
            }

            throw WebDBException(message, e)
        }
    }
}
