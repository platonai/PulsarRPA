package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.PulsarEnv
import ai.platon.pulsar.common.PulsarEnv.unmodifiedConfig
import ai.platon.pulsar.common.SParser
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.proxy.ProxyPool
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.ql.QuerySession
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.H2SessionFactory
import org.apache.commons.lang3.StringUtils
import org.h2.engine.Session
import org.h2.ext.pulsar.annotation.H2Context
import org.h2.tools.SimpleResultSet
import org.h2.value.Value
import org.h2.value.ValueArray
import org.h2.value.ValueString
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Long.MAX_VALUE
import java.time.Duration
import java.util.*

@UDFGroup
object CommonFunctions {

    val LOG = LoggerFactory.getLogger(CommonFunctions::class.java)

    @UDFunction
    @JvmStatic
    fun getProxyPoolStatus(): String {
        val proxyPool = ProxyPool.getInstance(PulsarEnv.unmodifiedConfig)
        return proxyPool.toString()
    }

    /**
     * Set config to the given value
     *
     * TODO: do we need FetchMode any more?
     *
     * @param h2session H2 session
     * @param mode The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setFetchMode(@H2Context h2session: Session, mode_: String, ttl: Int): String? {
        var mode = mode_
        try {
            mode = FetchMode.valueOf(mode.toUpperCase()).toString()
        } catch (e: Throwable) {
            LOG.warn("Unknown FetchMode $mode")
            return null
        }

        LOG.debug("Set fetch mode to $mode")
        return getAndSetConf(h2session, FETCH_MODE, mode, ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetFetchMode(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(FETCH_MODE)
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param browser The browser to use
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setBrowser(@H2Context h2session: Session, browser: String, ttl: Int): String? {
        val browserType = BrowserType.fromString(browser)
        if (browserType == BrowserType.NATIVE) {
            unsetFetchMode(h2session)
        } else {
            setFetchMode(h2session, FetchMode.SELENIUM.name, ttl)
        }

        LOG.debug("Set browser to $browser")
        return getAndSetConf(h2session, SELENIUM_BROWSER, browserType.name, ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetBrowser(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        unsetFetchMode(h2session)
        return session.config.getAndUnset(SELENIUM_BROWSER)
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setParallelFetch(@H2Context h2session: Session, parallel: Boolean, ttl: Int): String? {
        return getAndSetConf(h2session, FETCH_PREFER_PARALLEL, java.lang.Boolean.toString(parallel), ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetParallelFetch(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(FETCH_PREFER_PARALLEL)
    }

    /**
     * Also retrieve faded links whenever get out links of a page
     * @param ttl The property value time to live in session
     */
    @UDFunction
    @JvmStatic
    fun enableFadedLinks(@H2Context h2session: Session, ttl: Int): String? {
        return getAndSetConf(h2session, PARSE_RETRIEVE_FADED_LINKS, true.toString(), ttl)
    }

    @UDFunction
    @JvmStatic
    fun disableFadedLinks(@H2Context h2session: Session, ttl: Int): String? {
        return getAndSetConf(h2session, PARSE_RETRIEVE_FADED_LINKS, false.toString(), ttl)
    }

    @UDFunction
    @JvmStatic
    fun setEagerFetchLimit(@H2Context h2session: Session, parallel: Boolean, ttl: Int): String? {
        return getAndSetConf(h2session, FETCH_EAGER_FETCH_LIMIT, parallel.toString(), ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetEagerFetchLimit(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(FETCH_EAGER_FETCH_LIMIT)
    }

    /**
     * Set page expires, this config affects just in h2session scope
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
    @UDFunction
    @JvmStatic
    fun setPageExpires(@H2Context h2session: Session, duration: String, ttl: Int): String? {
        val d = getDuration(duration)
        val value = d?.toString()
        return getAndSetConf(h2session, STORAGE_DATUM_EXPIRES, value, ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetPageExpires(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(STORAGE_DATUM_EXPIRES)
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setPageLoadTimeout(@H2Context h2session: Session, duration: String, ttl: Int): String? {
        val d = getDuration(duration)
        val value = d?.toString()
        return getAndSetConf(h2session, FETCH_PAGE_LOAD_TIMEOUT, value, ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetPageLoadTimeout(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(FETCH_PAGE_LOAD_TIMEOUT)
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setScriptTimeout(@H2Context h2session: Session, duration: String, ttl: Int): String? {
        val d = getDuration(duration)
        val value = d?.toString()
        return getAndSetConf(h2session, FETCH_SCRIPT_TIMEOUT, value, ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetScriptTimeout(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(FETCH_SCRIPT_TIMEOUT)
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param count The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setScrollDownCount(@H2Context h2session: Session, count: Int, ttl: Int): String? {
        return getAndSetConf(h2session, FETCH_SCROLL_DOWN_COUNT, Integer.toString(count), ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetScrollDownCount(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(FETCH_SCROLL_DOWN_COUNT)
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     */
    @UDFunction
    @JvmStatic
    fun setScrollDownWait(@H2Context h2session: Session, duration: String, ttl: Int): String? {
        val d = getDuration(duration)
        val value = d?.toString()
        return getAndSetConf(h2session, FETCH_SCROLL_DOWN_WAIT, value, ttl)
    }

    @UDFunction
    @JvmStatic
    fun unsetScrollDownWait(@H2Context h2session: Session): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(FETCH_SCROLL_DOWN_WAIT)
    }

    /**
     * Set h2session scope configuration.
     *
     * SQL examples:
     * `CALL setConf('fetch.page.load.timeout', '1d')`
     * `CALL setConf('fetch.fetch.mode', 'NATIVE')`
     *
     * @param h2session The H2 session, auto injected by h2 runtime
     * @return The old value of the key
     */
    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun setConfig(@H2Context h2session: Session, name: String, value: String, ttl: Int = Integer.MAX_VALUE / 2): String? {
        val session = getSession(h2session)
        return session.config.getAndSet(name, value, ttl)
    }

    @UDFunction
    @JvmStatic
    fun setConf(@H2Context h2session: Session, name: String, value: String, ttl: Int): String? {
        return setConfig(h2session, name, value, ttl)
    }

    @UDFunction
    @JvmStatic
    fun setConf(@H2Context h2session: Session, name: String, value: String): String? {
        return setConfig(h2session, name, value, Integer.MAX_VALUE / 2)
    }

    @UDFunction
    @JvmStatic
    fun unsetConf(@H2Context h2session: Session, name: String): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(name)
    }

    @UDFunction
    @JvmStatic
    fun unsetConfig(@H2Context h2session: Session, name: String): String? {
        val session = getSession(h2session)
        return session.config.getAndUnset(name)
    }

    @UDFunction
    @JvmStatic
    fun getConf(@H2Context h2session: Session, name: String): String? {
        val session = getSession(h2session)
        return session.config.get(name)
    }

    @UDFunction
    @JvmStatic
    fun getConfig(@H2Context h2session: Session, name: String): String? {
        return getConf(h2session, name)
    }

    @UDFunction
    @JvmStatic
    fun config(): SimpleResultSet {
        val rs = SimpleResultSet()

        rs.addColumn("NAME")
        rs.addColumn("VALUE")

        for ((key, value) in PulsarEnv.unmodifiedConfig.unbox()) {
            rs.addRow(key, value)
        }

        return rs
    }

    @UDFunction
    @JvmStatic
    fun sessionConfig(@H2Context h2session: Session): SimpleResultSet {
        val rs = SimpleResultSet()
        rs.addColumn("NAME")
        rs.addColumn("VALUE")

        val session = getSession(h2session)
        for ((key, value) in session.config.unbox()) {
            rs.addRow(key, value)
        }

        return rs
    }

    @UDFunction
    @JvmStatic
    fun sysInfo(@H2Context h2session: Session): SimpleResultSet {
        val rs = SimpleResultSet()
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

    @UDFunction
    @JvmStatic
    fun addProxy(ipPort: String): Boolean {
        val proxyEntry = ai.platon.pulsar.common.proxy.ProxyEntry.parse(ipPort)
        if (proxyEntry != null && proxyEntry.testNetwork()) {
            val proxyPool = ai.platon.pulsar.common.proxy.ProxyPool.getInstance(PulsarEnv.unmodifiedConfig)
            return proxyPool.offer(proxyEntry)
        }

        return false
    }

    @UDFunction
    @JvmStatic
    fun addProxy(ip: String, port: Int): Boolean {
        if (ai.platon.pulsar.common.NetUtil.testNetwork(ip, port)) {
            val proxyPool = ai.platon.pulsar.common.proxy.ProxyPool.getInstance(PulsarEnv.unmodifiedConfig)
            return proxyPool.offer(ai.platon.pulsar.common.proxy.ProxyEntry(ip, port))
        }

        return false
    }

    @UDFunction
    @JvmStatic
    fun addProxies(ipPorts: ValueArray): Int {
        var count = 0

        for (value in ipPorts.list) {
            if (addProxy(value.string)) {
                ++count
            }
        }

        return count
    }

    @UDFunction
    @JvmStatic
    fun addProxiesUnchecked(ipPorts: ValueArray): Int {
        var count = 0

        val proxyPool = ai.platon.pulsar.common.proxy.ProxyPool.getInstance(unmodifiedConfig)
        for (value in ipPorts.list) {
            val proxyEntry = ai.platon.pulsar.common.proxy.ProxyEntry.parse(value.string)
            if (proxyEntry != null) {
                proxyPool.offer(proxyEntry)
                ++count
            }
        }

        return count
    }

    @UDFunction
    @JvmStatic
    fun recoverProxyPool(n: Int): Int {
        val proxyPool = ai.platon.pulsar.common.proxy.ProxyPool.getInstance(unmodifiedConfig)
        return proxyPool.recover(n)
    }

    @UDFunction
    @JvmStatic
    fun isNumeric(str: String): Boolean {
        return StringUtils.isNumeric(str)
    }

    @UDFunction
    @JvmStatic
    fun getDomain(url: String): String {
        return ai.platon.pulsar.common.URLUtil.getDomainName(url, "")
    }

    @UDFunction
    @JvmStatic
    fun re1(text: String, regex: String): String {
        return ai.platon.pulsar.common.RegexExtractor().re1(text, regex)
    }

    @UDFunction
    @JvmStatic
    fun re1(text: String, regex: String, group: Int): String {
        return ai.platon.pulsar.common.RegexExtractor().re1(text, regex, group)
    }

    @UDFunction
    @JvmStatic
    fun re2(text: String, regex: String): ValueArray {
        val result = ai.platon.pulsar.common.RegexExtractor().re2(text, regex)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun re2(text: String, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        val result = ai.platon.pulsar.common.RegexExtractor().re2(text, regex, keyGroup, valueGroup)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun array(vararg values: Value): ValueArray {
        return ValueArray.get(values)
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param name The property name to set
     * @param value The property value to set
     * If value is null, do not set and return null
     * @return The old value or null on failure
     */
    private fun getAndSetConf(@H2Context h2session: Session, name: String, value: String?, ttl: Int): String? {
        Objects.requireNonNull(name)

        val session = getSession(h2session)
        val old = session.config.get(name)
        if (value != null) {
            session.config.set(name, value, ttl)
            return old
        }

        return null
    }

    private fun getSession(h2session: Session): QuerySession {
        return H2SessionFactory.getSession(h2session.id)
    }

    private fun getDuration(duration: String): Duration? {
        var d = duration
        // default unit is second
        if (StringUtils.isNumeric(d)) {
            d += "s"
        }

        return SParser.wrap(d).duration
    }
}
