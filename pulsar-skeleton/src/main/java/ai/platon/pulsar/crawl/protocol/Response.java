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

import ai.platon.pulsar.persist.metadata.MultiMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

/**
 * A response interface. Makes all protocols model HTTP.
 */
public interface Response {
    Logger LOG = LoggerFactory.getLogger(Response.class);

    String getUrl();

    /** The protocol's response code, without transform. */
    int getStatus();

    /** The protocol's response code, without transform. */
    int getCode();

    /** The value of a named header. */
    String getHeader(String name);

    /** All the headers. */
    MultiMetadata getHeaders();

    /** Returns the full content of the response. */
    @Nullable
    byte[] getContent();

    default int size() {
        byte[] c = getContent();
        return c == null ? 0 : c.length;
    }
}
