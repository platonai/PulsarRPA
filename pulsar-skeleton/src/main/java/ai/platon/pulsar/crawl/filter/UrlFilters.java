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

package ai.platon.pulsar.crawl.filter;

import ai.platon.pulsar.common.config.ImmutableConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates and caches {@link UrlFilter} implementing plugins.
 */
public class UrlFilters {

    public Logger LOG = UrlFilter.LOG;

    private ArrayList<UrlFilter> urlFilters = new ArrayList<>();

    public UrlFilters() {
    }

    public UrlFilters(ImmutableConfig conf) {
        this(Collections.emptyList(), conf);
    }

    public UrlFilters(List<UrlFilter> urlFilters, ImmutableConfig conf) {
        this.urlFilters.addAll(urlFilters);
    }

    public ArrayList<UrlFilter> getUrlFilters() {
        return urlFilters;
    }

    public void setUrlFilters(ArrayList<UrlFilter> urlFilters) {
        this.urlFilters = urlFilters;
    }

    /**
     * Run all defined urlFilters. Assume logical AND.
     */
    public String filter(String urlString) {
        if (urlString == null) {
            return null;
        }

        String initialString = urlString;
        for (UrlFilter urlFilter : urlFilters) {
            urlString = urlFilter.filter(urlString);

            if (urlString == null) {
                LOG.debug("Url {} is filtered to null by {}", initialString, urlFilter.getClass().getSimpleName());
                break;
            }
        }

        return urlString;
    }

    @Override
    public String toString() {
        return urlFilters.stream().map(f -> f.getClass().getSimpleName()).collect(Collectors.joining(", "));
    }
}
