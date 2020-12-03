/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a getConf of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.common.config;

import ai.platon.pulsar.common.SParser;
import org.apache.hadoop.conf.Configuration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static ai.platon.pulsar.common.config.CapabilityTypes.LEGACY_CONFIG_PROFILE;
import static ai.platon.pulsar.common.config.CapabilityTypes.SYSTEM_PROPERTY_SPECIFIED_RESOURCES;

/**
 * Created by vincent on 17-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * @author vincent
 * @version $Id: $Id
 */
public abstract class AbstractConfiguration {

    /** Constant <code>LOG</code> */
    public static final Logger LOG = LoggerFactory.getLogger(AbstractConfiguration.class);

    // A LinkedHashSet's iterator preserves insertion order, and because it's a Set, its elements are unique.
    /** Constant <code>DEFAULT_RESOURCES</code> */
    public static final LinkedHashSet<String> DEFAULT_RESOURCES = new LinkedHashSet<>();

    /** Constant <code>APPLICATION_SPECIFIED_RESOURCES="pulsar-default.xml,pulsar-site.xml,puls"{trunked}</code> */
    public static final String APPLICATION_SPECIFIED_RESOURCES = "pulsar-default.xml,pulsar-site.xml,pulsar-task.xml";

    private final LinkedHashSet<String> resources = new LinkedHashSet<>();

    private String name = "Configuration#" + hashCode();

    private String profile = "";

    private final LinkedHashSet<URL> fullPathResources = new LinkedHashSet<>();

    /**
     * we will remove dependency on {@link Configuration} later
     */
    private Configuration conf;

    /**
     * Spring core is the first class dependency now, we will remove dependency on {@link Configuration} later
     */
    private Environment environment;

    /**
     * Create a {@link ai.platon.pulsar.common.config.AbstractConfiguration}. This will load the standard
     * resources, <code>pulsar-default.xml</code>, <code>pulsar-site.xml</code>, <code>pulsar-task.xml</code>
     * and hadoop resources.
     */
    public AbstractConfiguration() {
        this(true);
    }

    /**
     * <p>Constructor for AbstractConfiguration.</p>
     *
     * @param loadDefaults a boolean.
     */
    public AbstractConfiguration(boolean loadDefaults) {
        this(loadDefaults, System.getProperty(LEGACY_CONFIG_PROFILE, ""));
    }

    /**
     * <p>Constructor for AbstractConfiguration.</p>
     *
     * @param profile a {@link java.lang.String} object.
     */
    public AbstractConfiguration(String profile) {
        this(true, profile);
    }

    /**
     * <p>Constructor for AbstractConfiguration.</p>
     *
     * @param loadDefaults a boolean.
     * @param profile a {@link java.lang.String} object.
     */
    public AbstractConfiguration(boolean loadDefaults, String profile) {
        this(loadDefaults, profile, DEFAULT_RESOURCES);
    }

    /**
     * Construct the {@link ai.platon.pulsar.common.config.AbstractConfiguration}, load default resource if required
     *
     * @see Configuration#addDefaultResource
     * @param loadDefaults a boolean.
     * @param profile a {@link java.lang.String} object.
     * @param resources a {@link java.lang.Iterable} object.
     */
    public AbstractConfiguration(boolean loadDefaults, String profile, Iterable<String> resources) {
        loadConfResources(loadDefaults, profile, resources);
    }

    /**
     * <p>Constructor for AbstractConfiguration.</p>
     *
     * @param conf a {@link org.apache.hadoop.conf.Configuration} object.
     */
    public AbstractConfiguration(Configuration conf) {
        this.conf = new Configuration(conf);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Setter for the field <code>environment</code>.</p>
     *
     * @param environment a {@link org.springframework.core.env.Environment} object.
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private void loadConfResources(boolean loadDefaults, String profile, Iterable<String> extraResources) {
        conf = new Configuration(loadDefaults);
        extraResources.forEach(resources::add);
        this.profile = profile;

        if (!loadDefaults) {
            return;
        }

        if (!profile.isEmpty()) {
            conf.set(LEGACY_CONFIG_PROFILE, profile);
        }

        String specifiedResources = System.getProperty(SYSTEM_PROPERTY_SPECIFIED_RESOURCES, APPLICATION_SPECIFIED_RESOURCES);
        Arrays.spliterator(specifiedResources.split(",")).forEachRemaining(resources::add);

        String mode = isDistributedFs() ? "cluster" : "local";
        for (String name : resources) {
            URL realResource = getRealResource(profile, mode, name);
            if (realResource != null) {
                fullPathResources.add(realResource);
            } else {
                LOG.info("Resource not find: " + name);
            }
        }

        fullPathResources.forEach(conf::addResource);

        LOG.info(toString());
    }

    private URL getRealResource(String profile, String mode, String name) {
        String prefix = "config/legacy";
        String suffix = mode + "/" + name;
        String[] searchPaths = {
                prefix + "/" + suffix, prefix + "/" + profile + "/" + suffix,
                prefix + "/" + name, prefix + "/" + profile + "/" + name,
                name
        };

        URL resource = Stream.of(searchPaths).sorted(Comparator.comparingInt(String::length).reversed())
                .map(this::getResource).filter(Objects::nonNull).findFirst().orElse(null);
        if (resource != null) {
            LOG.info("Find legacy resource: " + resource);
        }
        return resource;
    }

    /**
     * <p>isDryRun.</p>
     *
     * @return a boolean.
     */
    public boolean isDryRun() {
        return getBoolean(CapabilityTypes.DRY_RUN, false);
    }

    /**
     * <p>isDistributedFs.</p>
     *
     * @return a boolean.
     */
    public boolean isDistributedFs() {
        String fsName = get("fs.defaultFS");
        return fsName != null && fsName.startsWith("hdfs");
    }

    /**
     * <p>unbox.</p>
     *
     * @return a {@link org.apache.hadoop.conf.Configuration} object.
     */
    public Configuration unbox() {
        return conf;
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        return conf.size();
    }

    /**
     * Get the value of the <code>name</code> property, <code>null</code> if
     * no such property exists. If the key is deprecated, it returns the value of
     * the first key which replaces the deprecated key and is not null.
     * <p>
     * Values are processed for <a href="#VariableExpansion">variable expansion</a>
     * before being returned.
     *
     * @param name the property name, will be trimmed before get value.
     * @return the value of the <code>name</code> or its replacing property,
     * or null if no such property exists.
     */
    public String get(String name) {
        String value = null;

        // spring environment has the highest priority
        if (environment != null) {
            value = environment.getProperty(name);
        }

        if (value == null) {
            value = conf.get(name);
        }

        return value;
    }

    /**
     * Get the value of the <code>name</code>. If the key is deprecated,
     * it returns the value of the first key which replaces the deprecated key
     * and is not null.
     * If no such property exists,
     * then <code>defaultValue</code> is returned.
     *
     * @param name         property name, will be trimmed before get value.
     * @param defaultValue default value.
     * @return property value, or <code>defaultValue</code> if the property
     * doesn't exist.
     */
    public String get(String name, String defaultValue) {
        String value = conf.get(name);
        if (environment != null && value == null) {
            value = environment.getProperty(name);
        }

        if (value == null) return defaultValue;
        return value;
    }

    /**
     * Get the value of the <code>name</code> property as an <code>int</code>.
     * <p>
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid <code>int</code>,
     * then an error is thrown.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as an <code>int</code>,
     * or <code>defaultValue</code>.
     * @throws java.lang.NumberFormatException when the value is invalid
     */
    public int getInt(String name, int defaultValue) {
        return p(name).getInt(defaultValue);
    }

    /**
     * Get the value of the <code>name</code> property as a set of comma-delimited
     * <code>int</code> values.
     * <p>
     * If no such property exists, an empty array is returned.
     *
     * @param name property name
     * @return property value interpreted as an array of comma-delimited
     * <code>int</code> values
     */
    public int[] getInts(String name) {
        return p(name).getInts();
    }

    /**
     * Get the value of the <code>name</code> property as a <code>long</code>.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid <code>long</code>,
     * then an error is thrown.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as a <code>long</code>,
     * or <code>defaultValue</code>.
     * @throws java.lang.NumberFormatException when the value is invalid
     */
    public long getLong(String name, long defaultValue) {
        return p(name).getLong(defaultValue);
    }

    /**
     * Get the value of the <code>name</code> property as a <code>float</code>.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid <code>float</code>,
     * then an error is thrown.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as a <code>float</code>,
     * or <code>defaultValue</code>.
     * @throws java.lang.NumberFormatException when the value is invalid
     */
    public float getFloat(String name, float defaultValue) {
        return p(name).getFloat(defaultValue);
    }

    /**
     * Get the value of the <code>name</code> property as a <code>double</code>.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid <code>double</code>,
     * then an error is thrown.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as a <code>double</code>,
     * or <code>defaultValue</code>.
     * @throws java.lang.NumberFormatException when the value is invalid
     */
    public double getDouble(String name, double defaultValue) {
        return p(name).getDouble(defaultValue);
    }

    /**
     * Get the value of the <code>name</code> property as a <code>boolean</code>.
     * If no such property is specified, or if the specified value is not a valid
     * <code>boolean</code>, then <code>defaultValue</code> is returned.
     *
     * @param name         property name.
     * @param defaultValue default value.
     * @return property value as a <code>boolean</code>,
     * or <code>defaultValue</code>.
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        return p(name).getBoolean(defaultValue);
    }

    /**
     * Return value matching this enumerated type.
     *
     * @param name         Property name
     * @param defaultValue Value returned if no mapping exists
     * @throws java.lang.IllegalArgumentException If mapping is illegal for the type
     *                                  provided
     * @param <T> a T object.
     * @return a T object.
     */
    @NotNull
    public <T extends Enum<T>> T getEnum(String name, T defaultValue) {
        return p(name).getEnum(defaultValue);
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as
     * a collection of <code>String</code>s.
     * If no such property is specified then empty collection is returned.
     * <p>
     * This is an optimized version of {@link #getStrings(String)}
     *
     * @param name property name.
     * @return property value as a collection of <code>String</code>s.
     */
    public Collection<String> getStringCollection(String name) {
        return p(name).getStringCollection();
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as
     * an array of <code>String</code>s.
     * If no such property is specified then <code>null</code> is returned.
     *
     * @param name property name.
     * @return property value as an array of <code>String</code>s,
     * or <code>null</code>.
     */
    public String[] getStrings(String name) {
        return p(name).getStrings();
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as
     * an array of <code>String</code>s.
     * If no such property is specified then default value is returned.
     *
     * @param name         property name.
     * @param defaultValue The default value
     * @return property value as an array of <code>String</code>s,
     * or default value.
     */
    public String[] getStrings(String name, String... defaultValue) {
        return p(name).getStrings(defaultValue);
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as
     * a collection of <code>String</code>s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then empty <code>Collection</code> is returned.
     *
     * @param name property name.
     * @return property value as a collection of <code>String</code>s, or empty <code>Collection</code>
     */
    public Collection<String> getTrimmedStringCollection(String name) {
        return p(name).getTrimmedStringCollection();
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as
     * an array of <code>String</code>s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then an empty array is returned.
     *
     * @param name property name.
     * @return property value as an array of trimmed <code>String</code>s,
     * or empty array.
     */
    public String[] getTrimmedStrings(String name) {
        return p(name).getTrimmedStrings();
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as
     * an array of <code>String</code>s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then default value is returned.
     *
     * @param name         property name.
     * @param defaultValue The default value
     * @return property value as an array of trimmed <code>String</code>s,
     * or default value.
     */
    public String[] getTrimmedStrings(String name, String... defaultValue) {
        return p(name).getTrimmedStrings(defaultValue);
    }

    /**
     * Get an unsigned integer, if the configured value is negative or not set, return the default value
     *
     * @param name         The property name
     * @param defaultValue The default value return if the configured value is negative
     * @return a positive integer
     */
    public Integer getUint(String name, int defaultValue) {
        int value = getInt(name, defaultValue);
        if (value < 0) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Get a unsigned long integer, if the configured value is negative, return the default value
     *
     * @param name         The property name
     * @param defaultValue The default value return if the configured value is negative
     * @return a positive long integer
     */
    public Long getUlong(String name, long defaultValue) {
        Long value = getLong(name, defaultValue);
        if (value < 0) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Support both ISO-8601 standard and hadoop time duration format
     * ISO-8601 standard : PnDTnHnMn.nS
     * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.time.Duration} object.
     */
    public Duration getDuration(String name) {
        return p(name).getDuration();
    }

    /**
     * <p>getDuration.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.time.Duration} object.
     * @return a {@link java.time.Duration} object.
     */
    public Duration getDuration(String name, Duration defaultValue) {
        return p(name).getDuration(defaultValue);
    }

    /**
     * <p>getInstant.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.time.Instant} object.
     * @return a {@link java.time.Instant} object.
     */
    public Instant getInstant(String name, Instant defaultValue) {
        return p(name).getInstant(defaultValue);
    }

    /**
     * <p>getPath.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param elsePath a {@link java.nio.file.Path} object.
     * @return a {@link java.nio.file.Path} object.
     */
    public Path getPath(String name, Path elsePath) {
        return p(name).getPath(elsePath);
    }

    /**
     * <p>getPathOrNull.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.nio.file.Path} object.
     */
    public Path getPathOrNull(String name) {
        return p(name).getPathOrNull();
    }

    /**
     * <p>getKvs.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.util.Map} object.
     */
    public Map<String, String> getKvs(String name) {
        return p(name).getKvs();
    }

    /**
     * <p>getConfResourceAsInputStream.</p>
     *
     * @param resource a {@link java.lang.String} object.
     * @return a {@link java.io.InputStream} object.
     */
    public InputStream getConfResourceAsInputStream(String resource) {
        return SParser.wrap(resource).getResourceAsInputStream();
    }

    /**
     * <p>getConfResourceAsReader.</p>
     *
     * @param resource a {@link java.lang.String} object.
     * @return a {@link java.io.Reader} object.
     */
    public Reader getConfResourceAsReader(String resource) {
        return SParser.wrap(resource).getResourceAsReader();
    }

    /**
     * <p>getResource.</p>
     *
     * @param resource a {@link java.lang.String} object.
     * @return a {@link java.net.URL} object.
     */
    public URL getResource(String resource) {
        // System.err.println("Search path: " + resource);
        return SParser.wrap(resource).getResource();
    }

    /**
     * Get the value of the <code>name</code> property as a <code>Class</code>.
     * If no such property is specified, then <code>defaultValue</code> is
     * returned.
     *
     * @param name         the class name.
     * @param defaultValue default value.
     * @return property value as a <code>Class</code>,
     * or <code>defaultValue</code>.
     */
    public Class<?> getClass(String name, Class<?> defaultValue) {
        return p(name).getClass(defaultValue);
    }

    /**
     * Get the value of the <code>name</code> property as a <code>Class</code>
     * implementing the interface specified by <code>xface</code>.
     * <p>
     * If no such property is specified, then <code>defaultValue</code> is
     * returned.
     * <p>
     * An exception is thrown if the returned class does not implement the named
     * interface.
     *
     * @param name         the class name.
     * @param defaultValue default value.
     * @param xface        the interface implemented by the named class.
     * @return property value as a <code>Class</code>,
     * or <code>defaultValue</code>.
     * @param <U> a U object.
     */
    public <U> Class<? extends U> getClass(String name,
                                           Class<? extends U> defaultValue,
                                           Class<U> xface) {
        return p(name).getClass(defaultValue, xface);
    }

    @NotNull
    private SParser p(String name) {
        return new SParser(get(name));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Expected " + conf);
        if (!profile.isEmpty()) {
            sb.append(", profile: ").append(profile);
        }
        return sb.toString();
    }
}
