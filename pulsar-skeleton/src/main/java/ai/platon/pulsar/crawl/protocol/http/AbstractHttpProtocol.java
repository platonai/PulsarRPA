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
import ai.platon.pulsar.persist.ProtocolStatus;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import crawlercommons.robots.BaseRobotRules;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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

    /**
     * The process environment
     * */
    private PulsarEnv env = PulsarEnv.Companion.initialize();
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
     * The PulsarConstants 'User-Agent' request header
     */
    protected String userAgent = NetUtil.getAgentString("Pulsar", null, "PulsarConstants",
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
     * Response Time
     */
    protected boolean storeResponseTime = true;
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

    // Inherited Javadoc
    public void setConf(ImmutableConfig conf) {
        this.conf = conf;
        this.defaultFetchMode = conf.getEnum(FETCH_MODE, FetchMode.NATIVE);

        // TODO: be consistent with WebDriverQueues
        this.proxyHost = conf.get("http.proxy.host");
        this.proxyPort = conf.getInt("http.proxy.port", 8080);
        this.useProxyPool = conf.getBoolean("http.proxy.pool", false);
        if (this.useProxyPool) {
            this.proxyPool = PulsarEnv.Companion.getApplicationContext().getBean(ProxyPool.class);
        }
        this.useProxy = (proxyHost != null && proxyHost.length() > 0) || this.useProxyPool;

        this.timeout = conf.getDuration(HTTP_TIMEOUT, Duration.ofSeconds(10));
        this.fetchMaxRetry = conf.getInt(HTTP_FETCH_MAX_RETRY, 3);
        this.maxContent = conf.getInt("http.content.limit", 1024 * 1024);
//      this.userAgent = getAgentString(conf.get("http.agent.name"),
//      conf.get("http.agent.version"), conf.get("http.agent.description"),
//      conf.get("http.agent.url"), conf.get("http.agent.email"));
        this.userAgent = NetUtil.getAgentString(conf.get("http.agent.name"));

        this.acceptLanguage = conf.get("http.accept.language", acceptLanguage);
        this.accept = conf.get("http.accept", accept);
        this.mimeTypes = new MimeUtil(conf);
        this.useHttp11 = conf.getBoolean("http.useHttp11", false);
        this.storeResponseTime = conf.getBoolean("http.store.responsetime", true);
        this.robots.setConf(conf);

        String[] protocols = conf.getStrings("http.tls.supported.protocols",
                "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3");
        String[] ciphers = conf.getStrings("http.tls.supported.cipher.suites",
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

        if (!parametersReported.get()) {
            LOG.info(Params.formatAsLine(
                    "defaultFetchMode", defaultFetchMode,
                    "proxyHost", proxyHost,
                    "proxyPort", proxyPort,
                    "httpTimeout", timeout,
                    "maxContent", maxContent,
                    "userAgent", userAgent,
                    "httpAcceptLanguage", acceptLanguage,
                    "httpAccept", accept
            ));
            parametersReported.set(true);
        }
    }

    @Override
    public Collection<Response> getResponses(Collection<WebPage> pages, VolatileConfig volatileConfig) {
        return pages.stream()
                .map(page -> getResponseOrNull(page.getUrl(), page, false))
                .collect(Collectors.toList());
    }

    /**
     * TODO: no access to WebPage in this layer
     */
    public ProtocolOutput getProtocolOutput(WebPage page) {
        try {
            Instant startTime = Instant.now();

            // page.baseUrl is the last working address, and page.url is the permanent internal address
            String location = page.getLocation();
            if (location == null) {
                location = page.getUrl();
            }

            Response response = null;
            Throwable lastThrowable = null;
            int tryCount = 0;
            int maxTry = getMaxRetry(page);
            while (response == null && tryCount < maxTry) {
                try {
                    if (tryCount > 0) {
                        LOG.info("Fetching {} , retries: {}/{}", page.getUrl(), tryCount, maxTry);
                    }

                    response = getResponse(location, page, false);
                } catch (Throwable e) {
                    ++tryCount;
                    response = null;
                    lastThrowable = e;
                    LOG.warn(StringUtil.stringifyException(e));
                }
            }

            if (response == null) {
                return getFailedResponse(lastThrowable, tryCount, maxTry);
            }

            if (this.storeResponseTime) {
                storeResponseTime(startTime, page, response);
            }

            return getProtocolOutput(page.getUrl(), location, response);
        } catch (Throwable e) {
            return new ProtocolOutput(null, new MultiMetadata(), ProtocolStatus.failed(e));
        }
    }

    /**
     * TODO: do not translate status code, they are just OK to handle in FetchComponent
     * */
    private ProtocolOutput getProtocolOutput(String url, String location, Response response) throws MalformedURLException {
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
            status = ProtocolStatus.failed(code, ARG_HTTP_CODE, httpCode, ARG_URL, u, ARG_REDIRECT_TO_URL, u);
        } else if (httpCode == SC_BAD_REQUEST) {
            LOG.warn("400 Bad request: " + u);
            status = ProtocolStatus.failed(GONE, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == SC_UNAUTHORIZED) {
            // requires authorization, but no valid auth provided.
            status = ProtocolStatus.failed(ACCESS_DENIED, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == SC_NOT_FOUND) {
            // GONE
            status = ProtocolStatus.failed(NOTFOUND, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == SC_REQUEST_TIMEOUT) {
            // TIMEOUT
            status = ProtocolStatus.failed(REQUEST_TIMEOUT, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == SC_GONE) {
            // permanently GONE
            status = ProtocolStatus.failed(GONE, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == THREAD_TIMEOUT) {
            status = ProtocolStatus.failed(THREAD_TIMEOUT, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == WEB_DRIVER_TIMEOUT) {
            status = ProtocolStatus.failed(WEB_DRIVER_TIMEOUT, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == DOCUMENT_READY_TIMEOUT) {
            // TODO: the document is still analysable?
            status = ProtocolStatus.failed(DOCUMENT_READY_TIMEOUT, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == RETRY) {
            status = ProtocolStatus.failed(RETRY, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else if (httpCode == CANCELED) {
            status = ProtocolStatus.failed(CANCELED, ARG_HTTP_CODE, httpCode, ARG_URL, u);
        } else {
            status = ProtocolStatus.failed(EXCEPTION, ARG_HTTP_CODE, httpCode, ARG_URL, u);
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

    private void storeResponseTime(Instant startTime, WebPage page, Response response) {
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
     * Value of "Accept-Language" request header sent by PulsarConstants.
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
        if (LOG.isTraceEnabled()) {
            LOG.trace("uncompressing....");
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

        if (LOG.isTraceEnabled()) {
            LOG.trace("fetched " + compressed.length
                    + " bytes of compressed content (expanded to " + content.length
                    + " bytes) from " + url);
        }
        return content;
    }

    public byte[] processDeflateEncoded(byte[] compressed, URL url) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("inflating....");
        }

        byte[] content = DeflateUtils.inflateBestEffort(compressed, getMaxContent());

        if (content == null) {
            throw new IOException("inflateBestEffort returned null");
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("fetched " + compressed.length
                    + " bytes of compressed content (expanded to " + content.length
                    + " bytes) from " + url);
        }
        return content;
    }

    protected abstract Response getResponse(String url, WebPage page, boolean followRedirects) throws Exception;

    @Nullable
    private Response getResponseOrNull(String url, WebPage page, boolean followRedirects) {
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
    public String toString() {
        return getClass().getSimpleName();
    }
}
