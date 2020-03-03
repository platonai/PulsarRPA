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
package ai.platon.pulsar.crawl.protocol;

import ai.platon.pulsar.persist.ProtocolStatus;
import ai.platon.pulsar.persist.RetryScope;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.MultiMetadata;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Forward a response.
 */
public class ForwardingResponse implements Response {

    private String url;
    private ProtocolStatus status;
    private MultiMetadata headers;
    private byte[] content;
    /**
     * The unmodified page, the page must be updated using status, headers and content later
     * */
    private WebPage page;

    public static ForwardingResponse canceled(WebPage page) {
        return new ForwardingResponse(page.getUrl(), ProtocolStatus.STATUS_CANCELED, page);
    }

    public static ForwardingResponse retry(WebPage page, RetryScope retryScope) {
        return new ForwardingResponse(page.getUrl(), retryScope, page);
    }

    public static ForwardingResponse failed(WebPage page, Throwable e) {
        return new ForwardingResponse(page.getUrl(), e, page);
    }

    /**
     * The page should keep status unchanged
     * */
    public ForwardingResponse(String url, ProtocolStatus status, WebPage page) {
        this(url, "", status, new MultiMetadata(), page);
    }

    /**
     * The page should keep status unchanged
     * */
    public ForwardingResponse(String url, RetryScope retryScope, WebPage page) {
        this(url, "", ProtocolStatus.retry(retryScope), new MultiMetadata(), page);
    }

    /**
     * The page should keep status unchanged
     * */
    public ForwardingResponse(String url, Throwable e, WebPage page) {
        this(url, "", ProtocolStatus.failed(e), new MultiMetadata(), page);
    }

    /**
     * The page should keep status unchanged
     * */
    public ForwardingResponse(String url, ProtocolStatus status, MultiMetadata headers, WebPage page) {
        this(url, "", status, headers, page);
    }

    /**
     * The page should keep status unchanged
     * */
    public ForwardingResponse(String url, String content, ProtocolStatus status, MultiMetadata headers, WebPage page) {
        this(url, content.getBytes(), status, headers, page);
    }

    /**
     * The page should keep status unchanged
     * */
    public ForwardingResponse(String url, byte[] content, ProtocolStatus status, MultiMetadata headers, WebPage page) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(content);
        Objects.requireNonNull(headers);
        Objects.requireNonNull(page);

        this.url = url;
        this.content = content;
        this.status = status;
        this.headers = headers;
        this.page = page;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public WebPage getPage() {
        return page;
    }

    @Override
    public ProtocolStatus getStatus() {
        return status;
    }

    /**
     * The protocol's response code, it must be compatible with standard http response code
     * */
    @Override
    public int getHttpCode() {
        return status.getMinorCode();
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public MultiMetadata getHeaders() {
        return headers;
    }

    @Nullable
    @Override
    public byte[] getContent() {
        return content;
    }
}
