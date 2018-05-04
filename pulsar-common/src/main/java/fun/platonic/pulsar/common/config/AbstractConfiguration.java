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

package fun.platonic.pulsar.common.config;

import com.google.common.collect.Lists;
import fun.platonic.pulsar.common.SParser;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static fun.platonic.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR;

/**
 * Created by vincent on 17-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public abstract class AbstractConfiguration {

    public static final Logger LOG = LoggerFactory.getLogger(AbstractConfiguration.class);

    public static final List<String> DEFAULT_RESOURCES =
            Lists.newArrayList("pulsar-default.xml", "pulsar-site.xml", "pulsar-task.xml");

    private Configuration conf;

    /**
     * Create a {@link AbstractConfiguration}. This will load the standard
     * resources, <code>pulsar-default.xml</code>, <code>pulsar-site.xml</code>, <code>pulsar-task.xml</code>
     * and hadoop resources.
     */
    public AbstractConfiguration() {
        this(true);
    }

    public AbstractConfiguration(boolean loadDefaults) {
        this(loadDefaults, System.getProperty(PULSAR_CONFIG_PREFERRED_DIR, "."));
    }

    public AbstractConfiguration(String preferredDir) {
        this(true, preferredDir);
    }

    public AbstractConfiguration(boolean loadDefaults, String preferredDir) {
        this(loadDefaults, preferredDir, DEFAULT_RESOURCES);
    }

    /**
     * Construct the {@link AbstractConfiguration}, load default resource if required
     *
     * @see Configuration#addDefaultResource
     */
    public AbstractConfiguration(boolean loadDefaults, String preferredDir, List<String> resources) {
        conf = new Configuration(loadDefaults);

        if (!loadDefaults) {
            return;
        }

        if (!preferredDir.isEmpty()) {
            conf.setIfUnset(PULSAR_CONFIG_PREFERRED_DIR, preferredDir);
        }

        String extraResources = System.getProperty(CapabilityTypes.PULSAR_CONFIG_RESOURCES);
        if (extraResources != null) {
            resources.addAll(Arrays.asList(extraResources.split(",")));
        }

        List<String> realResources = new ArrayList<>();
        String dir = isDistributedFs(conf) ? "cluster" : "local";
        for (String name : resources) {
            String realResource = getRealResource(preferredDir, dir, name);
            if (realResource != null) {
                realResources.add(realResource);
            } else {
                LOG.warn("Failed to find resource " + name);
            }
        }

        realResources.forEach(conf::addResource);

        LOG.info(toString());
        URL url = conf.getResource("log4j.properties");
        if (url != null) {
            LOG.info("Log4j: " + url);
        }
    }

    private String getRealResource(String prefix, String dir, String name) {
        String realResource = null;

        if (!prefix.isEmpty()) {
            realResource = getRealResource(prefix + "/" + dir, name);
        }

        if (realResource == null) {
            realResource = getRealResource(dir, name);
        }

        return realResource;
    }

    private String getRealResource(String prefix, String name) {
        String realResource = prefix + "/" + name;

        URL url = conf.getResource(realResource);
        if (url != null) {
            return realResource;
        }

        url = conf.getResource(name);
        if (url != null) {
            realResource = name;
            return realResource;
        }

        return null;
    }

    public AbstractConfiguration(Configuration conf) {
        this.conf = new Configuration(conf);
    }

    public static boolean isDistributedFs(Configuration conf) {
        String fsName = conf.get("fs.defaultFS");
        return fsName != null && fsName.startsWith("hdfs");
    }

    public Configuration unbox() {
        return conf;
    }

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
        return conf.get(name);
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
        return conf.get(name, defaultValue);
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
     * @throws NumberFormatException when the value is invalid
     */
    public int getInt(String name, int defaultValue) {
        return conf.getInt(name, defaultValue);
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
        return conf.getInts(name);
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
     * @throws NumberFormatException when the value is invalid
     */
    public long getLong(String name, long defaultValue) {
        return conf.getLong(name, defaultValue);
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
     * @throws NumberFormatException when the value is invalid
     */
    public float getFloat(String name, float defaultValue) {
        return conf.getFloat(name, defaultValue);
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
     * @throws NumberFormatException when the value is invalid
     */
    public double getDouble(String name, double defaultValue) {
        return conf.getDouble(name, defaultValue);
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
        return conf.getBoolean(name, defaultValue);
    }

    /**
     * Return value matching this enumerated type.
     *
     * @param name         Property name
     * @param defaultValue Value returned if no mapping exists
     * @throws IllegalArgumentException If mapping is illegal for the type
     *                                  provided
     */
    public <T extends Enum<T>> T getEnum(String name, T defaultValue) {
        return conf.getEnum(name, defaultValue);
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
        return conf.getStringCollection(name);
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
        return conf.getStrings(name);
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
        return conf.getStrings(name, defaultValue);
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
        return conf.getTrimmedStringCollection(name);
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
        return conf.getTrimmedStrings(name);
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
        return conf.getTrimmedStrings(name, defaultValue);
    }

    /**
     * Get a unsigned integer, if the configured value is negative or not set, return the default value
     *
     * @param name         The property name
     * @param defaultValue The default value return if the configured value is negative
     * @return a positive integer
     */
    public Integer getUint(String name, int defaultValue) {
        int value = conf.getInt(name, defaultValue);
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
        Long value = conf.getLong(name, defaultValue);
        if (value < 0) {
            value = defaultValue;
        }
        return value;
    }

    public void setDuration(String name, Duration duration) {
        conf.set(name, duration.toString());
    }

    /**
     * Support both ISO-8601 standard and hadoop time duration format
     * ISO-8601 standard : PnDTnHnMn.nS
     * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
     */
    public Duration getDuration(String name) {
        return SParser.wrap(conf.get(name)).getDuration();
    }

    public Duration getDuration(String name, Duration defaultValue) {
        return SParser.wrap(conf.get(name)).getDuration(defaultValue);
    }

    public Instant getInstant(String name, Instant defaultValue) {
        return SParser.wrap(conf.get(name)).getInstant(defaultValue);
    }

    public Path getPath(String name, Path elsePath) {
        return SParser.wrap(conf.get(name)).getPath(elsePath);
    }

    public Map<String, String> getKvs(String name) {
        return SParser.wrap(conf.get(name)).getKvs();
    }

    public InputStream getConfResourceAsInputStream(String name) {
        return conf.getConfResourceAsInputStream(name);
    }

    public Reader getConfResourceAsReader(String name) {
        return conf.getConfResourceAsReader(name);
    }

    public URL getResource(String name) {
        return conf.getResource(name);
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
        return conf.getClass(name, defaultValue);
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
     */
    public <U> Class<? extends U> getClass(String name,
                                           Class<? extends U> defaultValue,
                                           Class<U> xface) {
        return conf.getClass(name, defaultValue, xface);
    }

    public boolean isProductionEnv() {
        String env = conf.get("ENV", "production");
        return env.contains("production");
    }

    @Override
    public String toString() {
        return conf.toString();
    }
}
