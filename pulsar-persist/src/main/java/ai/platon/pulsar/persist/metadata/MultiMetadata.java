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
package ai.platon.pulsar.persist.metadata;

import ai.platon.pulsar.common.DateTimes;
import ai.platon.pulsar.common.DublinCore;
import ai.platon.pulsar.common.HttpHeaders;
import ai.platon.pulsar.common.config.AppConstants;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
 * @version $Id: $Id
 */
public class MultiMetadata implements DublinCore, HttpHeaders, AppConstants {

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

    public MultiMetadata(Map<String, String> kvs) {
        kvs.forEach(this::put);
    }

    /**
     * Constructs a new, empty data.
     *
     * @param kvs a {@link java.lang.String} object.
     */
    public MultiMetadata(String... kvs) {
        int length = kvs.length;
        if (length % 2 == 0) {
            for (int i = 0; i < length; i += 2) {
                put(kvs[i], kvs[1 + i]);
            }
        } else {
            throw new IllegalArgumentException("Length of the variable argument 'kvs' must be an even number");
        }
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

    /**
     * <p>asMultimap.</p>
     *
     * @return a {@link com.google.common.collect.Multimap} object.
     */
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

    /**
     * <p>get.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @return a {@link java.lang.String} object.
     */
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

    /**
     * <p>getNonNullValues.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.util.Collection} object.
     */
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

    /**
     * <p>put.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a {@link java.lang.String} object.
     */
    public void put(Name name, String value) {
        put(name.text(), value);
    }

    /**
     * <p>put.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a int.
     */
    public void put(Name name, int value) {
        put(name, String.valueOf(value));
    }

    /**
     * <p>put.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a long.
     */
    public void put(Name name, long value) {
        put(name, String.valueOf(value));
    }

    /**
     * <p>put.</p>
     *
     * @param name a {@link ai.platon.pulsar.persist.metadata.Name} object.
     * @param value a {@link java.time.Instant} object.
     */
    public void put(Name name, Instant value) {
        put(name, DateTimes.isoInstantFormat(value));
    }

    /**
     * <p>set.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public void set(String name, String value) {
        data.removeAll(name);
        data.put(name, value);
    }

    /**
     * <p>getInt.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a int.
     * @return a int.
     */
    public int getInt(String name, int defaultValue) {
        String s = get(name);
        return NumberUtils.toInt(s, defaultValue);
    }

    /**
     * <p>getLong.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a long.
     * @return a long.
     */
    public long getLong(String name, long defaultValue) {
        String s = get(name);
        return NumberUtils.toLong(s, defaultValue);
    }

    /**
     * <p>getBoolean.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.Boolean} object.
     * @return a boolean.
     */
    public boolean getBoolean(String name, Boolean defaultValue) {
        String s = get(name);
        if (s == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(s);
    }

    /**
     * <p>getInstant.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.time.Instant} object.
     * @return a {@link java.time.Instant} object.
     */
    public Instant getInstant(String name, Instant defaultValue) {
        return DateTimes.parseInstant(get(name), defaultValue);
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

    /**
     * <p>putAll.</p>
     *
     * @param metadata a {@link java.util.Map} object.
     */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
