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

import ai.platon.pulsar.common.MimeUtil;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.persist.metadata.MultiMetadata;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

public final class Content {
    public static final byte[] EMPTY_CONTENT = new byte[0];

    private int version;
    private String url;
    private String baseUrl;
    private byte[] content;
    private String contentType;
    private MultiMetadata metadata;
    private MimeUtil mimeTypes;

    public Content() {
        metadata = new MultiMetadata();
    }

    /**
     * The url is the permanent internal address, it might not still available to access the target.
     *
     * BaseUrl is the last working address, it might redirect to url, or it might have additional random parameters.
     *
     * BaseUrl may be different from url, it's generally normalized.
     */
    public Content(String url, String baseUrl, @Nullable byte[] content, String contentType, MultiMetadata metadata, ImmutableConfig conf) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(baseUrl);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(conf);

        this.url = url;
        this.baseUrl = baseUrl;
        this.content = content;
        this.metadata = metadata;

        this.mimeTypes = new MimeUtil(conf);
        this.contentType = getContentType(contentType, url, content);
    }

    // The baseUrl where the HTML was retrieved from, to resolve relative links against.
    public Content(String url, String baseUrl, @Nullable byte[] content, String contentType, MultiMetadata metadata, MimeUtil mimeTypes) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(baseUrl);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(mimeTypes);

        this.url = url;
        this.baseUrl = baseUrl;
        this.content = content;
        this.metadata = metadata;

        this.mimeTypes = mimeTypes;
        this.contentType = getContentType(contentType, url, content);
    }

    /**
     * The url fetched.
     */
    public String getUrl() {
        return url;
    }

    /**
     * The base url for relative links contained in the content. Maybe be
     * different from url if the request redirected.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * The binary content retrieved.
     */
    @Nullable
    public byte[] getContent() {
        return content;
    }

    public void setContent(@Nullable byte[] content) {
        this.content = content;
    }

    /**
     * The media type of the retrieved content.
     *
     * @see <a href="http://www.iana.org/assignments/media-types/">
     * http://www.iana.org/assignments/media-types/</a>
     */
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Other protocol-specific data.
     */
    public MultiMetadata getMetadata() {
        return metadata;
    }

    /**
     * Other protocol-specific data.
     */
    public void setMetadata(MultiMetadata metadata) {
        this.metadata = metadata;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Content)) {
            return false;
        }

        Content that = (Content) o;
        return this.url.equals(that.url) && this.baseUrl.equals(that.baseUrl)
                && Arrays.equals(this.getContent(), that.getContent())
                && this.contentType.equals(that.contentType)
                && this.metadata.equals(that.metadata);
    }

    private String getContentType(String typeName, String url, byte[] data) {
        return this.mimeTypes.autoResolveContentType(typeName, url, data);
    }

    @Override
    public String toString() {
        return Params.of(
                "version", version,
                "url", url,
                "baseUrl", baseUrl,
                "metadata", metadata,
                "contentType", contentType,
                "content", new String(content)
        ).formatAsLine();
    }
}
