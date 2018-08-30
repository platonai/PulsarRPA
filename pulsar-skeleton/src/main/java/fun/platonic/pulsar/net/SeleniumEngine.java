package fun.platonic.pulsar.net;

import fun.platonic.pulsar.common.GlobalExecutor;
import fun.platonic.pulsar.common.ObjectCache;
import fun.platonic.pulsar.common.PulsarFileSystem;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.common.config.ReloadableParameterized;
import fun.platonic.pulsar.crawl.protocol.ForwardingResponse;
import fun.platonic.pulsar.crawl.protocol.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.BrowserType;
import fun.platonic.pulsar.persist.metadata.MultiMetadata;
import fun.platonic.pulsar.persist.metadata.ProtocolStatusCodes;

import java.nio.charset.Charset;
import java.time.*;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fun.platonic.pulsar.common.HttpHeaders.*;
import static fun.platonic.pulsar.common.config.CapabilityTypes.*;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static fun.platonic.pulsar.persist.metadata.ProtocolStatusCodes.THREAD_TIMEOUT;

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class SeleniumEngine implements ReloadableParameterized, AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(SeleniumEngine.class);

    // The javascript to execute by Web browsers
    // TODO: A better solution to initialize view port and client javascript
    public static int VIEW_PORT_WIDTH = 1920;
    public static int VIEW_PORT_HEIGHT = 1080;
    public static String CLIENT_JS = "";

    private ImmutableConfig immutableConfig;
    private MutableConfig defaultMutableConfig;

    private WebDriverQueues drivers;
    private GlobalExecutor executor;
    private PulsarFileSystem fileSystem;

    private String supportedEncodings = "UTF-8|GB2312|GB18030|GBK|Big5|ISO-8859-1"
            + "|windows-1250|windows-1251|windows-1252|windows-1253|windows-1254|windows-1257";
    private Pattern HTML_CHARSET_PATTERN;

    private Duration defaultPageLoadTimeout;
    private Duration scriptTimeout;
    private int scrollDownCount;
    private Duration scrollDownWait;
    private String clientJs;

    private AtomicInteger totalTaskCount = new AtomicInteger(0);
    private AtomicInteger totalSuccessCount = new AtomicInteger(0);

    private AtomicInteger batchTaskCount = new AtomicInteger(0);
    private AtomicInteger batchSuccessCount = new AtomicInteger(0);

    private AtomicBoolean closed = new AtomicBoolean(false);

    public static SeleniumEngine getInstance(ImmutableConfig conf) {
        SeleniumEngine engine = ObjectCache.get(conf).getBean(SeleniumEngine.class);
        if (engine == null) {
            engine = new SeleniumEngine(conf);
            ObjectCache.get(conf).put(engine);
        }
        return engine;
    }

    public SeleniumEngine(
            GlobalExecutor executor,
            WebDriverQueues drivers,
            PulsarFileSystem fileSystem,
            ImmutableConfig immutableConfig) {
        this.executor = executor;
        this.drivers = drivers;
        this.fileSystem = fileSystem;
        this.immutableConfig = immutableConfig;

        reload(immutableConfig);
    }

    public SeleniumEngine(ImmutableConfig immutableConfig) {
        executor = GlobalExecutor.getInstance(immutableConfig);
        drivers = new WebDriverQueues(immutableConfig);
        fileSystem = new PulsarFileSystem(immutableConfig);

        reload(immutableConfig);
    }

    @Override
    public ImmutableConfig getConf() {
        return immutableConfig;
    }

    @Override
    public void reload(ImmutableConfig immutableConfig) {
        this.immutableConfig = immutableConfig;
        this.defaultMutableConfig = new MutableConfig(immutableConfig.unbox());

        boolean supportAllCharacterEncodings = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, false);
        if (supportAllCharacterEncodings) {
            // All charsets are supported by the system
            // The set is big, can use a static cache to hold them if necessary
            supportedEncodings = Charset.availableCharsets().values().stream()
                    .map(Charset::name)
                    .collect(Collectors.joining("|"));
        } else {
            // A limited support charsets
            supportedEncodings = immutableConfig.get(PARSE_SUPPORTED_CHARSETS, supportedEncodings);
        }
        HTML_CHARSET_PATTERN = Pattern.compile(supportedEncodings.replace("UTF-8\\|?", ""), CASE_INSENSITIVE);

        defaultPageLoadTimeout = immutableConfig.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(30));
        scriptTimeout = immutableConfig.getDuration(FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(5));

        scrollDownCount = immutableConfig.getInt(FETCH_SCROLL_DOWN_COUNT, 0);
        scrollDownWait = immutableConfig.getDuration(FETCH_SCROLL_DOWN_COUNT, Duration.ofMillis(500));

        clientJs = immutableConfig.get(FETCH_CLIENT_JS, CLIENT_JS);

        getParams().withLogger(LOG).info();
    }

    @Override
    public Params getParams() {
        return Params.of(
                "supportedEncodings", supportedEncodings,
                "defaultPageLoadTimeout", defaultPageLoadTimeout,
                "scriptTimeout", scriptTimeout,
                "scrollDownCount", scrollDownCount,
                "scrollDownWait", scrollDownWait,
                "clientJsLength", clientJs.length()
        );
    }

    public Response fetch(String url) {
        return fetchContent(WebPage.newWebPage(url, defaultMutableConfig));
    }

    public Response fetch(String url, MutableConfig mutableConfig) {
        return fetchContent(WebPage.newWebPage(url, mutableConfig));
    }

    public Response fetchContent(WebPage page) {
        Future<Response> future = executor.getExecutor().submit(() -> fetchContentInternal(page));
        ImmutableConfig conf = page.getMutableConfigOrElse(defaultMutableConfig);
        Duration timeout = conf.getDuration(FETCH_PAGE_LOAD_TIMEOUT, defaultPageLoadTimeout);
        return getResponse(page.getUrl(), future, timeout.plusSeconds(10));
    }

    public Collection<Response> fetchAll(Iterable<String> urls) {
        batchTaskCount.set(0);
        batchSuccessCount.set(0);

        return CollectionUtils.collect(urls, this::fetch);
    }

    public Collection<Response> fetchAll(Iterable<String> urls, MutableConfig mutableConfig) {
        batchTaskCount.set(0);
        batchSuccessCount.set(0);

        return CollectionUtils.collect(urls, url -> fetch(url, mutableConfig));
    }

    public Collection<Response> parallelFetchAll(Iterable<String> urls, MutableConfig mutableConfig) {
        return parallelFetchAllPages(CollectionUtils.collect(urls, WebPage::newWebPage), mutableConfig);
    }

    public Collection<Response> parallelFetchAllPages(Iterable<WebPage> pages, MutableConfig mutableConfig) {
        batchTaskCount.set(0);
        batchSuccessCount.set(0);

        Function<WebPage, Future<Response>> submitter =
                page -> executor.getExecutor().submit(() -> fetchContentInternal(page, mutableConfig));
        Collection<Pair<String, Future<Response>>> futures = CollectionUtils.collect(pages,
                page -> Pair.of(page.getUrl(), submitter.apply(page)));

        // The function must return in a reasonable time
        Duration threadTimeout = getPageLoadTimeout(mutableConfig).plusSeconds(10);
        return CollectionUtils.collect(futures, f -> getResponse(f.getKey(), f.getValue(), threadTimeout));
    }

    private Response fetchContentInternal(WebPage page) {
        return fetchContentInternal(page, page.getMutableConfigOrElse(defaultMutableConfig));
    }

    private Response fetchContentInternal(WebPage page, MutableConfig mutableConfig) {
        String url = page.getUrl();

        totalTaskCount.getAndIncrement();
        batchTaskCount.getAndIncrement();

        int priority = mutableConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0);
        WebDriver driver = drivers.poll(priority, mutableConfig);
        if (driver == null) {
            LOG.warn("Failed to get a WebDriver, retry later. Url: " + url);
            return new ForwardingResponse(url, ProtocolStatusCodes.RETRY, new MultiMetadata());
        }

        String pageSource = "";
        int status = ProtocolStatusCodes.SUCCESS_OK;
        MultiMetadata headers = new MultiMetadata();

        beforeVisit(priority, page, driver, mutableConfig);
        try {
            visit(url, driver);
            pageSource = driver.getPageSource();
            pageSource = handleSuccess(pageSource, page, driver, headers);
        } catch (org.openqa.selenium.TimeoutException e) {
            LOG.debug(e.toString());
            handleWebDriverTimeout(url, pageSource, driver);
            // TODO: the reason may be one of page load timeout, script timeout and implicit wait timeout
            status = ProtocolStatusCodes.WEB_DRIVER_TIMEOUT;
        } catch (org.openqa.selenium.WebDriverException e) {
            status = ProtocolStatusCodes.EXCEPTION;
            LOG.warn(e.toString());
        } finally {
            drivers.put(priority, driver);
        }
        afterVisit(status, page, driver);

        // TODO: handle redirect
        // TODO: collect response header
        // TODO: fetch only the major pages, css, js, etc, ignore the rest resources, ignore external resources
        // TODO: ignore timeout and get the page source

        return new ForwardingResponse(url, pageSource, status, headers);
    }

    private void visit(String url, WebDriver driver) {
        driver.manage().window().maximize();
        driver.get(url);

        // As a JavascriptExecutor
        if (JavascriptExecutor.class.isAssignableFrom(driver.getClass())) {
            JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

            if (driver instanceof HtmlUnitDriver) {
                // Wait for page load complete, htmlunit need to wait and then extract injected javascript
                FluentWait<WebDriver> fWait = new FluentWait<>(driver)
                        .withTimeout(30, TimeUnit.SECONDS)
                        .pollingEvery(1, TimeUnit.SECONDS)
                        .ignoring(NoSuchElementException.class, TimeoutException.class).ignoring(StaleElementReferenceException.class);

//                        fWait.until(ExpectedConditions.visibilityOf(driver.findElement(By.tagName("body"))));
//                        fWait.until(ExpectedConditions.elementToBeClickable(By.tagName("body")));

                for (int i = 0; i < scrollDownCount; ++i) {
                    // fWait.until(ExpectedConditions.javaScriptThrowsNoExceptions("window.scrollTo(0, document.body.scrollHeight);"));
                }
            } else {
                // Scroll down to bottom times to ensure ajax content can be loaded
                for (int i = 0; i < scrollDownCount; ++i) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Scrolling down #" + (i + 1));
                    }

                    jsExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight);");

                    try {
                        TimeUnit.SECONDS.sleep(scrollDownWait.getSeconds());
                    } catch (InterruptedException e) {
                        LOG.warn("Sleep interruption. " + e);
                    }
                }
            }

            if (StringUtils.isNotBlank(clientJs)) {
                jsExecutor.executeScript(clientJs);
            }
        }
    }

    private String handleSuccess(String pageSource, WebPage page, WebDriver driver, MultiMetadata headers) {
        // The page content's encoding is already converted to UTF-8 by Web driver
        headers.put(CONTENT_ENCODING, "UTF-8");
        headers.put(CONTENT_LENGTH, String.valueOf(pageSource.length()));

        // Some parsers use html directive to decide the content's encoding, correct it to be UTF-8
        // TODO: Do it only for html content
        // TODO: Replace only corresponding html meta directive, not all occurrence
        pageSource = HTML_CHARSET_PATTERN.matcher(pageSource).replaceFirst("UTF-8");

        headers.put(Q_TRUSTED_CONTENT_ENCODING, "UTF-8");
        headers.put(Q_RESPONSE_TIME, OffsetDateTime.now(page.getZoneId()).toString());
        headers.put(Q_WEB_DRIVER, driver.getClass().getName());
        // headers.put(CONTENT_TYPE, "");

        if (LOG.isDebugEnabled()) {
            fileSystem.save(page.getUrl(), pageSource);
        }

        return pageSource;
    }

    private void handleWebDriverTimeout(String url, String pageSource, WebDriver driver) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Selenium timeout. Timeouts: {}/{}/{}, drivers: {}/{}, url: {}",
                    defaultPageLoadTimeout, scriptTimeout, scrollDownWait,
                    drivers.freeSize(), drivers.totalSize(),
                    url
            );
        } else {
            LOG.warn("Selenium timeout, url: " + url);
        }

        if (!pageSource.isEmpty()) {
            LOG.info("Selenium timeout but the page source is OK, length: " + pageSource.length());
        }
    }

    private Duration getPageLoadTimeout(MutableConfig mutableConfig) {
        int priority = mutableConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0);
        return getPageLoadTimeout(priority, mutableConfig);
    }

    private Duration getPageLoadTimeout(int priority, MutableConfig mutableConfig) {
        Duration pageLoadTimeout;
        if (priority > 0) {
            pageLoadTimeout = Duration.ofSeconds(priority * 30);
        } else {
            pageLoadTimeout = mutableConfig.getDuration(FETCH_PAGE_LOAD_TIMEOUT, defaultPageLoadTimeout);
        }

        return pageLoadTimeout;
    }

    private void beforeVisit(int priority, WebPage page, WebDriver driver, MutableConfig mutableConfig) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Fetching task: {}/{}, thread: #{}, drivers: {}/{}, timeouts: {}/{}/{}, {}",
                    batchTaskCount.get(), totalTaskCount.get(),
                    Thread.currentThread().getId(),
                    drivers.freeSize(), drivers.totalSize(),
                    getPageLoadTimeout(priority, mutableConfig), scriptTimeout, scrollDownWait,
                    page.getConfiguredUrl()
            );
        }

        if (mutableConfig == null) {
            return;
        }

        WebDriver.Timeouts timeouts = driver.manage().timeouts();

        // Page load timeout
        Duration pageLoadTimeout = getPageLoadTimeout(priority, mutableConfig);
        timeouts.pageLoadTimeout(pageLoadTimeout.getSeconds(), TimeUnit.SECONDS);

        // Script timeout
        scriptTimeout = mutableConfig.getDuration(FETCH_SCRIPT_TIMEOUT, scriptTimeout);
        timeouts.setScriptTimeout(scriptTimeout.getSeconds(), TimeUnit.SECONDS);

        // Scrolling
        scrollDownCount = mutableConfig.getInt(FETCH_SCROLL_DOWN_COUNT, scrollDownCount);
        if (scrollDownCount > 20) {
            scrollDownCount = 20;
        }
        scrollDownWait = mutableConfig.getDuration(FETCH_SCROLL_DOWN_WAIT, scrollDownWait);
        if (scrollDownWait.compareTo(pageLoadTimeout) > 0) {
            scrollDownWait = pageLoadTimeout;
        }

        // custom js
        clientJs = mutableConfig.get(FETCH_CLIENT_JS, CLIENT_JS);
    }

    private void afterVisit(int status, WebPage page, WebDriver driver) {
        if (status == ProtocolStatusCodes.SUCCESS_OK) {
            batchSuccessCount.incrementAndGet();
            totalSuccessCount.incrementAndGet();

            // TODO: A metrics system
            if (LOG.isDebugEnabled()) {
                LOG.debug("Selenium batch task success: {}/{}, total task success: {}/{}",
                        batchSuccessCount, batchTaskCount,
                        totalSuccessCount, totalTaskCount
                );
            }
        }

        if (driver instanceof ChromeDriver) {
            page.setLastBrowser(BrowserType.CHROME);
        } else if (driver instanceof HtmlUnitDriver) {
            page.setLastBrowser(BrowserType.HTMLUNIT);
        } else {
            LOG.warn("Actual browser is set to be NATIVE by selenium engine");
            page.setLastBrowser(BrowserType.NATIVE);
        }

        // As a RemoteWebDriver
        if (RemoteWebDriver.class.isAssignableFrom(driver.getClass())) {
            RemoteWebDriver remoteWebDriver = (RemoteWebDriver) driver;

            if (LOG.isDebugEnabled()) {
                try {
                    byte[] bytes = remoteWebDriver.getScreenshotAs(OutputType.BYTES);
                    fileSystem.save(page.getUrl(), ".png", bytes);
                } catch (Exception e) {
                    LOG.warn("Failed to take screenshot for " + page.getUrl());
                }
            }
        }
    }

    private Response getResponse(String url, Future<Response> future, Duration timeout) {
        Objects.requireNonNull(future);

        int httpCode;
        MultiMetadata headers;
        try {
            return future.get(timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            httpCode = THREAD_TIMEOUT;
            headers = new MultiMetadata();
            headers.put("EXCEPTION", e.toString());

            LOG.warn("Fetch resource timeout, " + e.toString());
        } catch (InterruptedException e) {
            httpCode = ProtocolStatusCodes.EXCEPTION;
            headers = new MultiMetadata();
            headers.put("EXCEPTION", e.toString());

            LOG.warn("Interrupted when fetch resource " + e.toString());
        } catch (ExecutionException e) {
            httpCode = ProtocolStatusCodes.EXCEPTION;
            headers = new MultiMetadata();
            headers.put("EXCEPTION", e.toString());

            LOG.warn("Unexpected exception " + StringUtil.stringifyException(e));
        } catch (Exception e) {
            httpCode = ProtocolStatusCodes.EXCEPTION;
            headers = new MultiMetadata();
            headers.put("EXCEPTION", e.toString());

            LOG.warn(e.toString());
        }

        return new ForwardingResponse(url, httpCode, headers);
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }

        executor.close();
        drivers.close();
    }
}
