package fun.platonic.pulsar.ql.h2.udfs;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.QuerySession;
import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.common.NetUtil;
import fun.platonic.pulsar.common.RegexExtractor;
import fun.platonic.pulsar.common.SParser;
import fun.platonic.pulsar.common.URLUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.proxy.ProxyEntry;
import fun.platonic.pulsar.common.proxy.ProxyPool;
import fun.platonic.pulsar.persist.metadata.BrowserType;
import fun.platonic.pulsar.persist.metadata.FetchMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.engine.Session;
import org.h2.ext.pulsar.annotation.H2Context;
import org.h2.tools.SimpleResultSet;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static fun.platonic.pulsar.common.config.CapabilityTypes.*;
import static java.lang.Long.MAX_VALUE;

@SuppressWarnings("unused")
@UDFGroup
public class CommonFunctions {

    public static final Logger LOG = LoggerFactory.getLogger(CommonFunctions.class);

    private static QueryEngine engine;
    private static ImmutableConfig immutableConfig;
    static {
        engine = QueryEngine.getInstance();
        immutableConfig = engine.getConf();
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param mode The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     * */
    @UDFunction
    public static String setFetchMode(@H2Context Session h2session, String mode, int ttl) {
        try {
            mode = FetchMode.valueOf(mode.toUpperCase()).toString();
        } catch (Throwable e) {
            LOG.warn("Unknown FetchMode " + mode);
            mode = null;
        }

        LOG.debug("Set fetch mode to " + mode);
        return getAndSetConf(h2session, FETCH_MODE, mode, ttl);
    }

    @UDFunction
    public static String unsetFetchMode(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(FETCH_MODE);
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param browser The browser to use
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     * */
    @UDFunction
    public static String setBrowser(@H2Context Session h2session, String browser, int ttl) {
        BrowserType browserType = BrowserType.fromString(browser);
        if (browserType == BrowserType.NATIVE) {
            unsetFetchMode(h2session);
        } else {
            setFetchMode(h2session, FetchMode.SELENIUM.name(), ttl);
        }

        LOG.debug("Set browser to " + browser);
        return getAndSetConf(h2session, SELENIUM_BROWSER, browserType.name(), ttl);
    }

    @UDFunction
    public static String unsetBrowser(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        unsetFetchMode(h2session);
        return session.getConfig().getAndUnset(SELENIUM_BROWSER);
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     * */
    @UDFunction
    public static String setParallelFetch(@H2Context Session h2session, boolean parallel, int ttl) {
        return getAndSetConf(h2session, FETCH_PREFER_PARALLEL, Boolean.toString(parallel), ttl);
    }

    @UDFunction
    public static String unsetParallelFetch(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(FETCH_PREFER_PARALLEL);
    }

    /**
     * Also retrieve faded links whenever get out links of a page
     * @param ttl The property value time to live in session
     * */
    @UDFunction
    public static String enableFadedLinks(@H2Context Session h2session, int ttl) {
        return getAndSetConf(h2session, PARSE_RETRIEVE_FADED_LINKS, Boolean.toString(true), ttl);
    }

    @UDFunction
    public static String disableFadedLinks(@H2Context Session h2session, int ttl) {
        return getAndSetConf(h2session, PARSE_RETRIEVE_FADED_LINKS, Boolean.toString(false), ttl);
    }

    @UDFunction
    public static String setEagerFetchLimit(@H2Context Session h2session, boolean parallel, int ttl) {
        return getAndSetConf(h2session, FETCH_EAGER_FETCH_LIMIT, Boolean.toString(parallel), ttl);
    }

    @UDFunction
    public static String unsetEagerFetchLimit(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(FETCH_EAGER_FETCH_LIMIT);
    }

    /**
     * Set page expires, this config affects just in h2session scope
     * SQL examples:
     * <code>CALL setPageExpires('1d')</code>
     * <code>CALL setPageExpires('PT24H')</code>
     * <code>CALL setPageExpires('1s')</code>
     * <code>CALL setPageExpires('1')</code>
     *
     * @param h2session  The H2 session, auto injected by h2 runtime
     * @param duration The duration string.
     *                 Supported formats: 1. number in seconds, 2. ISO-8601 standard and 3. hadoop time duration format
     *                 ISO-8601 standard : PnDTnHnMn.nS
     *                 Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
     * @param ttl The property value time to live in session
     * @return The old page expires in seconds, or null if failed
     */
    @UDFunction
    public static String setPageExpires(@H2Context Session h2session, String duration, int ttl) {
        Duration d = getDuration(duration);
        String value = d != null ? d.toString() : null;
        return getAndSetConf(h2session, STORAGE_DATUM_EXPIRES, value, ttl);
    }

    @UDFunction
    public static String unsetPageExpires(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(STORAGE_DATUM_EXPIRES);
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     * */
    @UDFunction
    public static String setPageLoadTimeout(@H2Context Session h2session, String duration, int ttl) {
        Duration d = getDuration(duration);
        String value = d != null ? d.toString() : null;
        return getAndSetConf(h2session, FETCH_PAGE_LOAD_TIMEOUT, value, ttl);
    }

    @UDFunction
    public static String unsetPageLoadTimeout(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(FETCH_PAGE_LOAD_TIMEOUT);
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     * */
    @UDFunction
    public static String setScriptTimeout(@H2Context Session h2session, String duration, int ttl) {
        Duration d = getDuration(duration);
        String value = d != null ? d.toString() : null;
        return getAndSetConf(h2session, FETCH_SCRIPT_TIMEOUT, value, ttl);
    }

    @UDFunction
    public static String unsetScriptTimeout(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(FETCH_SCRIPT_TIMEOUT);
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param count The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     * */
    @UDFunction
    public static String setScrollDownCount(@H2Context Session h2session, int count, int ttl) {
        return getAndSetConf(h2session, FETCH_SCROLL_DOWN_COUNT, Integer.toString(count), ttl);
    }

    @UDFunction
    public static String unsetScrollDownCount(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(FETCH_SCROLL_DOWN_COUNT);
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param duration The property value to set
     * @param ttl The property value time to live in session
     * @return The old value or null on failure
     * */
    @UDFunction
    public static String setScrollDownWait(@H2Context Session h2session, String duration, int ttl) {
        Duration d = getDuration(duration);
        String value = d != null ? d.toString() : null;
        return getAndSetConf(h2session, FETCH_SCROLL_DOWN_WAIT, value, ttl);
    }

    @UDFunction
    public static String unsetScrollDownWait(@H2Context Session h2session) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(FETCH_SCROLL_DOWN_WAIT);
    }

    /**
     * Set h2session scope configuration.
     * <p>
     * SQL examples:
     * <code>CALL setConf('fetch.page.load.timeout', '1d')</code>
     * <code>CALL setConf('fetch.fetch.mode', 'NATIVE')</code>
     * <p>
     *
     * @param h2session The H2 session, auto injected by h2 runtime
     * @return The old value of the key
     */
    @UDFunction
    public static String setConfig(@H2Context Session h2session, String name, String value, int ttl) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndSet(name, value, ttl);
    }

    @UDFunction
    public static String setConfig(@H2Context Session h2session, String name, String value) {
        return setConfig(h2session, name, value, Integer.MAX_VALUE / 2);
    }

    @UDFunction
    public static String setConf(@H2Context Session h2session, String name, String value, int ttl) {
        return setConfig(h2session, name, value, ttl);
    }

    @UDFunction
    public static String setConf(@H2Context Session h2session, String name, String value) {
        return setConfig(h2session, name, value, Integer.MAX_VALUE / 2);
    }

    @UDFunction
    public static String unsetConf(@H2Context Session h2session, String name) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(name);
    }

    @UDFunction
    public static String unsetConfig(@H2Context Session h2session, String name) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().getAndUnset(name);
    }

    @UDFunction
    public static String getConf(@H2Context Session h2session, String name) {
        QuerySession session = engine.getSession(new DbSession(h2session));
        return session.getConfig().get(name);
    }

    @UDFunction
    public static String getConfig(@H2Context Session h2session, String name) {
        return getConf(h2session, name);
    }

    @UDFunction
    public static SimpleResultSet config() {
        SimpleResultSet rs = new SimpleResultSet();

        rs.addColumn("NAME");
        rs.addColumn("VALUE");

        for (Map.Entry<String, String> entry : immutableConfig.unbox()) {
            rs.addRow(entry.getKey(), entry.getValue());
        }

        return rs;
    }

    @UDFunction
    public static SimpleResultSet sessionConfig(@H2Context Session h2session) {
        SimpleResultSet rs = new SimpleResultSet();
        rs.addColumn("NAME");
        rs.addColumn("VALUE");

        ImmutableConfig conf = engine.getSession(new DbSession(h2session)).getConfig();
        for (Map.Entry<String, String> entry : conf.unbox()) {
            rs.addRow(entry.getKey(), entry.getValue());
        }

        return rs;
    }

    @UDFunction
    public static SimpleResultSet sysInfo(@H2Context Session h2session) {
        SimpleResultSet rs = new SimpleResultSet();
        rs.addColumn("NAME");
        rs.addColumn("VALUE");

        /* Total number of processors or cores available to the JVM */
        rs.addRow("Available processors (cores)", Runtime.getRuntime().availableProcessors());

        /* Total amount of free memory available to the JVM */
        rs.addRow("Free memory (Mbytes)", Runtime.getRuntime().freeMemory() / 1024 / 1024);

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        rs.addRow("Maximum memory (Mbytes)", (maxMemory == MAX_VALUE) ? "no limit" : Long.toString(maxMemory / 1024 / 1024));

        /* Total memory currently available to the JVM */
        rs.addRow("Total memory available to JVM (Mbytes)", Runtime.getRuntime().totalMemory() / 1024 / 1024);

        /* Get a list of all filesystem roots on this system */
        File[] roots = File.listRoots();
        for (File root : roots) {
            /* For each filesystem root, print some info */
            rs.addRow("File system root", root.getAbsolutePath());
            rs.addRow("Total space (Mbytes)", root.getTotalSpace() / 1024 / 1024);
            rs.addRow("Free space (Mbytes)", root.getFreeSpace() / 1024 / 1024);
            rs.addRow("Usable space (Mbytes)", root.getUsableSpace() / 1024 / 1024);
        }

        return rs;
    }

    @UDFunction
    public static boolean addProxy(String ipPort) {
        ProxyEntry proxyEntry = ProxyEntry.parse(ipPort);
        if (proxyEntry != null && proxyEntry.testNetwork()) {
            ProxyPool proxyPool = ProxyPool.getInstance(immutableConfig);
            return proxyPool.offer(proxyEntry);
        }

        return false;
    }

    @UDFunction
    public static boolean addProxy(String ip, int port) {
        if (NetUtil.testNetwork(ip, port)) {
            ProxyPool proxyPool = ProxyPool.getInstance(immutableConfig);
            return proxyPool.offer(new ProxyEntry(ip, port));
        }

        return false;
    }

    @UDFunction
    public static int addProxies(ValueArray ipPorts) {
        int count = 0;

        for (Value value : ipPorts.getList()) {
            if (addProxy(value.getString())) {
                ++count;
            }
        }

        return count;
    }

    @UDFunction
    public static int addProxiesUnchecked(ValueArray ipPorts) {
        int count = 0;

        ProxyPool proxyPool = ProxyPool.getInstance(immutableConfig);
        for (Value value : ipPorts.getList()) {
            ProxyEntry proxyEntry = ProxyEntry.parse(value.getString());
            if (proxyEntry != null) {
                proxyPool.offer(proxyEntry);
                ++count;
            }
        }

        return count;
    }

    @UDFunction
    public static int recoverProxyPool(int n) {
        ProxyPool proxyPool = ProxyPool.getInstance(immutableConfig);
        return proxyPool.recover(n);
    }

    @UDFunction
    public static String getProxyPoolStatus() {
        ProxyPool proxyPool = ProxyPool.getInstance(immutableConfig);
        return proxyPool.toString();
    }

    @UDFunction
    public static boolean isNumeric(String str) {
        return StringUtils.isNumeric(str);
    }

    @UDFunction
    public static String getDomain(String url) {
        return URLUtil.getDomainName(url, "");
    }

    @UDFunction
    public static String re1(String text, String regex) {
        return new RegexExtractor().re1(text, regex);
    }

    @UDFunction
    public static String re1(String text, String regex, int group) {
        return new RegexExtractor().re1(text, regex, group);
    }

    @UDFunction
    public static ValueArray re2(String text, String regex) {
        Pair<String, String> result = new RegexExtractor().re2(text, regex);
        Value[] array = {ValueString.get(result.getKey()), ValueString.get(result.getValue())};
        return ValueArray.get(array);
    }

    @UDFunction
    public static ValueArray re2(String text, String regex, int keyGroup, int valueGroup) {
        Pair<String, String> result = new RegexExtractor().re2(text, regex, keyGroup, valueGroup);
        Value[] array = {ValueString.get(result.getKey()), ValueString.get(result.getValue())};
        return ValueArray.get(array);
    }

    @UDFunction
    public static ValueArray array(Value... values) {
        return ValueArray.get(values);
    }

    /**
     * Set config to the given value
     * @param h2session H2 session
     * @param name The property name to set
     * @param value The property value to set
     *              If value is null, do not set and return null
     * @return The old value or null on failure
     * */
    private static String getAndSetConf(@H2Context Session h2session, String name, @Nullable String value, int ttl) {
        Objects.requireNonNull(name);

        QuerySession session = engine.getSession(new DbSession(h2session));
        String old = session.getConfig().get(name);
        if (value != null) {
            session.getConfig().set(name, value, ttl);
            return old;
        }

        return null;
    }

    private static Duration getDuration(String duration) {
        // default unit is second
        if (StringUtils.isNumeric(duration)) {
            duration += "s";
        }

        return SParser.wrap(duration).getDuration();
    }
}
