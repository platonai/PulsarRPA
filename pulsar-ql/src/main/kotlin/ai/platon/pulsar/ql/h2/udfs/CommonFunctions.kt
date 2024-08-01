package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.RegexExtractor
import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.skeleton.crawl.common.URLUtil
import ai.platon.pulsar.common.browser.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.ql.common.ResultSets
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.ql.SQLSession
import ai.platon.pulsar.ql.common.annotation.H2Context
import ai.platon.pulsar.ql.common.annotation.UDFGroup
import ai.platon.pulsar.ql.common.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import ai.platon.pulsar.ql.h2.addColumn
import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.h2.tools.SimpleResultSet
import org.h2.value.*
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Long.MAX_VALUE
import java.sql.Connection
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*

@UDFGroup
object CommonFunctions {

    private val log = LoggerFactory.getLogger(CommonFunctions::class.java)

    private val sqlContext get() = SQLContexts.create()
    private val unmodifiedConfig get() = sqlContext.unmodifiedConfig

    /**
     * Set volatileConfig to the given value
     *
     * TODO: do we need FetchMode any more?
     *
     * @param mode The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setFetchMode(@H2Context conn: Connection, mode: String, ttl: Int): String? {
        var mode0 = mode
        try {
            mode0 = FetchMode.valueOf(mode0.uppercase(Locale.getDefault())).toString()
        } catch (e: Throwable) {
            log.warn("Unknown FetchMode $mode0")
            return null
        }

        log.debug("Set fetch mode to $mode0")
        return getAndSetConf(conn, FETCH_MODE, mode0, ttl)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's unmodified volatileConfig")
    @JvmStatic
    fun unsetFetchMode(@H2Context conn: Connection): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(FETCH_MODE)
    }

    /**
     * Set volatileConfig to the given value
     * @param h2session H2 session
     * @param browser The browser to use
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setBrowser(@H2Context conn: Connection, browser: String, ttl: Int): String? {
        val browserType = BrowserType.fromString(browser)
        if (browserType == BrowserType.NATIVE) {
            unsetFetchMode(conn)
        } else {
            setFetchMode(conn, FetchMode.BROWSER.name, ttl)
        }

        log.debug("Set browser to $browser")
        return getAndSetConf(conn, BROWSER_TYPE, browserType.name, ttl)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's unmodified volatileConfig")
    @JvmStatic
    fun unsetBrowser(@H2Context conn: Connection): String? {
        val session = getSession(conn)
        unsetFetchMode(conn)
        return session.sessionConfig.getAndUnset(BROWSER_TYPE)
    }

    /**
     * Also retrieve faded links whenever get out links of a page
     * @param ttl The property value time to live in session
     */
    @UDFunction
    @JvmStatic
    fun enableFadedLinks(@H2Context conn: Connection, ttl: Int): String? {
        return getAndSetConf(conn, PARSE_RETRIEVE_FADED_LINKS, true.toString(), ttl)
    }

    @UDFunction
    @JvmStatic
    fun disableFadedLinks(@H2Context conn: Connection, ttl: Int): String? {
        return getAndSetConf(conn, PARSE_RETRIEVE_FADED_LINKS, false.toString(), ttl)
    }

    @UDFunction
    @JvmStatic
    fun setEagerFetchLimit(@H2Context conn: Connection, parallel: Boolean, ttl: Int): String? {
        return getAndSetConf(conn, FETCH_CONCURRENCY, parallel.toString(), ttl)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's unmodified volatileConfig")
    @JvmStatic
    fun unsetEagerFetchLimit(@H2Context conn: Connection): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(FETCH_CONCURRENCY)
    }

    /**
     * Set page expires, this volatileConfig affects just in h2session scope
     * SQL examples:
     * `CALL setPageExpires('1d')`
     * `CALL setPageExpires('PT24H')`
     * `CALL setPageExpires('1s')`
     * `CALL setPageExpires('1')`
     *
     * @param h2session  The H2 session, auto injected by h2 runtime
     * @param duration The duration string.
     * Supported formats: 1. number in seconds, 2. ISO-8601 standard and 3. hadoop time duration format
     * ISO-8601 standard : PnDTnHnMn.nS
     * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
     * @param ttl The property value time to live in session
     * @return The old page expires in seconds, or null if failed
     */
    @UDFunction(description = "Set the page expire time with time-to-life of the calling session")
    @JvmStatic
    @JvmOverloads
    fun setPageExpires(@H2Context conn: Connection, duration: String, ttl: Int = 1): String? {
        val d = getDuration(duration)
        val value = d?.toString()
        return getAndSetConf(conn, STORAGE_DATUM_EXPIRES, value, ttl)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's unmodified volatileConfig")
    @JvmStatic
    fun unsetPageExpires(@H2Context conn: Connection): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(STORAGE_DATUM_EXPIRES)
    }

    /**
     * Set volatileConfig to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction(description = "Set the page load timeout with time-to-life of the calling session")
    @JvmStatic
    @JvmOverloads
    fun setPageLoadTimeout(@H2Context conn: Connection, duration: String, ttl: Int = 1): String? {
        val d = getDuration(duration)
        val value = d?.toString()
        return getAndSetConf(conn, FETCH_PAGE_LOAD_TIMEOUT, value, ttl)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's unmodified volatileConfig")
    @JvmStatic
    fun unsetPageLoadTimeout(@H2Context conn: Connection): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(FETCH_PAGE_LOAD_TIMEOUT)
    }

    /**
     * Set volatileConfig to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction(description = "Set the script timeout with time-to-life of the calling session")
    @JvmStatic
    @JvmOverloads
    fun setScriptTimeout(@H2Context conn: Connection, duration: String, ttl: Int = 1): String? {
        val d = getDuration(duration)
        val value = d?.toString()
        return getAndSetConf(conn, FETCH_SCRIPT_TIMEOUT, value, ttl)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's unmodified volatileConfig")
    @JvmStatic
    fun unsetScriptTimeout(@H2Context conn: Connection): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(FETCH_SCRIPT_TIMEOUT)
    }

    /**
     * Set volatileConfig to the given value
     * @param h2session H2 session
     * @param count The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction(description = "Set the scroll down count with time-to-life of the calling session")
    @JvmStatic
    @JvmOverloads
    fun setScrollDownCount(@H2Context conn: Connection, count: Int, ttl: Int = 1): String? {
        return getAndSetConf(conn, FETCH_SCROLL_DOWN_COUNT, count.toString(), ttl)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's initial volatileConfig")
    @JvmStatic
    fun unsetScrollDownCount(@H2Context conn: Connection): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(FETCH_SCROLL_DOWN_COUNT)
    }

    /**
     * Set volatileConfig to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction(description = "Set the scroll interval with time-to-life of the calling session")
    @JvmStatic
    fun setScrollInterval(@H2Context conn: Connection, duration: String, ttl: Int): String? {
        val d = getDuration(duration)
        val value = d?.toString()
        return getAndSetConf(conn, FETCH_SCROLL_DOWN_INTERVAL, value, ttl)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's initial volatileConfig")
    @JvmStatic
    fun setScrollInterval(@H2Context conn: Connection): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(FETCH_SCROLL_DOWN_INTERVAL)
    }

    /**
     * Set h2session scope configuration.
     *
     * SQL examples:
     * `CALL setConf('fetch.page.load.timeout', '1m')`
     * `CALL setConf('fetch.fetch.mode', 'NATIVE')`
     *
     * @param h2session The H2 session, auto injected by h2 runtime
     * @return The old value of the key
     */
    @UDFunction(description = "Set the volatileConfig property associated by name with time-to-life of the calling session")
    @JvmStatic
    @JvmOverloads
    fun setConfig(@H2Context conn: Connection, name: String, value: String, ttl: Int = Integer.MAX_VALUE / 2): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndSet(name, value, ttl)
    }

    @UDFunction(description = "Set the volatileConfig property associated by name with time-to-life of the calling session")
    @JvmStatic
    fun setConf(@H2Context conn: Connection, name: String, value: String, ttl: Int): String? {
        return setConfig(conn, name, value, ttl)
    }

    @UDFunction(description = "Set the volatileConfig property associated by name of the calling session")
    @JvmStatic
    fun setConf(@H2Context conn: Connection, name: String, value: String): String? {
        return setConfig(conn, name, value, Integer.MAX_VALUE / 2)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's initial volatileConfig")
    @JvmStatic
    fun unsetConf(@H2Context conn: Connection, name: String): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(name)
    }

    @UDFunction(description = "Unset the volatileConfig property of the calling session, " +
            "so it fallback to the process's initial volatileConfig")
    @JvmStatic
    fun unsetConfig(@H2Context conn: Connection, name: String): String? {
        val session = getSession(conn)
        return session.sessionConfig.getAndUnset(name)
    }

    @UDFunction(description = "Get the value associated by the given key of the calling session")
    @JvmStatic
    fun getConf(@H2Context conn: Connection, name: String): String? {
        val session = getSession(conn)
        return session.sessionConfig.get(name)
    }

    @UDFunction(description = "Get the value associated by the given key of the calling session")
    @JvmStatic
    fun getConfig(@H2Context conn: Connection, name: String): String? {
        return getConf(conn, name)
    }

    @UDFunction(description = "Get the initial configuration properties of the process")
    @JvmStatic
    fun volatileConfig(): SimpleResultSet {
        val rs = ResultSets.newSimpleResultSet()

        rs.addColumn("NAME")
        rs.addColumn("VALUE")

        for ((key, value) in unmodifiedConfig.unbox()) {
            rs.addRow(key, value)
        }

        return rs
    }

    @UDFunction(description = "Get the configuration properties of the calling session")
    @JvmStatic
    fun sessionConfig(@H2Context conn: Connection): SimpleResultSet {
        val rs = ResultSets.newSimpleResultSet()
        rs.addColumn("NAME")
        rs.addColumn("VALUE")

        val session = getSession(conn)
        for ((key, value) in session.sessionConfig.unbox()) {
            rs.addRow(key, value)
        }

        return rs
    }

    @UDFunction(description = "Get the system info")
    @JvmStatic
    fun sysInfo(@H2Context conn: Connection): SimpleResultSet {
        val rs = ResultSets.newSimpleResultSet()
        rs.addColumn("NAME")
        rs.addColumn("VALUE")

        /* Total number of processors or cores available to the JVM */
        rs.addRow("Available processors (cores)", Runtime.getRuntime().availableProcessors())

        /* Total amount of free memory available to the JVM */
        rs.addRow("Free memory (Mbytes)", Runtime.getRuntime().freeMemory() / 1024 / 1024)

        /* This will return Long.MAX_VALUE if there is no preset limit */
        val maxMemory = Runtime.getRuntime().maxMemory()
        /* Maximum amount of memory the JVM will attempt to use */
        rs.addRow("Maximum memory (Mbytes)", if (maxMemory == MAX_VALUE) "no limit" else java.lang.Long.toString(maxMemory / 1024 / 1024))

        /* Total memory currently available to the JVM */
        rs.addRow("Total memory available to JVM (Mbytes)", Runtime.getRuntime().totalMemory() / 1024 / 1024)

        /* Get a list of all filesystem roots on this system */
        val roots = File.listRoots()
        for (root in roots) {
            /* For each filesystem root, print some info */
            rs.addRow("File system root", root.absolutePath)
            rs.addRow("Total space (Mbytes)", root.totalSpace / 1024 / 1024)
            rs.addRow("Free space (Mbytes)", root.freeSpace / 1024 / 1024)
            rs.addRow("Usable space (Mbytes)", root.usableSpace / 1024 / 1024)
        }

        return rs
    }

    @UDFunction(description = "Test if the given string is a number")
    @JvmStatic
    fun isNumeric(str: String): Boolean {
        return StringUtils.isNumeric(str)
    }

    @UDFunction(description = "Get the domain of a url")
    @JvmStatic
    fun getDomain(url: String): String {
        return URLUtil.getDomainName(url, "")
    }

    @UDFunction(description = "Extract the first group of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re1(text: String, regex: String): String {
        return RegexExtractor().re1(text, regex)
    }

    @UDFunction(description = "Extract the nth group of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re1(text: String, regex: String, group: Int): String {
        return RegexExtractor().re1(text, regex, group)
    }

    @UDFunction(description = "Extract two groups of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re2(text: String, regex: String): ValueArray {
        val result = RegexExtractor().re2(text, regex)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction(description = "Extract two groups(key and value) of the result of java.util.regex.matcher()")
    @JvmStatic
    fun re2(text: String, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        val result = RegexExtractor().re2(text, regex, keyGroup, valueGroup)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun makeArray(vararg values: Value): ValueArray {
        return ValueArray.get(values)
    }

    @UDFunction
    @JvmStatic
    fun makeArrayN(value: Value, n: Int): ValueArray {
        val values = Array(n) { value }
        return ValueArray.get(values)
    }

    /**
     * The first column is treated as the key while the second one is treated as the value
     * */
    @UDFunction
    @JvmStatic
    fun toJson(rs: ResultSet): String {
        if (rs.metaData.columnCount < 2) {
            return "{}"
        }

        val map = mutableMapOf<String, String>()
        rs.beforeFirst()
        while (rs.next()) {
            // TODO: this is a temporary solution, find out why there is a ' surrounding
            val k = rs.getString(1).removeSurrounding("'")
            val v = rs.getString(2).removeSurrounding("'")
            map[k] = v
        }

        return Gson().toJson(map)
    }

    /**
     * For all ValueInts in the values, find out the minimal value, ignore no-integer values
     * */
    @UDFunction
    @JvmStatic
    fun intArrayMin(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueInt>().minByOrNull { it.int } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueInts in the values, find out the maximal value, ignore no-integer values
     * */
    @UDFunction
    @JvmStatic
    fun intArrayMax(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueInt>().maxByOrNull { it.int } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueFloats in the values, find out the minimal value, ignore no-float values
     * */
    @UDFunction
    @JvmStatic
    fun floatArrayMin(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueFloat>().minByOrNull { it.float } ?: ValueNull.INSTANCE
    }

    /**
     * For all ValueFloats in the values, find out the maximal value, ignore no-float values
     * */
    @UDFunction
    @JvmStatic
    fun floatArrayMax(values: ValueArray): Value {
        return values.list.filterIsInstance<ValueFloat>().maxByOrNull { it.float } ?: ValueNull.INSTANCE
    }

    @UDFunction
    @JvmStatic
    fun getString(value: Value): String {
        return value.string
    }

    @UDFunction
    @JvmStatic
    fun getSql(value: Value): String {
        return value.sql
    }

    @UDFunction
    @JvmStatic
    fun isEmpty(array: ValueArray): Boolean {
        return array.list.isEmpty()
    }

    @UDFunction
    @JvmStatic
    fun isNotEmpty(array: ValueArray): Boolean {
        return array.list.isNotEmpty()
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun formatTimestamp(timestamp: String, fmt: String = "yyyy-MM-dd HH:mm:ss"): String {
        val time = timestamp.toLongOrNull() ?: 0
        return formatTimestamp(time, fmt)
    }

    /**
     * Set volatileConfig to the given value
     * @param h2session H2 session
     * @param name The property name to set
     * @param value The property value to set
     * If value is null, do not set and return null
     * @return The old value or null on failure
     */
    private fun getAndSetConf(@H2Context conn: Connection, name: String, value: String?, ttl: Int): String? {
        Objects.requireNonNull(name)

        val session = getSession(conn)
        val old = session.sessionConfig.get(name)
        if (value != null) {
            session.sessionConfig.set(name, value, ttl)
            return old
        }

        return null
    }

    private fun getSession(conn: Connection): SQLSession {
        return H2SessionFactory.getSession(conn)
    }

    private fun getDuration(duration: String): Duration? {
        var d = duration
        // default unit is second
        if (StringUtils.isNumeric(d)) {
            d += "s"
        }

        return SParser.wrap(d).duration
    }

    private fun formatTimestamp(timestamp: Long, fmt: String): String {
        return SimpleDateFormat(fmt).format(Date(timestamp))
    }
}
