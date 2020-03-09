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

import ai.platon.pulsar.common.HttpHeaders;
import ai.platon.pulsar.common.MimeUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.VolatileConfig;
import ai.platon.pulsar.crawl.protocol.Content;
import ai.platon.pulsar.crawl.protocol.Protocol;
import ai.platon.pulsar.crawl.protocol.ProtocolOutput;
import ai.platon.pulsar.crawl.protocol.Response;
import ai.platon.pulsar.persist.ProtocolStatus;
import ai.platon.pulsar.persist.RetryScope;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.FetchMode;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import crawlercommons.robots.BaseRobotRules;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static ai.platon.pulsar.common.HttpHeaders.Q_REQUEST_TIME;
import static ai.platon.pulsar.common.HttpHeaders.Q_RESPONSE_TIME;
import static ai.platon.pulsar.common.config.CapabilityTypes.HTTP_FETCH_MAX_RETRY;
import static ai.platon.pulsar.persist.ProtocolStatus.*;
import static ai.platon.pulsar.persist.metadata.Name.RESPONSE_TIME;
import static org.apache.http.HttpStatus.*;

public abstract class AbstractHttpProtocol implements Protocol {

    private static final int MAX_REY_GUARD = 10;

    private Logger log = LoggerFactory.getLogger(AbstractHttpProtocol.class);

    protected AtomicBoolean closed = new AtomicBoolean();

    /**
     * The max retry time
     */
    protected int fetchMaxRetry = 3;

    /**
     * The pulsar configuration
     */
    private ImmutableConfig conf;
    private MimeUtil mimeTypes;
    private HttpRobotRulesParser robots;

    /**
     * Creates a new instance of HttpBase
     */
    public AbstractHttpProtocol() {
    }

    public ImmutableConfig getConf() {
        return this.conf;
    }

    public void setConf(ImmutableConfig jobConf) {
        this.conf = jobConf;
        this.fetchMaxRetry = jobConf.getInt(HTTP_FETCH_MAX_RETRY, 3);
        this.mimeTypes = new MimeUtil(jobConf);
        this.robots = new HttpRobotRulesParser();
        this.robots.setConf(jobConf);
    }

    @Override
    public void reset() {
        // reset proxy, user agent, etc
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
                // TODO: FETCH_PROTOCOL does not work if the response is forwarded
                response = getResponse(page.getUrl(), page, false);
                retry = response == null || response.getStatus().isRetry(RetryScope.PROTOCOL);
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

        return getOutputWithHttpStatusTransformed(page.getUrl(), location, response);
    }

    /**
     * TODO: do not translate status code, they are just OK to handle in FetchComponent
     * */
    private ProtocolOutput getOutputWithHttpStatusTransformed(
            String url, String location, Response response) throws MalformedURLException {
        URL u = new URL(url);

        int httpCode = response.getHttpCode();
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
        } else {
            status = response.getStatus();
        }

        return new ProtocolOutput(content, headers, status);
    }

    private int getMaxRetry(WebPage page) {
        // Never retry if fetch mode is crowd sourcing
        int maxRry = this.fetchMaxRetry;
        FetchMode pageFetchMode = page.getFetchMode();
        if (pageFetchMode == FetchMode.CROWD_SOURCING) {
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
        if (pageFetchMode == FetchMode.CROWD_SOURCING || pageFetchMode == FetchMode.SELENIUM) {
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
