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

import ai.platon.pulsar.common.DeflateUtils;
import ai.platon.pulsar.common.GZIPUtils;
import ai.platon.pulsar.common.MimeUtil;
import ai.platon.pulsar.common.NetUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.crawl.protocol.Response;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static ai.platon.pulsar.common.config.CapabilityTypes.HTTP_TIMEOUT;

public abstract class AbstractNativeHttpProtocol extends AbstractHttpProtocol {

    public static final int BUFFER_SIZE = 8 * 1024;

    private Logger log = LoggerFactory.getLogger(AbstractNativeHttpProtocol.class);
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
    /**
     * The fetch mode
     */
    private FetchMode defaultFetchMode;
    private MimeUtil mimeTypes;

    /**
     * Creates a new instance of HttpBase
     */
    public AbstractNativeHttpProtocol() {
        super();
    }

    public void setConf(ImmutableConfig jobConf) {
        super.setConf(jobConf);

        // TODO: be consistent with WebDriverQueues
        this.proxyHost = jobConf.get("http.proxy.host");
        this.proxyPort = jobConf.getInt("http.proxy.port", 8080);
        this.useProxy = (proxyHost != null && proxyHost.length() > 0);

        this.timeout = jobConf.getDuration(HTTP_TIMEOUT, Duration.ofSeconds(10));
        this.maxContent = jobConf.getInt("http.content.limit", 1024 * 1024);
//      this.userAgent = getAgentString(conf.get("http.agent.name"),
//      conf.get("http.agent.version"), conf.get("http.agent.description"),
//      conf.get("http.agent.url"), conf.get("http.agent.email"));
        this.userAgent = NetUtil.getAgentString(jobConf.get("http.agent.name"));

        this.acceptLanguage = jobConf.get("http.accept.language", acceptLanguage);
        this.accept = jobConf.get("http.accept", accept);
        this.mimeTypes = new MimeUtil(jobConf);
        this.useHttp11 = jobConf.getBoolean("http.useHttp11", false);

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
    public void reset() {
        // reset proxy, user agent, etc
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
    public void close() {
        closed.set(true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
