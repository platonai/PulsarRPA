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

import ai.platon.pulsar.common.DateTimeDetector;
import ai.platon.pulsar.common.ResourceLoader;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.crawl.filter.UrlFilter;

import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

import static ai.platon.pulsar.common.config.CapabilityTypes.RECENT_DAYS_WINDOWN;

/**
 * Filters URLs based on a file of regular expressions using the
 * {@link java.util.regex Java Regex implementation}.
 */
public class DateUrlFilter implements UrlFilter {

    public static final String URLFILTER_DATE_FILE = "urlfilter.date.file";
    public static final String URLFILTER_DATE_RULES = "urlfilter.date.rules";

    private ImmutableConfig conf;
    private String configFile = null;
    private Set<String> rules = new LinkedHashSet<>();
    private DateTimeDetector detector = new DateTimeDetector();
    private int oldDays = 7;

    public DateUrlFilter() {
    }

    public DateUrlFilter(ImmutableConfig conf) {
        oldDays = conf.getInt(RECENT_DAYS_WINDOWN, 7);
    }

    public DateUrlFilter(ZoneId zoneId, ImmutableConfig conf) {
        this.conf = conf;
        detector.setZoneId(zoneId);
    }

    public int getOldDays() {
        return oldDays;
    }

    public void setOldDays(int oldDays) {
        this.oldDays = oldDays;
    }

    /**
     * TODO : Not implemented yet
     */
    private void load() {
        String resourceFile = configFile != null ? configFile : conf.get(URLFILTER_DATE_FILE, "date-urlfilter.txt");
        String stringResource = conf.get(URLFILTER_DATE_RULES);
        ResourceLoader.readAllLines(stringResource, resourceFile);
    }

    @Override
    public String filter(String url) {
        // TODO : The timezone is the where the article published
        return detector.containsOldDate(url, oldDays, ZoneId.systemDefault()) ? null : url;
    }
}
