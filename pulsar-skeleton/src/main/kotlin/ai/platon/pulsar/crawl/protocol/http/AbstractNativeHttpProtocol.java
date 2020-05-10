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
import ai.platon.pulsar.crawl.protocol.Response;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
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
    private int maxContent = 1024 * 1024;
    /**
     * The AppConstants 'User-Agent' request header
     */
    protected String userAgent = NetUtil.getAgentString("Pulsar", null, "AppConstants",
            "http://pulsar.platon.ai/bot.html", "agent@pulsar.platon.ai");
    /**
     * The "Accept-Language" request header value.
     */
    private String acceptLanguage = "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2";
    /**
     * The "Accept" request header value.
     */
    protected String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    /**
     * Do we use HTTP/1.1?
     */
    private boolean useHttp11 = false;
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

    }

    public byte[] processGzipEncoded(byte[] compressed, URL url) throws IOException {
        byte[] content;
        if (maxContent >= 0) {
            content = GZIPUtils.unzipBestEffort(compressed, maxContent);
        } else {
            content = GZIPUtils.unzipBestEffort(compressed);
        }

        if (content == null) {
            throw new IOException("unzipBestEffort returned null");
        }

        return content;
    }

    public byte[] processDeflateEncoded(byte[] compressed, URL url) throws IOException {
        byte[] content = DeflateUtils.inflateBestEffort(compressed, maxContent);

        if (content == null) {
            throw new IOException("inflateBestEffort returned null");
        }

        return content;
    }

    public abstract Response getResponse(@NotNull String url, @NotNull WebPage page, boolean followRedirects) throws Exception;
}
