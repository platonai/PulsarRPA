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

package org.warps.pulsar.crawl.parse;

import org.slf4j.Logger;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.crawl.parse.html.ParseContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Creates and caches {@link ParseFilter} implementing plugins. */
public class ParseFilters {

    public static final Logger LOG = ParseFilter.LOG;

    private ArrayList<ParseFilter> parseFilters = new ArrayList<>();

    public ParseFilters() {
    }

    public ParseFilters(ImmutableConfig conf) {
    }

    public ParseFilters(List<ParseFilter> parseFilters, ImmutableConfig conf) {
        this.parseFilters.addAll(parseFilters);
    }

    public ArrayList<ParseFilter> getParseFilters() {
        return parseFilters;
    }

    public void setParseFilters(ArrayList<ParseFilter> parseFilters) {
        this.parseFilters = parseFilters;
    }

    /** Run all defined filters. */
    public void filter(ParseContext parseContext) {
        // loop on each filter
        for (ParseFilter parseFilter : parseFilters) {
            try {
                parseFilter.filter(parseContext);
            } catch (Throwable e) {
                LOG.warn(e.toString());
            }
        }
    }

    @Override
    public String toString() {
        return parseFilters.stream().map(n -> n.getClass().getSimpleName()).collect(Collectors.joining(", "));
    }
}
