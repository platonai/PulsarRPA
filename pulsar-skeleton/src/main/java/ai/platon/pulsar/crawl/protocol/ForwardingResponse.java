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
import ai.platon.pulsar.persist.metadata.MultiMetadata;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Forward a response.
 */
public class ForwardingResponse implements Response {

    private String url;
    private byte[] content;
    private ProtocolStatus status;
    private MultiMetadata headers;

    public ForwardingResponse(String url, ProtocolStatus status) {
        this(url, "", status, new MultiMetadata());
    }

    public ForwardingResponse(String url, ProtocolStatus status, MultiMetadata headers) {
        this(url, "", status, headers);
    }

    public ForwardingResponse(String url, String content, ProtocolStatus status, MultiMetadata headers) {
        this(url, content.getBytes(), status, headers);
    }

    public ForwardingResponse(String url, byte[] content, ProtocolStatus status, MultiMetadata headers) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(content);
        Objects.requireNonNull(headers);

        this.url = url;
        this.content = content;
        this.status = status;
        this.headers = headers;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public int getStatus() {
        return status.getMajorCode();
    }

    @Override
    public int getCode() {
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
