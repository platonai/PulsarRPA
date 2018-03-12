package org.warps.pulsar.net;

import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.StringUtil;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.common.config.Params;
import org.warps.pulsar.common.config.ReloadableParameterized;
import org.warps.pulsar.common.proxy.ProxyEntry;
import org.warps.pulsar.common.proxy.ProxyPool;
import org.warps.pulsar.persist.metadata.BrowserType;

import javax.net.ssl.SSLContext;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static org.openqa.selenium.remote.CapabilityType.SUPPORTS_JAVASCRIPT;
import static org.openqa.selenium.remote.CapabilityType.TAKES_SCREENSHOT;
import static org.warps.pulsar.common.GlobalExecutor.NCPU;
import static org.warps.pulsar.common.config.CapabilityTypes.*;

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class WebDriverQueues implements ReloadableParameterized, AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(WebDriverQueues.class);

    public static final DesiredCapabilities DEFAULT_CAPABILITIES;
    public static final ChromeOptions DEFAULT_CHROME_CAPABILITIES;

    static {
        // HtmlUnit
        DEFAULT_CAPABILITIES = new DesiredCapabilities();
        DEFAULT_CAPABILITIES.setCapability(SUPPORTS_JAVASCRIPT, true);
        DEFAULT_CAPABILITIES.setCapability(TAKES_SCREENSHOT, false);
        DEFAULT_CAPABILITIES.setCapability("downloadImages", false);
        DEFAULT_CAPABILITIES.setCapability("browserLanguage", "zh_CN");
        DEFAULT_CAPABILITIES.setCapability("resolution", "1920x1080");

        // see https://peter.sh/experiments/chromium-command-line-switches/
        DEFAULT_CHROME_CAPABILITIES = new ChromeOptions();
        DEFAULT_CHROME_CAPABILITIES.merge(DEFAULT_CAPABILITIES);
        DEFAULT_CHROME_CAPABILITIES.setHeadless(true);
        DEFAULT_CHROME_CAPABILITIES.addArguments("--window-size=1920,1080");
    }

    private ImmutableConfig conf;
    private Class<? extends WebDriver> defaultWebDriverClass;

    private final Map<Integer, ArrayBlockingQueue<WebDriver>> freeDrivers = new HashMap<>();
    private final Set<WebDriver> allDrivers = new HashSet<>();
    private final ProxyPool proxyPool;
    private final AtomicInteger freeDriverCount = new AtomicInteger(0);

    private boolean isHeadless;
    private Duration implicitlyWait;
    private Duration pageLoadTimeout;
    private Duration scriptTimeout;
    private boolean closed = false;

    public WebDriverQueues(ImmutableConfig conf) {
        proxyPool = ProxyPool.getInstance(conf);

        this.reload(conf);

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    @Override
    public void reload(ImmutableConfig conf) {
        this.conf = conf;

        defaultWebDriverClass = conf.getClass(SELENIUM_WEB_DRIVER_CLASS, ChromeDriver.class, RemoteWebDriver.class);
        isHeadless = conf.getBoolean(SELENIUM_BROWSER_HEADLESS, true);
        implicitlyWait = conf.getDuration(FETCH_DOM_WAIT_FOR_TIMEOUT, Duration.ofSeconds(20));
        pageLoadTimeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(30));
        scriptTimeout = conf.getDuration(FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(5));
    }

    @Override
    public Params getParams() {
        return Params.of(
                "defaultWebDriverClass", defaultWebDriverClass,
                "isHeadless", isHeadless,
                "implicitlyWait", implicitlyWait,
                "pageLoadTimeout", pageLoadTimeout,
                "scriptTimeout", scriptTimeout
        );
    }

    @Override
    public ImmutableConfig getConf() {
        return conf;
    }

    public long freeSize() {
        return freeDriverCount.get();
    }

    public long totalSize() {
        return allDrivers.size();
    }

    public void put(int priority, WebDriver driver) {
        try {
            freeDrivers.get(priority).put(driver);
            freeDriverCount.getAndIncrement();
        } catch (InterruptedException e) {
            LOG.warn("Failed to put a WebDriver into pool, " + e.toString());
        }
    }

    public WebDriver poll(int priority, ImmutableConfig conf) {
        Objects.requireNonNull(conf);

        try {
            ArrayBlockingQueue<WebDriver> queue = freeDrivers.get(priority);
            if (queue == null) {
                queue = new ArrayBlockingQueue<>(NCPU);
                freeDrivers.put(priority, queue);
            }

            if (queue.isEmpty()) {
                allocateWebDriver(queue, conf);
            }

            Duration timeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, pageLoadTimeout);
            WebDriver driver = queue.poll(2 * timeout.getSeconds(), TimeUnit.SECONDS);
            freeDriverCount.decrementAndGet();

            return driver;
        } catch (InterruptedException e) {
            LOG.warn("Failed to poll a WebDriver from pool, " + e.toString());
        }

        // TODO: throw exception
        return null;
    }

    private void ssl() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy trustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                return true;
            }
        };
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
        // SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    private void getRunningBrowserCount() {

    }

    private void allocateWebDriver(ArrayBlockingQueue<WebDriver> queue, ImmutableConfig conf) {
        if (allDrivers.size() >= 1.5 * NCPU) {
            LOG.warn("Too many WebDrivers ... cpu cores: {}, free/total: {}/{}",
                    NCPU, freeSize(), totalSize());
            return;
        }

        try {
            WebDriver driver = doCreateWebDriver(conf);
            if (driver != null) {
                allDrivers.add(driver);
                queue.put(driver);
                freeDriverCount.incrementAndGet();
                LOG.info("The {}th WebDriver is online, browser: {}", allDrivers.size(), driver.getClass().getSimpleName());
            }
        } catch (Throwable e) {
            LOG.error(StringUtil.stringifyException(e));
            // throw new RuntimeException("Can not create WebDriver");
        }
    }

    /**
     * Create a RemoteWebDriver
     * Use reflection so we can make the dependency level to be "provided" rather than "source"
     */
    private WebDriver doCreateWebDriver(ImmutableConfig mutableConfig)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        BrowserType browser = getBrowser(mutableConfig);

        DesiredCapabilities capabilities = new DesiredCapabilities(DEFAULT_CAPABILITIES);
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.merge(DEFAULT_CHROME_CAPABILITIES);
        // Use headless mode by default, GUI mode can be used for debugging
        boolean headless = mutableConfig.getBoolean(SELENIUM_BROWSER_HEADLESS, this.isHeadless);
        chromeOptions.setHeadless(headless);
        if (headless) {
            // Do not downloading images in headless mode
            chromeOptions.addArguments("--blink-settings=imagesEnabled=false");
        }

        // Reset proxy
        capabilities.setCapability(CapabilityType.PROXY, (Object) null);
        chromeOptions.setCapability(CapabilityType.PROXY, (Object) null);

        // Proxy is enabled by default
        boolean disableProxy = mutableConfig.getBoolean(PROXY_DISABLED, false);
        if (!disableProxy) {
            org.openqa.selenium.Proxy proxy = getProxy(mutableConfig);
            if (proxy != null) {
                capabilities.setCapability(CapabilityType.PROXY, proxy);
                chromeOptions.setCapability(CapabilityType.PROXY, proxy);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Use proxy " + proxy);
                }
            }
        }

        // Choose the WebDriver
        WebDriver driver;
        if (browser == BrowserType.CHROME) {
            driver = new ChromeDriver(chromeOptions);
        } else if (browser == BrowserType.HTMLUNIT) {
            capabilities.setCapability("browserName", "htmlunit");
            driver = new HtmlUnitDriver(capabilities);
        } else {
            if (RemoteWebDriver.class.isAssignableFrom(defaultWebDriverClass)) {
                driver = defaultWebDriverClass.getConstructor(Capabilities.class).newInstance(capabilities);
            } else {
                driver = defaultWebDriverClass.getConstructor().newInstance();
            }
        }

        // Set timeouts
        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        timeouts.pageLoadTimeout(pageLoadTimeout.getSeconds(), TimeUnit.SECONDS);
        timeouts.setScriptTimeout(scriptTimeout.getSeconds(), TimeUnit.SECONDS);
        timeouts.implicitlyWait(implicitlyWait.getSeconds(), TimeUnit.SECONDS);

        // Set log level
        if (driver instanceof RemoteWebDriver) {
            RemoteWebDriver remoteWebDriver = (RemoteWebDriver) driver;

            final Logger webDriverLog = LoggerFactory.getLogger(WebDriver.class);
            Level level = Level.FINE;
            if (webDriverLog.isDebugEnabled()) {
                level = Level.FINER;
            } else if (webDriverLog.isTraceEnabled()) {
                level = Level.ALL;
            }

            LOG.info("WebDriver log level: " + level);
            remoteWebDriver.setLogLevel(level);
        }

        return driver;
    }

    /**
     * Get a proxy from the proxy pool
     * 1. Get a proxy from config, it is usually set in session scope
     * 2. Get a proxy from the proxy poll
     */
    @Nullable
    private org.openqa.selenium.Proxy getProxy(ImmutableConfig mutableConfig) {
        String ipPort = mutableConfig.get(PROXY_IP_PORT);
        if (ipPort == null) {
            ProxyEntry proxyEntry = proxyPool.poll();
            if (proxyEntry != null) {
                ipPort = proxyEntry.ipPort();
            }
        }

        if (ipPort == null) {
            return null;
        }

        org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
        proxy.setHttpProxy(ipPort)
                .setFtpProxy(ipPort)
                .setSslProxy(ipPort);

        return proxy;
    }

    /**
     * TODO: choose a best browser automatically: which one is faster yet still have good result
     * Speed: native > htmlunit > chrome
     * Quality: chrome > htmlunit > native
     */
    private BrowserType getBrowser(ImmutableConfig mutableConfig) {
        BrowserType browser;

        if (mutableConfig != null) {
            browser = mutableConfig.getEnum(SELENIUM_BROWSER, BrowserType.CHROME);
        } else {
            browser = conf.getEnum(SELENIUM_BROWSER, BrowserType.CHROME);
        }

        return browser;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        if (isHeadless) {
            freeDrivers.clear();
            Iterator<WebDriver> it = allDrivers.iterator();
            while (it.hasNext()) {
                WebDriver driver = it.next();
                LOG.info("Closing WebDriver " + driver);
                driver.close();
                driver.quit();

                it.remove();
            }
        }
    }
}
