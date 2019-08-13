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

// JDK imports
package ai.platon.pulsar.filter;

import ai.platon.pulsar.common.FSUtils;
import ai.platon.pulsar.common.LocalFSUtils;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.filter.UrlFilter;

import java.util.HashSet;
import java.util.Set;

import static ai.platon.pulsar.common.PulsarPaths.PATH_BANNED_URLS;
import static ai.platon.pulsar.common.PulsarPaths.PATH_UNREACHABLE_HOSTS;

/**
 * Filters URLs based on a file of regular expressions using the
 * {@link java.util.regex Java Regex implementation}.
 */
public class BannedUrlFilter implements UrlFilter {

    public static final String URLFILTER_DATE_FILE = "urlfilter.date.file";
    public static final String URLFILTER_DATE_RULES = "urlfilter.date.rules";
    private final Set<String> bannedUrls = new HashSet<>();
    private final Set<String> unreachableHosts = new HashSet<>();
    private ImmutableConfig conf;

    /**
     * TODO: conf is not initialized
     * */
    public BannedUrlFilter() {
        bannedUrls.addAll(FSUtils.readAllLinesSilent(PATH_BANNED_URLS, conf));
        unreachableHosts.addAll(LocalFSUtils.readAllLinesSilent(PATH_UNREACHABLE_HOSTS));
    }

    public BannedUrlFilter(ImmutableConfig conf) {
        this();
        this.conf = conf;
    }

    @Override
    public String filter(String url) {
        return bannedUrls.contains(url) || unreachableHosts.contains(url) ? null : url;
    }
}
