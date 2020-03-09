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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProtocolOutput {
    private Content content;
    private MultiMetadata headers;
    private ProtocolStatus status;

    public ProtocolOutput(Content content) {
        this(content, new MultiMetadata(), ProtocolStatus.STATUS_SUCCESS);
    }

    public ProtocolOutput(Content content, MultiMetadata headers) {
        this(content, headers, ProtocolStatus.STATUS_SUCCESS);
    }

    public ProtocolOutput(ProtocolStatus status) {
        this(null, new MultiMetadata(), status);
    }

    public ProtocolOutput(@Nullable Content content, @NotNull MultiMetadata headers, @NotNull ProtocolStatus status) {
        this.content = content;
        this.headers = headers;
        this.status = status;
    }

    @Nullable
    public Content getContent() {
        return content;
    }

    public long length() {
        return content != null ? content.length() : 0;
    }

    public ProtocolStatus getStatus() {
        return status;
    }

    public MultiMetadata getHeaders() {
        return headers;
    }
}
