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

package ai.platon.pulsar.common;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.util.StringUtils;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A common string parser
 * */
public class SParser {
    public static final Log LOG = LogFactory.getLog(SParser.class);

    public static final Duration INVALID_DURATION = Duration.ofSeconds(Integer.MIN_VALUE);

    private static final Map<ClassLoader, Map<String, WeakReference<Class<?>>>> CACHE_CLASSES = new WeakHashMap<>();

    /**
     * Sentinel value to store negative cache results in {@link #CACHE_CLASSES}.
     */
    private static final Class<?> NEGATIVE_CACHE_SENTINEL = NegativeCacheSentinel.class;
    /**
     * Stores the mapping of key to the resource which modifies or loads
     * the key most recently
     */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");
    private static final int MAX_SUBST = 20;
    private ClassLoader classLoader;
    {
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Configuration.class.getClassLoader();
        }
    }
    private String value;

    public SParser() {
    }

    public SParser(String value) {
        this.value = value;
    }

    public static SParser wrap(String value) {
        return new SParser(value);
    }

    public void set(String value) {
        this.value = value;
    }

    public synchronized void setIfUnset(String value) {
        if (this.value == null) {
            set(value);
        }
    }

    /**
     * Get the value of property, <code>null</code> if
     * no such property exists. If the key is deprecated, it returns the value of
     * the first key which replaces the deprecated key and is not null.
     *
     * Values are processed for <a href="#VariableExpansion">variable expansion</a>
     * before being returned.
     *
     * @return the value of or its replacing property,
     *         or null if no such property exists.
     */
    public String get() {
        return substituteVars(value);
    }

    /**
     * Get the value of property as a trimmed <code>String</code>,
     * <code>null</code> if no such property exists.
     * If the key is deprecated, it returns the value of
     * the first key which replaces the deprecated key and is not null
     *
     * Values are processed for <a href="#VariableExpansion">variable expansion</a>
     * before being returned.
     *
     * @return the value of or its replacing property,
     *         or null if no such property exists.
     */
    public String getTrimmed() {
        if (null == value) {
            return null;
        } else {
            return value.trim();
        }
    }

    /**
     * Get the value of property as a trimmed <code>String</code>,
     * <code>defaultValue</code> if no such property exists.
     * See @{Configuration#getTrimmed} for more details.
     *
     * @param defaultValue  the property default value.
     * @return the value of or defaultValue
     *                      if it is not set.
     */
    public String getTrimmed(String defaultValue) {
        String ret = getTrimmed();
        return ret == null ? defaultValue : ret;
    }

    public String getRaw() {
        return value;
    }

    public String get(String defaultValue) {
        return value == null ? defaultValue : value;
    }

    public int getInt(int defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        String hexString = getHexDigits(valueString);
        if (hexString != null) {
            return Integer.parseInt(hexString, 16);
        }
        return Integer.parseInt(valueString);
    }

    public int[] getInts() {
        String[] strings = getTrimmedStrings();
        int[] ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    public void setInt(int value) {
        set(Integer.toString(value));
    }

    /**
     * Get the value of property as a <code>long</code>.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid <code>long</code>,
     * then an error is thrown.
     *
     * @param defaultValue default value.
     * @throws NumberFormatException when the value is invalid
     * @return property value as a <code>long</code>,
     *         or <code>defaultValue</code>.
     */
    public long getLong(long defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        String hexString = getHexDigits(valueString);
        if (hexString != null) {
            return Long.parseLong(hexString, 16);
        }
        return Long.parseLong(valueString);
    }

    /**
     * Get the value of property as a <code>long</code> or
     * human readable format. If no such property exists, the provided default
     * value is returned, or if the specified value is not a valid
     * <code>long</code> or human readable format, then an error is thrown. You
     * can use the following suffix (case insensitive): k(kilo), m(mega), g(giga),
     * t(tera), p(peta), e(exa)
     *
     * @param defaultValue default value.
     * @throws NumberFormatException when the value is invalid
     * @return property value as a <code>long</code>,
     *         or <code>defaultValue</code>.
     */
    public long getLongBytes(long defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        return StringUtils.TraditionalBinaryPrefix.string2long(valueString);
    }

    private String getHexDigits(String value) {
        boolean negative = false;
        String str = value;
        String hexString = null;
        if (value.startsWith("-")) {
            negative = true;
            str = value.substring(1);
        }
        if (str.startsWith("0x") || str.startsWith("0X")) {
            hexString = str.substring(2);
            if (negative) {
                hexString = "-" + hexString;
            }
            return hexString;
        }
        return null;
    }

    /**
     * Set the value of property to a <code>long</code>.
     *
     * @param value <code>long</code> value of the property.
     */
    public void setLong(long value) {
        set(Long.toString(value));
    }

    /**
     * Get the value of property as a <code>float</code>.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid <code>float</code>,
     * then an error is thrown.
     *
     * @param defaultValue default value.
     * @throws NumberFormatException when the value is invalid
     * @return property value as a <code>float</code>,
     *         or <code>defaultValue</code>.
     */
    public float getFloat(float defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        return Float.parseFloat(valueString);
    }

    /**
     * Set the value of property to a <code>float</code>.
     *
     * @param value property value.
     */
    public void setFloat(float value) {
        set(Float.toString(value));
    }

    /**
     * Get the value of property as a <code>double</code>.
     * If no such property exists, the provided default value is returned,
     * or if the specified value is not a valid <code>double</code>,
     * then an error is thrown.
     *
     * @param defaultValue default value.
     * @throws NumberFormatException when the value is invalid
     * @return property value as a <code>double</code>,
     *         or <code>defaultValue</code>.
     */
    public double getDouble(double defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        return Double.parseDouble(valueString);
    }

    /**
     * Set the value of property to a <code>double</code>.
     *
     * @param value property value.
     */
    public void setDouble(double value) {
        set(Double.toString(value));
    }

    /**
     * Get the value of property as a <code>boolean</code>.
     * If no such property is specified, or if the specified value is not a valid
     * <code>boolean</code>, then <code>defaultValue</code> is returned.
     *
     * @param defaultValue default value.
     * @return property value as a <code>boolean</code>,
     *         or <code>defaultValue</code>.
     */
    public boolean getBoolean(boolean defaultValue) {
        String valueString = getTrimmed();
        if (null == valueString || valueString.isEmpty()) {
            return defaultValue;
        }

        valueString = valueString.toLowerCase();

        if ("true".equals(valueString))
            return true;
        else if ("false".equals(valueString))
            return false;
        else return defaultValue;
    }

    /**
     * Set the value of property to a <code>boolean</code>.
     *
     * @param value <code>boolean</code> value of the property.
     */
    public void setBoolean(boolean value) {
        set(Boolean.toString(value));
    }

    /**
     * Set the given property, if it is currently unset.
     * @param value new value
     */
    public void setBooleanIfUnset(boolean value) {
        setIfUnset(Boolean.toString(value));
    }

    /**
     * Set the value of property to the given type. This
     * is equivalent to <code>set(&lt;name&gt;, value.toString())</code>.
     * @param value new value
     */
    public <T extends Enum<T>> void setEnum(T value) {
        set(value.toString());
    }

    /**
     * Return value matching this enumerated type.
     * @param defaultValue Value returned if no mapping exists
     * @throws IllegalArgumentException If mapping is illegal for the type
     * provided
     */
    public <T extends Enum<T>> T getEnum(T defaultValue) {
        return null == value
                ? defaultValue
                : Enum.valueOf(defaultValue.getDeclaringClass(), value);
    }

    /**
     * Set the value to the given time duration
     * @param value Time duration
     * @param unit Unit of time
     */
    public void setTimeDuration(long value, TimeUnit unit) {
        set(value + ParsedTimeDuration.unitFor(unit).suffix());
    }

    /**
     * Return time duration in the given time unit. Valid units are encoded in
     * properties as suffixes: nanoseconds (ns), microseconds (us), milliseconds
     * (ms), seconds (s), minutes (m), hours (h), and days (d).
     * @param defaultValue Value returned if no mapping exists.
     * @param unit Unit to convert the stored property, if it exists.
     * @throws NumberFormatException If the property stripped of its unit is not
     *         a number
     */
    public long getTimeDuration(long defaultValue, TimeUnit unit) {
        if (null == value) {
            return defaultValue;
        }
        value = value.trim();
        ParsedTimeDuration vUnit = ParsedTimeDuration.unitFor(value);
        if (null == vUnit) {
            LOG.warn("No unit for " + "(" + value + ") assuming " + unit);
            vUnit = ParsedTimeDuration.unitFor(unit);
        } else {
            value = value.substring(0, value.lastIndexOf(vUnit.suffix()));
        }
        return unit.convert(Long.parseLong(value), vUnit.unit());
    }

    /**
     * Get the value of property as a <code>Pattern</code>.
     * If no such property is specified, or if the specified value is not a valid
     * <code>Pattern</code>, then <code>DefaultValue</code> is returned.
     *
     * @param defaultValue default value
     * @return property value as a compiled Pattern, or defaultValue
     */
    public Pattern getPattern(Pattern defaultValue) {
        if (null == value || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Pattern.compile(value);
        } catch (PatternSyntaxException pse) {
            LOG.warn("Regular expression '" + value + "' for property '" + "' not valid. Using default", pse);
            return defaultValue;
        }
    }

    /**
     * Set the given property to <code>Pattern</code>.
     * If the pattern is passed as null, sets the empty pattern which results in
     * further calls to getPattern(...) returning the default value.
     *
     * @param pattern new value
     */
    public void setPattern(Pattern pattern) {
        if (null == pattern) {
            set(null);
        } else {
            set(pattern.pattern());
        }
    }

    /**
     * Parse the given attribute as a set of integer ranges
     * @param defaultValue the default value if it is not set
     * @return a new set of ranges from the configured value
     */
    public IntegerRanges getRange(String defaultValue) {
        return new IntegerRanges(get(defaultValue));
    }

    /**
     * Get the comma delimited values of property as
     * a collection of <code>String</code>s.
     * If no such property is specified then empty collection is returned.
     * <p>
     *
     * @return property value as a collection of <code>String</code>s.
     */
    public Collection<String> getStringCollection() {
        return StringUtils.getStringCollection(value);
    }

    public Pair<String, String> getPair(Pair<String, String> defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        int pos = value.indexOf(":");

        if (pos > 0 && pos < value.length() - 1) {
            return Pair.of(value.substring(0, pos), value.substring(pos));
        }
        return defaultValue;
    }

    public Map<String, String> getKvs() {
        return getKvs("[\\s+|,]", ":");
    }

    public Map<String, String> getKvs(String kvDelimeter) {
        return getKvs("[\\s+|,]", kvDelimeter);
    }

    public Map<String, String> getKvs(String pairDelimeterPattern, String kvDelimeter) {
        Map<String, String> kvs = new HashMap<>();
        if (value == null) {
            return kvs;
        }

        for (String s : value.split(pairDelimeterPattern)) {
            int pos = s.indexOf(kvDelimeter);
            if (pos > 0 && pos < s.length() - 1) {
                kvs.put(s.substring(0, pos), s.substring(pos + 1));
            }
        }
        return kvs;
    }

    /**
     * Get the comma delimited values of property as
     * an array of <code>String</code>s.
     * If no such property is specified then <code>null</code> is returned.
     *
     * @return property value as an array of <code>String</code>s,
     *         or <code>null</code>.
     */
    public String[] getStrings() {
        return StringUtils.getStrings(value);
    }

    /**
     * Set the array of string values for property as
     * as comma delimited values.
     *
     * @param values The values
     */
    public void setStrings(String... values) {
        set(StringUtils.arrayToString(values));
    }

    /**
     * Get the comma delimited values of property as
     * an array of <code>String</code>s.
     * If no such property is specified then default value is returned.
     *
     * @param defaultValue The default value
     * @return property value as an array of <code>String</code>s,
     *         or default value.
     */
    public String[] getStrings(String... defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            return StringUtils.getStrings(value);
        }
    }

    /**
     * Get the comma delimited values of property as
     * a collection of <code>String</code>s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then empty <code>Collection</code> is returned.
     *
     * @return property value as a collection of <code>String</code>s, or empty <code>Collection</code>
     */
    public Collection<String> getTrimmedStringCollection() {
        if (null == value) {
            return Collections.emptyList();
        }
        return StringUtils.getTrimmedStringCollection(value);
    }

    /**
     * Get the comma delimited values of property as
     * an array of <code>String</code>s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then an empty array is returned.
     *
     * @return property value as an array of trimmed <code>String</code>s,
     *         or empty array.
     */
    public String[] getTrimmedStrings() {
        return StringUtils.getTrimmedStrings(value);
    }

    /**
     * Get the comma delimited values of property as
     * an array of <code>String</code>s, trimmed of the leading and trailing whitespace.
     * If no such property is specified then default value is returned.
     *
     * @param defaultValue The default value
     * @return property value as an array of trimmed <code>String</code>s,
     *         or default value.
     */
    public String[] getTrimmedStrings(String... defaultValue) {
        if (null == value) {
            return defaultValue;
        } else {
            return StringUtils.getTrimmedStrings(value);
        }
    }

    /**
     * Get a unsigned integer, if the configured value is negative or not set, return the default value
     *
     * @param defaultValue The default value return if the configured value is negative
     * @return a positive integer
     */
    public Integer getUint(int defaultValue) {
        int value = getInt(defaultValue);
        if (value < 0) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Get a unsigned long integer, if the configured value is negative, return the default value
     *
     * @param defaultValue The default value return if the configured value is negative
     * @return a positive long integer
     */
    public Long getUlong(long defaultValue) {
        Long value = getLong(defaultValue);
        if (value < 0) {
            value = defaultValue;
        }
        return value;
    }

    public void setIfNotNull(String value) {
        if (value != null) {
            set(value);
        }
    }

    public void setIfNotEmpty(String value) {
        if (value != null && !value.isEmpty()) {
            set(value);
        }
    }

    /**
     * Support both ISO-8601 standard and hadoop time duration format
     * ISO-8601 standard : PnDTnHnMn.nS
     * Hadoop time duration format : Valid units are : ns, us, ms, s, m, h, d.
     */
    public Duration getDuration(Duration defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value.startsWith("P") || value.startsWith("-P")) {
                try {
                    return Duration.parse(value);
                } catch (Throwable ignored) {
                }
            }

            // Can not use TimeUnit.SECONDS because of unacceptable precision
            long value = getTimeDuration(Integer.MIN_VALUE, TimeUnit.MILLISECONDS);
            if (value == Integer.MIN_VALUE) {
                return defaultValue;
            }
            return Duration.ofMillis(value);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public Duration getDuration() {
        return getDuration(INVALID_DURATION);
    }

    public Instant getInstant(Instant defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (NumberUtils.isDigits(value)) {
            return Instant.ofEpochMilli(getLong(defaultValue.toEpochMilli()));
        }

        return DateTimeUtil.parseInstant(value, defaultValue);
    }

    public Path getPath(Path defaultValue, boolean createDirectories) throws IOException {
        Path path = (value != null) ? Paths.get(value) : defaultValue;
        if (createDirectories) {
            Files.createDirectories(path.getParent());
        }
        return path;
    }

    public Path getPath(Path defaultValue) {
        try {
            Path path = (value != null) ? Paths.get(value) : defaultValue;
            Files.createDirectories(path.getParent());
            return path;
        } catch (Throwable ignored) {
        }
        return defaultValue;
    }

    /**
     * Get the socket address for <code>name</code> property as a
     * <code>InetSocketAddress</code>.
     * @param name property name.
     * @param defaultAddress the default value
     * @param defaultPort the default port
     * @return InetSocketAddress
     */
    public InetSocketAddress getSocketAddr(
            String name, String defaultAddress, int defaultPort) {
        final String address = get(defaultAddress);
        return NetUtils.createSocketAddr(address, defaultPort, name);
    }

    /**
     * Set the socket address for property as
     * a <code>host:port</code>.
     */
    public void setSocketAddr(InetSocketAddress addr) {
        set(NetUtils.getHostPortString(addr));
    }

    /**
     * Set the socket address a client can use to connect for the
     * <code>name</code> property as a <code>host:port</code>.  The wildcard
     * address is replaced with the local host's address.
     * @param name property name.
     * @param addr InetSocketAddress of a listener to store in the given property
     * @return InetSocketAddress for clients to connect
     */
    public InetSocketAddress updateConnectAddr(String name,
                                               InetSocketAddress addr) {
        final InetSocketAddress connectAddr = NetUtils.getConnectAddress(addr);
        setSocketAddr(connectAddr);
        return connectAddr;
    }

    /**
     * Load a class by name.
     *
     * @return the class object.
     * @throws ClassNotFoundException if the class is not found.
     */
    public Class<?> getClassByName(String name) throws ClassNotFoundException {
        Class<?> ret = getClassByNameOrNull(name);
        if (ret == null) {
            throw new ClassNotFoundException("Class " + name + " not found");
        }
        return ret;
    }

    /**
     * Load a class by name, returning null rather than throwing an exception
     * if it couldn't be loaded. This is to avoid the overhead of creating
     * an exception.
     *
     * @param name the class name
     * @return the class object, or null if it could not be found.
     */
    public Class<?> getClassByNameOrNull(String name) {
        Map<String, WeakReference<Class<?>>> map;

        synchronized (CACHE_CLASSES) {
            map = CACHE_CLASSES.get(classLoader);
            if (map == null) {
                map = Collections.synchronizedMap(new WeakHashMap<>());
                CACHE_CLASSES.put(classLoader, map);
            }
        }

        Class<?> clazz = null;
        WeakReference<Class<?>> ref = map.get(name);
        if (ref != null) {
            clazz = ref.get();
        }

        if (clazz == null) {
            try {
                clazz = Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException e) {
                // Leave a marker that the class isn't found
                map.put(name, new WeakReference<>(NEGATIVE_CACHE_SENTINEL));
                return null;
            }
            // two putters can race here, but they'll put the same class
            map.put(name, new WeakReference<>(clazz));
            return clazz;
        } else if (clazz == NEGATIVE_CACHE_SENTINEL) {
            return null; // not found
        } else {
            // cache hit
            return clazz;
        }
    }

    /**
     * Get the value of property
     * as an array of <code>Class</code>.
     * The value of the property specifies a list of comma separated class names.
     * If no such property is specified, then <code>defaultValue</code> is
     * returned.
     *
     * @param defaultValue default value.
     * @return property value as a <code>Class[]</code>,
     *         or <code>defaultValue</code>.
     */
    public Class<?>[] getClasses(Class<?>... defaultValue) {
        String[] classnames = getTrimmedStrings();
        if (classnames == null)
            return defaultValue;
        try {
            Class<?>[] classes = new Class<?>[classnames.length];
            for (int i = 0; i < classnames.length; i++) {
                classes[i] = getClassByName(classnames[i]);
            }
            return classes;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the value as a <code>Class</code>.
     * If no such property is specified, then <code>defaultValue</code> is
     * returned.
     *
     * @param defaultValue default value.
     * @return property value as a <code>Class</code>,
     *         or <code>defaultValue</code>.
     */
    public Class<?> getClass(Class<?> defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        try {
            return getClassByName(valueString);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the value to <code>theClass</code> implementing the given interface <code>xface</code>.
     *
     * If no such property is specified, then <code>defaultValue</code> is
     * returned.
     *
     * An exception is thrown if the returned class does not implement the named
     * interface.
     *
     * @param defaultValue default value.
     * @param xface the interface implemented by the named class.
     * @return property value as a <code>Class</code>,
     *         or <code>defaultValue</code>.
     */
    public <U> Class<? extends U> getClass(Class<? extends U> defaultValue, Class<U> xface) {
        try {
            Class<?> theClass = getClass(defaultValue);
            if (theClass != null && !xface.isAssignableFrom(theClass))
                throw new RuntimeException(theClass + " not " + xface.getName());
            else if (theClass != null)
                return theClass.asSubclass(xface);
            else
                return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the value to <code>theClass</code> implementing the given interface <code>xface</code>.
     *
     * An exception is thrown if <code>theClass</code> does not implement the
     * interface <code>xface</code>.
     *
     * @param theClass property value.
     * @param xface the interface implemented by the named class.
     */
    public void setClass(Class<?> theClass, Class<?> xface) {
        if (!xface.isAssignableFrom(theClass))
            throw new RuntimeException(theClass + " not " + xface.getName());
        set(theClass.getName());
    }

    /**
     * Get a local file name under a directory named in <i>dirsProp</i> with
     * the given <i>path</i>.  If <i>dirsProp</i> contains multiple directories,
     * then one is chosen based on <i>path</i>'s hash code.  If the selected
     * directory does not exist, an attempt is made to create it.
     *
     * @param dirsProp directory in which to locate the file.
     * @param path file-path.
     * @return local file under the directory with the given path.
     */
    public File getFile(String dirsProp, String path)
            throws IOException {
        String[] dirs = getTrimmedStrings(dirsProp);
        int hashCode = path.hashCode();
        for (int i = 0; i < dirs.length; i++) {  // try each local dir
            int index = (hashCode + i & Integer.MAX_VALUE) % dirs.length;
            File file = new File(dirs[index], path);
            File dir = file.getParentFile();
            if (dir.exists() || dir.mkdirs()) {
                return file;
            }
        }
        throw new IOException("No valid local directories in property: " + dirsProp);
    }

    /**
     * Get the {@link URL} for the named resource.
     *
     * @return the url for the named resource.
     */
    public URL getResource() {
        return classLoader.getResource(value);
    }

    /**
     * Get an input stream
     *
     * @return an input stream attached to the resource.
     */
    public InputStream getResourceAsInputStream() {
        try {
            URL url = getResource();

            if (url == null) {
                LOG.info(value + " not found");
                return null;
            } else {
                LOG.info("found resource " + value + " at " + url);
            }

            return url.openStream();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a {@link Reader}
     *
     * @return a reader attached to the resource.
     */
    public Reader getResourceAsReader() {
        try {
            URL url = getResource();

            if (url == null) {
                LOG.info(value + " not found");
                return null;
            } else {
                LOG.info("found resource " + value + " at " + url);
            }

            return new InputStreamReader(url.openStream());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the {@link ClassLoader} for this job.
     *
     * @return the correct class loader.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Set the class loader that will be used to load the various objects.
     *
     * @param classLoader the new class loader.
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private String substituteVars(String expr) {
        if (expr == null) {
            return null;
        }
        Matcher match = VAR_PATTERN.matcher("");
        String eval = expr;
        for (int s = 0; s < MAX_SUBST; s++) {
            match.reset(eval);
            if (!match.find()) {
                return eval;
            }
            String var = match.group();
            var = var.substring(2, var.length() - 1); // remove ${ .. }
            String val = null;
            try {
                val = System.getProperty(var);
            } catch (SecurityException se) {
                LOG.warn("Unexpected SecurityException in Configuration", se);
            }
            if (val == null) {
                val = getRaw();
            }
            if (val == null) {
                return eval; // return literal ${var}: var is unbound
            }
            // substitute
            eval = eval.substring(0, match.start()) + val + eval.substring(match.end());
        }
        throw new IllegalStateException("Variable substitution depth too large: " + MAX_SUBST + " " + expr);
    }

    enum ParsedTimeDuration {
        NS {
            TimeUnit unit() {
                return TimeUnit.NANOSECONDS;
            }

            String suffix() {
                return "ns";
            }
        },
        US {
            TimeUnit unit() {
                return TimeUnit.MICROSECONDS;
            }

            String suffix() {
                return "us";
            }
        },
        MS {
            TimeUnit unit() {
                return TimeUnit.MILLISECONDS;
            }

            String suffix() {
                return "ms";
            }
        },
        S {
            TimeUnit unit() {
                return TimeUnit.SECONDS;
            }

            String suffix() {
                return "s";
            }
        },
        M {
            TimeUnit unit() {
                return TimeUnit.MINUTES;
            }

            String suffix() {
                return "m";
            }
        },
        H {
            TimeUnit unit() {
                return TimeUnit.HOURS;
            }

            String suffix() {
                return "h";
            }
        },
        D {
            TimeUnit unit() {
                return TimeUnit.DAYS;
            }

            String suffix() {
                return "d";
            }
        };

        static ParsedTimeDuration unitFor(String s) {
            for (ParsedTimeDuration ptd : values()) {
                // iteration order is in decl order, so SECONDS matched last
                if (s.endsWith(ptd.suffix())) {
                    return ptd;
                }
            }
            return null;
        }

        static ParsedTimeDuration unitFor(TimeUnit unit) {
            for (ParsedTimeDuration ptd : values()) {
                if (ptd.unit() == unit) {
                    return ptd;
                }
            }
            return null;
        }

        abstract TimeUnit unit();

        abstract String suffix();
    }

    private static abstract class NegativeCacheSentinel {
    }

    /**
     * A class that represents a set of positive integer ranges. It parses
     * strings of the form: "2-3,5,7-" where ranges are separated by comma and
     * the lower/upper bounds are separated by dash. Either the lower or upper
     * bound may be omitted meaning all values up to or over. So the string
     * above means 2, 3, 5, and 7, 8, 9, ...
     */
    public static class IntegerRanges implements Iterable<Integer> {
        List<IntegerRanges.Range> ranges = new ArrayList<IntegerRanges.Range>();

        public IntegerRanges() {
        }

        public IntegerRanges(String newValue) {
            StringTokenizer itr = new StringTokenizer(newValue, ",");
            while (itr.hasMoreTokens()) {
                String rng = itr.nextToken().trim();
                String[] parts = rng.split("-", 3);
                if (parts.length < 1 || parts.length > 2) {
                    throw new IllegalArgumentException("integer range badly formed: " +
                            rng);
                }
                IntegerRanges.Range r = new IntegerRanges.Range();
                r.start = convertToInt(parts[0], 0);
                if (parts.length == 2) {
                    r.end = convertToInt(parts[1], Integer.MAX_VALUE);
                } else {
                    r.end = r.start;
                }
                if (r.start > r.end) {
                    throw new IllegalArgumentException("IntegerRange from " + r.start +
                            " to " + r.end + " is invalid");
                }
                ranges.add(r);
            }
        }

        /**
         * Convert a string to an int treating empty strings as the default value.
         * @param value the string value
         * @param defaultValue the value for if the string is empty
         * @return the desired integer
         */
        private static int convertToInt(String value, int defaultValue) {
            String trim = value.trim();
            if (trim.length() == 0) {
                return defaultValue;
            }
            return Integer.parseInt(trim);
        }

        /**
         * Is the given value in the set of ranges
         * @param value the value to check
         * @return is the value in the ranges?
         */
        public boolean isIncluded(int value) {
            for (IntegerRanges.Range r : ranges) {
                if (r.start <= value && value <= r.end) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if there are no values in this range, else false.
         */
        public boolean isEmpty() {
            return ranges == null || ranges.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (IntegerRanges.Range r : ranges) {
                if (first) {
                    first = false;
                } else {
                    result.append(',');
                }
                result.append(r.start);
                result.append('-');
                result.append(r.end);
            }
            return result.toString();
        }

        @Override
        public Iterator<Integer> iterator() {
            return new IntegerRanges.RangeNumberIterator(ranges);
        }

        private static class Range {
            int start;
            int end;
        }

        private static class RangeNumberIterator implements Iterator<Integer> {
            Iterator<IntegerRanges.Range> internal;
            int at;
            int end;

            public RangeNumberIterator(List<IntegerRanges.Range> ranges) {
                if (ranges != null) {
                    internal = ranges.iterator();
                }
                at = -1;
                end = -2;
            }

            @Override
            public boolean hasNext() {
                if (at <= end) {
                    return true;
                } else if (internal != null) {
                    return internal.hasNext();
                }
                return false;
            }

            @Override
            public Integer next() {
                if (at <= end) {
                    at++;
                    return at - 1;
                } else if (internal != null) {
                    IntegerRanges.Range found = internal.next();
                    if (found != null) {
                        at = found.start;
                        end = found.end;
                        at++;
                        return at - 1;
                    }
                }
                return null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

    }
}
