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
import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

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
        this(loadDefaults, System.getProperty(CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR, "."));
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
        loadConfResources(loadDefaults, preferredDir, resources);
    }

    public AbstractConfiguration(Configuration conf) {
        this.conf = new Configuration(conf);
    }

    private void loadConfResources(boolean loadDefaults, String preferredDir, List<String> resources) {
        conf = new Configuration(loadDefaults);

        if (!loadDefaults) {
            return;
        }

        if (!preferredDir.isEmpty()) {
            conf.setIfUnset(CapabilityTypes.PULSAR_CONFIG_PREFERRED_DIR, preferredDir);
        }

        String extraResources = System.getProperty(CapabilityTypes.PULSAR_CONFIG_RESOURCES);
        if (extraResources != null) {
            resources.addAll(Arrays.asList(extraResources.split(",")));
        }

        List<String> realResources = new ArrayList<>();
        String dir = isDistributedFs() ? "cluster" : "local";
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
    }

    /**
     * Most logging systems check it's environment by itself, if not, use this one
     * */
    private void checkLogConfig() {
        if (!checkLogbackConfig() && !checkLog4jProperties()) {
            System.err.println("Failed to find log4j or logback configuration");
        }
    }

    private boolean checkLogbackConfig() {
        String logback = System.getProperty("logback.configurationFile");
        if (logback != null) {
            LOG.info("Logback(specified): " + logback);
        } else {
            URL url = getResource("logback.xml");
            if (url != null) {
                LOG.info("Logback(classpath): " + url);
                return true;
            }
        }
        return true;
    }

    private boolean checkLog4jProperties() {
        String log4j = System.getProperty("Log4j.configuration");
        if (log4j != null) {
            LOG.info("Log4j(specified): " + log4j);
        } else {
            URL url = getResource("log4j.properties");
            if (url != null) {
                LOG.info("Log4j(classpath): " + url);
            } else {
                return false;
            }
        }
        return true;
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

        URL url = getResource(realResource);
        if (url != null) {
            return realResource;
        }

        url = getResource(name);
        if (url != null) {
            realResource = name;
            return realResource;
        }

        return null;
    }

    public boolean isDryRun() {
        return getBoolean(CapabilityTypes.DRY_RUN, false);
    }

    public boolean isDistributedFs() {
        String fsName = get("fs.defaultFS");
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
     * @throws NumberFormatException when the value is invalid
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
     * @throws NumberFormatException when the value is invalid
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
     * @throws NumberFormatException when the value is invalid
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
     * @throws IllegalArgumentException If mapping is illegal for the type
     *                                  provided
     */
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
     */
    public Duration getDuration(String name) {
        return p(name).getDuration();
    }

    public Duration getDuration(String name, Duration defaultValue) {
        return p(name).getDuration(defaultValue);
    }

    public Instant getInstant(String name, Instant defaultValue) {
        return p(name).getInstant(defaultValue);
    }

    public Path getPath(String name, Path elsePath) {
        return p(name).getPath(elsePath);
    }

    public Map<String, String> getKvs(String name) {
        return p(name).getKvs();
    }

    public InputStream getConfResourceAsInputStream(String resource) {
        return SParser.wrap(resource).getResourceAsInputStream();
    }

    public Reader getConfResourceAsReader(String resource) {
        return SParser.wrap(resource).getResourceAsReader();
    }

    public URL getResource(String resource) {
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
     */
    public <U> Class<? extends U> getClass(String name,
                                           Class<? extends U> defaultValue,
                                           Class<U> xface) {
        return p(name).getClass(defaultValue, xface);
    }

    public boolean isProductionEnv() {
        String env = get("ENV", "production");
        return env.contains("production");
    }

    @Nonnull
    private SParser p(String name) {
        return new SParser(get(name));
    }

    @Override
    public String toString() {
        return conf.toString();
    }
}
