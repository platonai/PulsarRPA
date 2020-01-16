/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.protocol.http;

import ai.platon.pulsar.PulsarEnv;
import ai.platon.pulsar.common.*;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.common.proxy.ProxyPool;
import ai.platon.pulsar.crawl.protocol.Content;
import ai.platon.pulsar.crawl.protocol.Protocol;
import ai.platon.pulsar.crawl.protocol.ProtocolOutput;
import ai.platon.pulsar.crawl.protocol.Response;
import ai.platon.pulsar.persist.RetryScope;
import ai.platon.pulsar.persist.ProtocolStatus;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import crawlercommons.robots.BaseRobotRules;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.HttpHeaders.Q_REQUEST_TIME;
import static ai.platon.pulsar.common.HttpHeaders.Q_RESPONSE_TIME;
import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.persist.ProtocolStatus.*;
import static ai.platon.pulsar.persist.metadata.Name.RESPONSE_TIME;
import static org.apache.http.HttpStatus.*;

public abstract class AbstractHttpProtocol implements Protocol {

    public static final int BUFFER_SIZE = 8 * 1024;
    private static final int MAX_REY_GUARD = 10;

    private Logger log = LoggerFactory.getLogger(AbstractHttpProtocol.class);

    private AtomicBoolean closed = new AtomicBoolean();

    /**
     * Prevent multiple threads generate the same log unnecessary
     */
    private static AtomicBoolean parametersReported = new AtomicBoolean(false);
    /**
     * The proxy hostname.
     */
    protected String proxyHost;

    /**
     * The proxy port.
     */
    protected int proxyPort = 8080;

    /**
     * Indicates if a proxy is used
     */
    protected boolean useProxy = false;

    /**
     * Indicates if a proxy pool is used
     */
    protected boolean useProxyPool = false;

    /**
     * The proxy pool
     */
    protected ProxyPool proxyPool;

    /**
     * The network timeout in millisecond
     */
    protected Duration timeout = Duration.ofSeconds(10);

    /**
     * The max retry time if there is a network problem especially when uses a
     * proxy pool
     */
    protected int fetchMaxRetry = 3;

    /**
     * The length limit for downloaded content, in bytes.
     */
    protected int maxContent = 1024 * 1024;

    /**
     * The AppConstants 'User-Agent' request header
     */
    protected String userAgent = NetUtil.getAgentString("Pulsar", null, "AppConstants",
            "http://pulsar.platon.ai/bot.html", "agent@pulsar.platon.ai");

    /**
     * The "Accept-Language" request header value.
     */
    protected String acceptLanguage = "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2";

    /**
     * The "Accept" request header value.
     */
    protected String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    /**
     * Do we use HTTP/1.1?
     */
    protected boolean useHttp11 = false;
    /**
     * Which TLS/SSL protocols to support
     */
    protected Set<String> tlsPreferredProtocols;
    /**
     * Which TLS/SSL cipher suites to support
     */
    protected Set<String> tlsPreferredCipherSuites;
    private HttpRobotRulesParser robots;
    /**
     * The pulsar configuration
     */
    private ImmutableConfig conf;
    /**
     * The fetch mode
     */
    private FetchMode defaultFetchMode;
    private MimeUtil mimeTypes;

    /**
     * Creates a new instance of HttpBase
     */
    public AbstractHttpProtocol() {
        robots = new HttpRobotRulesParser();
    }

    public ImmutableConfig getConf() {
        return this.conf;
    }

    public void setConf(ImmutableConfig jobConf) {
        this.conf = jobConf;
        this.defaultFetchMode = jobConf.getEnum(FETCH_MODE, FetchMode.SELENIUM);

        // TODO: be consistent with WebDriverQueues
        this.proxyHost = jobConf.get("http.proxy.host");
        this.proxyPort = jobConf.getInt("http.proxy.port", 8080);
        this.useProxyPool = jobConf.getBoolean("http.proxy.pool", false);
        if (this.useProxyPool) {
            this.proxyPool = PulsarEnv.Companion.getApplicationContext().getBean(ProxyPool.class);
        }
        this.useProxy = (proxyHost != null && proxyHost.length() > 0) || this.useProxyPool;

        this.timeout = jobConf.getDuration(HTTP_TIMEOUT, Duration.ofSeconds(10));
        this.fetchMaxRetry = jobConf.getInt(HTTP_FETCH_MAX_RETRY, 3);
        this.maxContent = jobConf.getInt("http.content.limit", 1024 * 1024);
//      this.userAgent = getAgentString(conf.get("http.agent.name"),
//      conf.get("http.agent.version"), conf.get("http.agent.description"),
//      conf.get("http.agent.url"), conf.get("http.agent.email"));
        this.userAgent = NetUtil.getAgentString(jobConf.get("http.agent.name"));

        this.acceptLanguage = jobConf.get("http.accept.language", acceptLanguage);
        this.accept = jobConf.get("http.accept", accept);
        this.mimeTypes = new MimeUtil(jobConf);
        this.useHttp11 = jobConf.getBoolean("http.useHttp11", false);
        this.robots.setConf(jobConf);

        String[] protocols = jobConf.getStrings("http.tls.supported.protocols",
                "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3");
        String[] ciphers = jobConf.getStrings("http.tls.supported.cipher.suites",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                "TLS_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA", "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA", "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                "SSL_RSA_WITH_RC4_128_SHA", "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
                "TLS_ECDH_RSA_WITH_RC4_128_SHA",
                "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_RC4_128_MD5",
                "TLS_EMPTY_RENEGOTIATION_INFO_SCSV", "TLS_RSA_WITH_NULL_SHA256",
                "TLS_ECDHE_ECDSA_WITH_NULL_SHA", "TLS_ECDHE_RSA_WITH_NULL_SHA",
                "SSL_RSA_WITH_NULL_SHA", "TLS_ECDH_ECDSA_WITH_NULL_SHA",
                "TLS_ECDH_RSA_WITH_NULL_SHA", "SSL_RSA_WITH_NULL_MD5",
                "SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA", "TLS_KRB5_WITH_RC4_128_SHA",
                "TLS_KRB5_WITH_RC4_128_MD5", "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
                "TLS_KRB5_WITH_3DES_EDE_CBC_MD5", "TLS_KRB5_WITH_DES_CBC_SHA",
                "TLS_KRB5_WITH_DES_CBC_MD5");

        tlsPreferredProtocols = new HashSet<>(Arrays.asList(protocols));
        tlsPreferredCipherSuites = new HashSet<>(Arrays.asList(ciphers));

        if (parametersReported.compareAndSet(false, true)) {
            log.info(Params.formatAsLine(
                    "defaultFetchMode", defaultFetchMode,
                    "proxyHost", proxyHost,
                    "proxyPort", proxyPort,
                    "httpTimeout", timeout,
                    "maxContent", maxContent,
                    "userAgent", userAgent,
                    "httpAcceptLanguage", acceptLanguage,
                    "httpAccept", accept
            ));
        }
    }

    @Override
    public Collection<Response> getResponses(Collection<WebPage> pages, VolatileConfig volatileConfig) {
        if (closed.get()) {
            return Collections.emptyList();
        }

        return pages.stream()
                .map(page -> getResponseNoexcept(page.getUrl(), page, false))
                .collect(Collectors.toList());
    }

    public ProtocolOutput getProtocolOutput(WebPage page) {
        try {
            return getProtocolOutputWithRetry(page);
        } catch (MalformedURLException e) {
            return new ProtocolOutput(STATUS_PROTO_NOT_FOUND);
        } catch (Throwable e) {
            log.warn("Unexpected exception", e);
            return new ProtocolOutput(ProtocolStatus.failed(e));
        }
    }

    private ProtocolOutput getProtocolOutputWithRetry(WebPage page) throws MalformedURLException {
        Instant startTime = Instant.now();

        Response response;
        boolean retry = false;
        Throwable lastThrowable = null;
        int i = 0;
        int maxTry = getMaxRetry(page);
        do {
            if (i > 0) {
                log.info("Protocol retry: {}/{} | {}", i, maxTry, page.getUrl());
            }

            try {
                response = getResponse(page.getUrl(), page, false);
                retry = response == null || response.getStatus().isRetry(RetryScope.FETCH_PROTOCOL);
            } catch (Throwable e) {
                response = null;
                lastThrowable = e;
                log.warn("Unexpected protocol exception", e);
            }
        } while (retry && ++i < maxTry && !closed.get());

        if (response == null) {
            return getFailedResponse(lastThrowable, i, maxTry);
        }

        setResponseTime(startTime, page, response);

        // page.baseUrl is the last working address, and page.url is the permanent internal address
        String location = page.getLocation();
        if (location == null) {
            location = page.getUrl();
        }

        return transformProtocolOutput(page.getUrl(), location, response);
    }

    /**
     * TODO: do not translate status code, they are just OK to handle in FetchComponent
     * */
    private ProtocolOutput transformProtocolOutput(String url, String location, Response response) throws MalformedURLException {
        URL u = new URL(url);

        int httpCode = response.getCode();
        byte[] bytes = response.getContent();
        // bytes = bytes == null ? EMPTY_CONTENT : bytes;
        String contentType = response.getHeader(HttpHeaders.CONTENT_TYPE);
        Content content = new Content(url, location, bytes, contentType, response.getHeaders(), mimeTypes);
        MultiMetadata headers = response.getHeaders();
        ProtocolStatus status;

        if (httpCode == 200) { // got a good response
            return new ProtocolOutput(content, headers); // return it
        } else if (httpCode == 304) {
            return new ProtocolOutput(content, headers, ProtocolStatus.STATUS_NOTMODIFIED);
        } else if (httpCode >= 300 && httpCode < 400) { // handle redirect
            String redirect = response.getHeader("Location");
            // some broken servers, such as MS IIS, use lowercase header name...
            if (redirect == null) {
                redirect = response.getHeader("location");
            }
            if (redirect == null) {
                redirect = "";
            }

            u = new URL(u, redirect);
            int code;
            switch (httpCode) {
                case SC_MULTIPLE_CHOICES: // multiple choices, preferred value in Location
                    code = ProtocolStatus.MOVED;
                    break;
                case SC_MOVED_PERMANENTLY: // moved permanently
                case SC_USE_PROXY: // use proxy (Location is URL of proxy)
                    code = ProtocolStatus.MOVED;
                    break;
                case SC_MOVED_TEMPORARILY: // found (temporarily moved)
                case SC_SEE_OTHER: // see other (redirect after POST)
                case SC_TEMPORARY_REDIRECT: // temporary redirect
                    code = ProtocolStatus.TEMP_MOVED;
                    break;
                default:
                    code = ProtocolStatus.MOVED;
            }
            // handle redirection in the higher layer.

            // page.getMetadata().set(ARG_REDIRECT_TO_URL, url.toString());
            status = ProtocolStatus.failed(code, ARG_HTTP_CODE, httpCode, ARG_REDIRECT_TO_URL, u);
        } else if (httpCode == SC_BAD_REQUEST) {
            log.warn("400 Bad request | {}", u);
            status = ProtocolStatus.failed(GONE, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == SC_UNAUTHORIZED) {
            // requires authorization, but no valid auth provided.
            status = ProtocolStatus.failed(ACCESS_DENIED, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == SC_NOT_FOUND) {
            // GONE
            status = ProtocolStatus.failed(NOTFOUND, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == SC_REQUEST_TIMEOUT) {
            // TIMEOUT
            status = ProtocolStatus.failed(REQUEST_TIMEOUT, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == SC_GONE) {
            // permanently GONE
            status = ProtocolStatus.failed(GONE, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == THREAD_TIMEOUT) {
            status = ProtocolStatus.failed(THREAD_TIMEOUT, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == WEB_DRIVER_TIMEOUT) {
            status = ProtocolStatus.failed(WEB_DRIVER_TIMEOUT, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == DOM_TIMEOUT) {
            status = ProtocolStatus.failed(DOM_TIMEOUT, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == RETRY) {
            status = ProtocolStatus.failed(RETRY, ARG_HTTP_CODE, httpCode);
        } else if (httpCode == CANCELED) {
            status = ProtocolStatus.failed(CANCELED, ARG_HTTP_CODE, httpCode);
        } else {
            status = ProtocolStatus.failed(EXCEPTION, ARG_HTTP_CODE, httpCode);
        }

        return new ProtocolOutput(content, headers, status);
    }

    private int getMaxRetry(WebPage page) {
        // Never retry if fetch mode is croudsourcing or selenium
        int maxRry = this.fetchMaxRetry;
        FetchMode pageFetchMode = page.getFetchMode();
        if (pageFetchMode == FetchMode.CROWDSOURCING || pageFetchMode == FetchMode.SELENIUM) {
            maxRry = 1;
        }

        if (maxRry > MAX_REY_GUARD) {
            maxRry = MAX_REY_GUARD;
        }

        return maxRry;
    }

    private ProtocolOutput getFailedResponse(Throwable lastThrowable, int tryCount, int maxRry) {
        int code;
        if (lastThrowable instanceof ConnectException) {
            code = ProtocolStatus.REQUEST_TIMEOUT;
        } else if (lastThrowable instanceof SocketTimeoutException) {
            code = ProtocolStatus.REQUEST_TIMEOUT;
        } else if (lastThrowable instanceof UnknownHostException) {
            code = ProtocolStatus.UNKNOWN_HOST;
        } else {
            code = ProtocolStatus.EXCEPTION;
        }

        ProtocolStatus protocolStatus = ProtocolStatus.failed(code,
                "exception", lastThrowable,
                "retry", tryCount,
                "maxRetry", maxRry);
        return new ProtocolOutput(null, new MultiMetadata(), protocolStatus);
    }

    private void setResponseTime(Instant startTime, WebPage page, Response response) {
        Duration elapsedTime;
        FetchMode pageFetchMode = page.getFetchMode();
        if (pageFetchMode == FetchMode.CROWDSOURCING || pageFetchMode == FetchMode.SELENIUM) {
            long requestTime = NumberUtils.toLong(response.getHeader(Q_REQUEST_TIME), -1);
            long responseTime = NumberUtils.toLong(response.getHeader(Q_RESPONSE_TIME), -1);

            if (requestTime > 0 && responseTime > 0) {
                elapsedTime = Duration.ofMillis(responseTime - requestTime);
            } else {
                // -1 hour means an invalid response time
                elapsedTime = Duration.ofHours(-1);
            }
        } else {
            elapsedTime = Duration.between(startTime, Instant.now());
        }

        page.getMetadata().set(RESPONSE_TIME, elapsedTime.toString());
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public boolean useProxy() {
        return useProxy;
    }

    public boolean useProxyPool() {
        return useProxyPool;
    }

    public ProxyPool proxyPool() {
        return proxyPool;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public int getMaxContent() {
        return maxContent;
    }

    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Value of "Accept-Language" request header sent by AppConstants.
     *
     * @return The value of the header "Accept-Language" header.
     */
    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    public String getAccept() {
        return accept;
    }

    public boolean getUseHttp11() {
        return useHttp11;
    }

    public Set<String> getTlsPreferredCipherSuites() {
        return tlsPreferredCipherSuites;
    }

    public Set<String> getTlsPreferredProtocols() {
        return tlsPreferredProtocols;
    }

    public byte[] processGzipEncoded(byte[] compressed, URL url) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("uncompressing....");
        }

        byte[] content;
        if (getMaxContent() >= 0) {
            content = GZIPUtils.unzipBestEffort(compressed, getMaxContent());
        } else {
            content = GZIPUtils.unzipBestEffort(compressed);
        }

        if (content == null) {
            throw new IOException("unzipBestEffort returned null");
        }

        if (log.isTraceEnabled()) {
            log.trace("fetched " + compressed.length
                    + " bytes of compressed content (expanded to " + content.length
                    + " bytes) from " + url);
        }
        return content;
    }

    public byte[] processDeflateEncoded(byte[] compressed, URL url) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("inflating....");
        }

        byte[] content = DeflateUtils.inflateBestEffort(compressed, getMaxContent());

        if (content == null) {
            throw new IOException("inflateBestEffort returned null");
        }

        if (log.isTraceEnabled()) {
            log.trace("fetched " + compressed.length
                    + " bytes of compressed content (expanded to " + content.length
                    + " bytes) from " + url);
        }
        return content;
    }

    protected abstract Response getResponse(String url, WebPage page, boolean followRedirects) throws Exception;

    @Nullable
    private Response getResponseNoexcept(String url, WebPage page, boolean followRedirects) {
        try {
            return getResponse(url, page, followRedirects);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public BaseRobotRules getRobotRules(WebPage page) {
        return robots.getRobotRulesSet(this, page.getUrl());
    }

    @Override
    public void close() {
        closed.set(true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
