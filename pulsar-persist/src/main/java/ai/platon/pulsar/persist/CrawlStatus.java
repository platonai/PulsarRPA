/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.metadata.CrawlStatusCodes;

import java.util.HashMap;
import java.util.Map;

public class CrawlStatus implements CrawlStatusCodes {

    public static final CrawlStatus STATUS_UNFETCHED = new CrawlStatus(UNFETCHED);
    public static final CrawlStatus STATUS_FETCHED = new CrawlStatus(FETCHED);
    public static final CrawlStatus STATUS_GONE = new CrawlStatus(GONE);
    public static final CrawlStatus STATUS_REDIR_TEMP = new CrawlStatus(REDIR_TEMP);
    public static final CrawlStatus STATUS_REDIR_PERM = new CrawlStatus(REDIR_PERM);
    public static final CrawlStatus STATUS_RETRY = new CrawlStatus(RETRY);
    public static final CrawlStatus STATUS_NOTMODIFIED = new CrawlStatus(NOTMODIFIED);

    private static final Map<Byte, String> NAMES = new HashMap<>();

    static {
        NAMES.put(UNFETCHED, "status_unfetched");
        NAMES.put(FETCHED, "status_fetched");
        NAMES.put(GONE, "status_gone");
        NAMES.put(REDIR_TEMP, "status_redir_temp");
        NAMES.put(REDIR_PERM, "status_redir_perm");
        NAMES.put(RETRY, "status_retry");
        NAMES.put(NOTMODIFIED, "status_notmodified");
    }

    private byte status;

    public CrawlStatus(byte status) {
        this.status = status;
    }

    public int getCode() {
        return status;
    }

    public String getName() {
        String name = NAMES.get(status);
        return name != null ? name : "status_unknown";
    }

    public boolean isFetched() {
        return status == CrawlStatus.FETCHED;
    }

    public boolean isUnFetched() {
        return status == CrawlStatus.UNFETCHED;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CrawlStatus && status == ((CrawlStatus) o).getCode();
    }

    @Override
    public String toString() {
        return getName();
    }
}
