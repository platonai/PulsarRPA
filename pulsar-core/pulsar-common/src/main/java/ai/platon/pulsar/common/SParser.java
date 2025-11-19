package ai.platon.pulsar.common;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.lang.ref.WeakReference;
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

import static ai.platon.pulsar.common.LogsKt.getLogger;

public class SParser {
    public static final Logger LOG = getLogger(SParser.class);

    public static final Duration INVALID_DURATION = Duration.ofSeconds(Integer.MIN_VALUE);

    private static final Map<ClassLoader, Map<String, WeakReference<Class<?>>>> CACHE_CLASSES = new WeakHashMap<>();

    private static final Class<?> NEGATIVE_CACHE_SENTINEL = NegativeCacheSentinel.class;

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{[^\\}\\$\\u0020]+\\}");
    private static final int MAX_SUBST = 20;

    private ClassLoader classLoader;

    {
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = SParser.class.getClassLoader();
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

    public String get() {
        return substituteVars(value);
    }

    public String getTrimmed() {
        if (null == value) {
            return null;
        } else {
            return value.trim();
        }
    }

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
        if (valueString == null) {
            return defaultValue;
        }

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

    public long getLongBytes(long defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        return TraditionalBinaryPrefix.string2long(valueString);
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

    public void setLong(long value) {
        set(Long.toString(value));
    }

    public float getFloat(float defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        return Float.parseFloat(valueString);
    }

    public void setFloat(float value) {
        set(Float.toString(value));
    }

    public double getDouble(double defaultValue) {
        String valueString = getTrimmed();
        if (valueString == null)
            return defaultValue;
        return Double.parseDouble(valueString);
    }

    public void setDouble(double value) {
        set(Double.toString(value));
    }

    public boolean getBoolean(boolean defaultValue) {
        String valueString = getTrimmed();
        if (null == valueString || valueString.isEmpty()) {
            return defaultValue;
        }

        valueString = valueString.toLowerCase();

        if ("true".equals(valueString)) return true;
        else if ("false".equals(valueString)) return false;
        else return defaultValue;
    }

    public void setBoolean(boolean value) {
        set(Boolean.toString(value));
    }

    public void setBooleanIfUnset(boolean value) {
        setIfUnset(Boolean.toString(value));
    }

    public <T extends Enum<T>> void setEnum(T value) {
        set(value.toString());
    }

    public <T extends Enum<T>> T getEnum(T defaultValue) {
        return null == value
                ? defaultValue
                : Enum.valueOf(defaultValue.getDeclaringClass(), value);
    }

    public void setTimeDuration(long value, TimeUnit unit) {
        set(value + ParsedTimeDuration.unitFor(unit).suffix());
    }

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

    public void setPattern(Pattern pattern) {
        if (null == pattern) {
            set(null);
        } else {
            set(pattern.pattern());
        }
    }

    public IntegerRanges getRange(String defaultValue) {
        return new IntegerRanges(get(defaultValue));
    }

    public Collection<String> getStringCollection() {
        return Strings.getStringCollection(value);
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

    public String[] getStrings() {
        return Strings.getStrings(value);
    }

    public void setStrings(String... values) {
        set(Strings.arrayToString(values));
    }

    public String[] getStrings(String... defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            return Strings.getStrings(value);
        }
    }

    public Collection<String> getTrimmedStringCollection() {
        if (null == value) {
            return Collections.emptyList();
        }
        return Strings.getTrimmedStringCollection(value);
    }

    public String[] getTrimmedStrings() {
        return Strings.getTrimmedStrings(value);
    }

    public String[] getTrimmedStrings(String... defaultValue) {
        if (null == value) {
            return defaultValue;
        } else {
            return Strings.getTrimmedStrings(value);
        }
    }

    public Integer getUint(int defaultValue) {
        int value = getInt(defaultValue);
        if (value < 0) {
            value = defaultValue;
        }
        return value;
    }

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

    public Duration getDuration(Duration defaultValue) {
        if (value == null || value.length() < 2) {
            return defaultValue;
        }

        String upperCase = value.toUpperCase();
        try {
            if (upperCase.startsWith("P") || upperCase.startsWith("-P")) {
                try {
                    return Duration.parse(upperCase);
                } catch (Throwable ignored) {
                    return defaultValue;
                }
            }

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

    public Instant getInstant() {
        return getInstant(Instant.EPOCH);
    }

    public Instant getInstant(Instant defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (NumberUtils.isDigits(value)) {
            return Instant.ofEpochMilli(getLong(defaultValue.toEpochMilli()));
        }

        return DateTimes.parseBestInstant(value);
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

    @Nullable
    public Path getPathOrNull() {
        try {
            if (value != null) {
                Path path = Paths.get(value);
                Files.createDirectories(path.getParent());
                return path;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public Class<?> getClassByName(String name) throws ClassNotFoundException {
        Class<?> ret = getClassByNameOrNull(name);
        if (ret == null) {
            throw new ClassNotFoundException("Class " + name + " not found");
        }
        return ret;
    }

    public Class<?> getClassByNameOrNull(String name) {
        Map<String, WeakReference<Class<?>>> map;

        synchronized (CACHE_CLASSES) {
            map = CACHE_CLASSES.computeIfAbsent(classLoader, k -> Collections.synchronizedMap(new WeakHashMap<>()));
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
                map.put(name, new WeakReference<>(NEGATIVE_CACHE_SENTINEL));
                return null;
            }
            map.put(name, new WeakReference<>(clazz));
            return clazz;
        } else if (clazz == NEGATIVE_CACHE_SENTINEL) {
            return null;
        } else {
            return clazz;
        }
    }

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

    public void setClass(Class<?> theClass, Class<?> xface) {
        if (!xface.isAssignableFrom(theClass))
            throw new RuntimeException(theClass + " not " + xface.getName());
        set(theClass.getName());
    }

    public File getFile(String dirsProp, String path)
            throws IOException {
        String[] dirs = getTrimmedStrings(dirsProp);
        int hashCode = path.hashCode();
        for (int i = 0; i < dirs.length; i++) {
            int index = (hashCode + i & Integer.MAX_VALUE) % dirs.length;
            File file = new File(dirs[index], path);
            File dir = file.getParentFile();
            if (dir.exists() || dir.mkdirs()) {
                return file;
            }
        }
        throw new IOException("No valid local directories in property: " + dirsProp);
    }

    public URL getResource() {
        return classLoader.getResource(value);
    }

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

    public ClassLoader getClassLoader() {
        return classLoader;
    }

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
            var = var.substring(2, var.length() - 1);
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
                return eval;
            }
            eval = eval.substring(0, match.start()) + val + eval.substring(match.end());
        }
        throw new IllegalArgumentException("Variable substitution depth too large: " + MAX_SUBST + " " + expr);
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

        private static int convertToInt(String value, int defaultValue) {
            String trim = value.trim();
            if (trim.length() == 0) {
                return defaultValue;
            }
            return Integer.parseInt(trim);
        }

        public boolean isIncluded(int value) {
            for (IntegerRanges.Range r : ranges) {
                if (r.start <= value && value <= r.end) {
                    return true;
                }
            }
            return false;
        }

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

    public enum TraditionalBinaryPrefix {
        KILO(10),
        MEGA(KILO.bitShift + 10),
        GIGA(MEGA.bitShift + 10),
        TERA(GIGA.bitShift + 10),
        PETA(TERA.bitShift + 10),
        EXA(PETA.bitShift + 10);

        public final long value;
        public final char symbol;
        public final int bitShift;
        public final long bitMask;

        TraditionalBinaryPrefix(int bitShift) {
            this.bitShift = bitShift;
            this.value = 1L << bitShift;
            this.bitMask = this.value - 1L;
            this.symbol = toString().charAt(0);
        }

        public static TraditionalBinaryPrefix valueOf(char symbol) {
            symbol = Character.toUpperCase(symbol);
            for (TraditionalBinaryPrefix prefix : TraditionalBinaryPrefix.values()) {
                if (symbol == prefix.symbol) {
                    return prefix;
                }
            }
            throw new IllegalArgumentException("Unknown symbol '" + symbol + "'");
        }

        public static long string2long(String s) {
            s = s.trim();
            final int lastpos = s.length() - 1;
            final char lastchar = s.charAt(lastpos);
            if (Character.isDigit(lastchar))
                return Long.parseLong(s);
            else {
                long prefix;
                try {
                    prefix = TraditionalBinaryPrefix.valueOf(lastchar).value;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid size prefix '" + lastchar
                            + "' in '" + s
                            + "'. Allowed prefixes are k, m, g, t, p, e(case insensitive)");
                }
                long num = Long.parseLong(s.substring(0, lastpos));
                if (num > (Long.MAX_VALUE / prefix) || num < (Long.MIN_VALUE / prefix)) {
                    throw new IllegalArgumentException(s + " does not fit in a Long");
                }
                return num * prefix;
            }
        }

        public static String long2String(long n, String unit, int decimalPlaces) {
            if (unit == null) {
                unit = "";
            }
            if (n == Long.MIN_VALUE) {
                return "-8 " + EXA.symbol + unit;
            }

            final StringBuilder b = new StringBuilder();
            if (n < 0) {
                b.append('-');
                n = -n;
            }
            if (n < KILO.value) {
                b.append(n);
                return (unit.isEmpty() ? b : b.append(" ").append(unit)).toString();
            } else {
                int i = 0;
                for (; i < values().length && n >= values()[i].value; i++) ;
                TraditionalBinaryPrefix prefix = values()[i - 1];

                if ((n & prefix.bitMask) == 0) {
                    b.append(n >> prefix.bitShift);
                } else {
                    final String format = "%." + decimalPlaces + "f";
                    String s = format(format, n / (double) prefix.value);
                    if (s.startsWith("1024")) {
                        prefix = values()[i];
                        s = format(format, n / (double) prefix.value);
                    }
                    b.append(s);
                }
                return b.append(' ').append(prefix.symbol).append(unit).toString();
            }
        }
    }

    private static String format(final String format, final Object... objects) {
        return String.format(Locale.ENGLISH, format, objects);
    }
}
