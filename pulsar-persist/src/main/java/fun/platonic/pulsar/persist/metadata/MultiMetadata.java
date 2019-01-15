/*
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
 */
package fun.platonic.pulsar.persist.metadata;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import fun.platonic.pulsar.common.DateTimeUtil;
import fun.platonic.pulsar.common.DublinCore;
import fun.platonic.pulsar.common.HttpHeaders;
import fun.platonic.pulsar.common.config.PulsarConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A multi-valued data container.
 *
 * @author Chris Mattmann
 * @author J&eacute;r&ocirc;me Charron
 */
public class MultiMetadata implements DublinCore, HttpHeaders, PulsarConstants {

    public static final String META_TMP = "TMP_";

    /**
     * A map of all data attributes.
     */
    private Multimap<String, String> data = ArrayListMultimap.create();

    /**
     * Constructs a new, empty data.
     */
    public MultiMetadata() {
    }

    /**
     * Returns true if named value is multivalued.
     *
     * @param name name of data
     * @return true is named value is multivalued, false if single value or null
     */
    public boolean isMultiValued(String name) {
        return data.get(name) != null && data.get(name).size() > 1;
    }

    /**
     * Returns a set of the names contained in the data.
     *
     * @return Metadata names
     */
    public Set<String> names() {
        return data.keySet();
    }

    public Multimap<String, String> asMultimap() {
        return data;
    }

    /**
     * Get the value associated to a data name. If many values are assiociated
     * to the specified name, then the first one is returned.
     *
     * @param name of the data.
     * @return the value associated to the specified data name.
     */
    public String get(String name) {
        Collection<String> values = data.get(name);
        if (values.isEmpty()) {
            return null;
        } else {
            return values.iterator().next();
        }
    }

    public String get(Name name) {
        return get(name.text());
    }

    /**
     * Get the values associated to a data name.
     *
     * @param name of the data.
     * @return the values associated to a data name.
     */
    public Collection<String> getValues(String name) {
        return CollectionUtils.emptyIfNull(data.get(name));
    }

    public Collection<String> getNonNullValues(String name) {
        return getValues(name).stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Add a data name/value mapping. Add the specified value to the list of
     * values associated to the specified data name.
     *
     * @param name  the data name.
     * @param value the data value.
     */
    public void put(String name, String value) {
        data.put(name, value);
    }

    public void put(Name name, String value) {
        put(name.text(), value);
    }

    public void put(Name name, int value) {
        put(name, String.valueOf(value));
    }

    public void put(Name name, long value) {
        put(name, String.valueOf(value));
    }

    public void put(Name name, Instant value) {
        put(name, DateTimeUtil.isoInstantFormat(value));
    }

    public void set(String name, String value) {
        data.removeAll(name);
        data.put(name, value);
    }

    public int getInt(Name name, int defaultValue) {
        String s = get(name.text());
        return NumberUtils.toInt(s, defaultValue);
    }

    public long getLong(Name name, long defaultValue) {
        String s = get(name.text());
        return NumberUtils.toLong(s, defaultValue);
    }

    public boolean getBoolean(Name name, Boolean defaultValue) {
        String s = get(name);
        if (s == null) {
            return defaultValue;
        }
        return Boolean.valueOf(s);
    }

    public Instant getInstant(Name name, Instant defaultValue) {
        return DateTimeUtil.parseInstant(get(name), defaultValue);
    }

    /**
     * Copy All key-value pairs from properties.
     *
     * @param properties properties to copy from
     */
    public void putAll(Properties properties) {
        Enumeration<?> names = properties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            data.put(name, properties.getProperty(name));
        }
    }

    public void putAll(Map<String, String> metadata) {
        metadata.forEach((key, value) -> data.put(key, value));
    }

    /**
     * Remove a data and all its associated values.
     *
     * @param name data name to remove
     */
    public void removeAll(String name) {
        data.removeAll(name);
    }

    /**
     * Remove all mappings from data.
     */
    public void clear() {
        data.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof MultiMetadata)) {
            return false;
        }

        MultiMetadata other;
        try {
            other = (MultiMetadata) o;
        } catch (ClassCastException e) {
            return false;
        }

        return data.equals(other.data);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (String name : data.keySet()) {
            List<String> values = Lists.newArrayList(data.get(name));
            buf.append(name).append("=").append(StringUtils.join(values, ",")).append(StringUtils.SPACE);
        }
        return buf.toString();
    }
}
