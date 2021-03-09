package ai.platon.pulsar.common.config;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by vincent on 16-9-24.
 *
 * @author vincent
 * @version $Id: $Id
 */
public class Params {
    /** Constant <code>EMPTY_PARAMS</code> */
    public static final Params EMPTY_PARAMS = new Params();

    private Logger log = LoggerFactory.getLogger(Params.class);
    private List<Pair<String, Object>> paramsList = new LinkedList<>();
    private String captionFormat = String.format("%20sParams Table%-25s\n", "----------", "----------");
    private String headerFormat = String.format("%25s   %-25s\n", "Name", "Value");
    private String rowFormat = "%25s: %s";
    private boolean cmdLineStyle = false;
    private List<String> distinctBooleanParams;
    private String pairDelimiter = " ";
    private String kvDelimiter = ": ";
    private Logger defaultLog = null;

    /**
     * <p>Constructor for Params.</p>
     */
    public Params() {
    }

    /**
     * <p>Constructor for Params.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     * @param others a {@link java.lang.Object} object.
     */
    public Params(String key, Object value, Object... others) {
        this.paramsList.addAll(toArgList(key, value, others));
    }

    /**
     * <p>Constructor for Params.</p>
     *
     * @param args a {@link java.util.Map} object.
     */
    public Params(Map<String, Object> args) {
        args.forEach((key, value) -> this.paramsList.add(Pair.of(key, value)));
    }

    /**
     * <p>of.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     * @param others a {@link java.lang.Object} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public static Params of(String key, Object value, Object... others) {
        return new Params(key, value, others);
    }

    /**
     * <p>of.</p>
     *
     * @param args a {@link java.util.Map} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public static Params of(Map<String, Object> args) {
        return new Params(args);
    }

    /**
     * <p>toArgList.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     * @param others a {@link java.lang.Object} object.
     * @return a {@link java.util.List} object.
     */
    public static List<Pair<String, Object>> toArgList(String key, Object value, Object... others) {
        List<Pair<String, Object>> results = new LinkedList<>();

        results.add(Pair.of(key, value));

        if (others == null || others.length < 2) {
            return results;
        }

        if (others.length % 2 != 0) {
            throw new RuntimeException("expected name/value pairs");
        }

        for (int i = 0; i < others.length; i += 2) {
            Object k = others[i];
            Object v = others[i + 1];

            if (k != null && v != null) {
                results.add(Pair.of(String.valueOf(others[i]), others[i + 1]));
            }
        }

        return results;
    }

    /**
     * Convert K/V pairs array into a map.
     *
     * @param others A K/V pairs array, the length of the array must be a even number
     *               null key or null value pair is ignored
     * @return A map contains all non-null key/values
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     */
    public static Map<String, Object> toArgMap(String key, Object value, Object... others) {
        Map<String, Object> results = new LinkedHashMap<>();

        results.put(key, value);

        if (others == null || others.length < 2) {
            return results;
        }

        if (others.length % 2 != 0) {
            throw new RuntimeException("expected name/value pairs");
        }

        for (int i = 0; i < others.length; i += 2) {
            Object k = others[i];
            Object v = others[i + 1];

            if (k != null && v != null) {
                results.put(String.valueOf(others[i]), others[i + 1]);
            }
        }

        return results;
    }

    /**
     * <p>formatAsLine.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     * @param others a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
     */
    public static String formatAsLine(String key, Object value, Object... others) {
        return Params.of(key, value, others).formatAsLine();
    }

    /**
     * <p>format.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     * @param others a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
     */
    public static String format(String key, Object value, Object... others) {
        return Params.of(key, value, others).format();
    }

    /**
     * <p>put.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     */
    public void put(String name, Object value) {
        paramsList.add(Pair.of(name, value));
    }

    /**
     * <p>remove.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean remove(String key) {
        List<Pair<String, Object>> list = paramsList.stream().filter(entry -> !entry.getKey().equals(key)).collect(Collectors.toList());
        boolean removed = list.size() < paramsList.size();
        if (removed) {
            this.paramsList = list;
        }
        return removed;
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public Object get(String name) {
        Pair<String, Object> entry = CollectionUtils.find(paramsList, e -> e.getKey().equals(name));
        return entry == null ? null : entry.getValue();
    }

    /**
     * <p>get.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String get(String name, String defaultValue) {
        String value = (String) get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * <p>getString.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getString(String name) {
        return (String) get(name);
    }

    /**
     * <p>getEnum.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a T object.
     * @param <T> a T object.
     * @return a T object.
     */
    public <T extends Enum<T>> T getEnum(String name, T defaultValue) {
        Object val = this.get(name);
        return null == val ? defaultValue : Enum.valueOf(defaultValue.getDeclaringClass(), val.toString());
    }

    /**
     * <p>getInt.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getInt(String name) {
        return (Integer) get(name);
    }

    /**
     * <p>getInt.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.Integer} object.
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getInt(String name, Integer defaultValue) {
        Integer value = (Integer) get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * <p>getLong.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.Long} object.
     */
    public Long getLong(String name) {
        return (Long) get(name);
    }

    /**
     * <p>getLong.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.Long} object.
     * @return a {@link java.lang.Long} object.
     */
    public Long getLong(String name, Long defaultValue) {
        Long value = (Long) get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * <p>getBoolean.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getBoolean(String name) {
        return (Boolean) get(name);
    }

    /**
     * <p>getBoolean.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.lang.Boolean} object.
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getBoolean(String name, Boolean defaultValue) {
        Boolean value = (Boolean) get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * <p>getStrings.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue an array of {@link java.lang.String} objects.
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getStrings(String name, String[] defaultValue) {
        String valueString = get(name, null);
        if (valueString == null) {
            return defaultValue;
        }
        return org.apache.hadoop.util.StringUtils.getStrings(valueString);
    }

    /**
     * <p>getStringCollection.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param delim a {@link java.lang.String} object.
     * @return a {@link java.util.Collection} object.
     */
    public Collection<String> getStringCollection(String name, String delim) {
        String valueString = get(name, null);
        return org.apache.hadoop.util.StringUtils.getStringCollection(valueString, delim);
    }

    /**
     * <p>getPath.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.nio.file.Path} object.
     * @throws java.io.IOException if any.
     */
    public Path getPath(String name) throws IOException {
        String value = getString(name);
        if (value == null) return null;

        Path path = Paths.get(value);
        Files.createDirectories(path.getParent());

        return path;
    }

    /**
     * <p>getPath.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.nio.file.Path} object.
     * @return a {@link java.nio.file.Path} object.
     * @throws java.io.IOException if any.
     */
    public Path getPath(String name, Path defaultValue) throws IOException {
        String value = getString(name);
        Path path = value == null ? Paths.get(value) : defaultValue;
        Files.createDirectories(path.getParent());
        return path;
    }

    /**
     * <p>getInstant.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.time.Instant} object.
     */
    public Instant getInstant(String name) {
        return (Instant) get(name);
    }

    /**
     * <p>getInstant.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.time.Instant} object.
     * @return a {@link java.time.Instant} object.
     */
    public Instant getInstant(String name, Instant defaultValue) {
        Instant value = (Instant) get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * <p>getDuration.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link java.time.Duration} object.
     */
    public Duration getDuration(String name) {
        return (Duration) get(name);
    }

    /**
     * <p>getDuration.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a {@link java.time.Duration} object.
     * @return a {@link java.time.Duration} object.
     */
    public Duration getDuration(String name, Duration defaultValue) {
        Duration value = (Duration) get(name);
        return value == null ? defaultValue : value;
    }

    /**
     * <p>format.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String format() {
        return format(paramsList);
    }

    /**
     * <p>formatAsLine.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String formatAsLine() {
        return formatAsLine(paramsList);
    }

    /**
     * <p>withCaptionFormat.</p>
     *
     * @param captionFormat a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params withCaptionFormat(String captionFormat) {
        this.captionFormat = captionFormat;
        return this;
    }

    /**
     * <p>withHeaderFormat.</p>
     *
     * @param headerFormat a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params withHeaderFormat(String headerFormat) {
        this.headerFormat = headerFormat;
        return this;
    }

    /**
     * <p>withRowFormat.</p>
     *
     * @param rowFormat a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params withRowFormat(String rowFormat) {
        this.rowFormat = rowFormat;
        return this;
    }

    /**
     * <p>withPairDelimiter.</p>
     *
     * @param pairDelimiter a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params withPairDelimiter(String pairDelimiter) {
        this.pairDelimiter = pairDelimiter;
        return this;
    }

    /**
     * <p>withKVDelimiter.</p>
     *
     * @param kvDelimiter a {@link java.lang.String} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params withKVDelimiter(String kvDelimiter) {
        this.kvDelimiter = kvDelimiter;
        return this;
    }

    /**
     * <p>iscmdLineStyle.</p>
     *
     * @return a boolean.
     */
    public boolean isCmdLineStyle() {
        return cmdLineStyle;
    }

    /**
     * <p>withCmdLineStyle.</p>
     *
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params withCmdLineStyle() {
        return withCmdLineStyle(true);
    }

    /**
     * <p>withCmdLineStyle.</p>
     *
     * @param isCmdLineStyle a boolean.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params withCmdLineStyle(boolean isCmdLineStyle) {
        this.cmdLineStyle = isCmdLineStyle;
        return this;
    }

    public Params withDistinctBooleanParams(List<String> distinctBooleanParams) {
        this.distinctBooleanParams = distinctBooleanParams;
        return this;
    }

    /**
     * <p>sorted.</p>
     *
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params sorted() {
        this.paramsList = this.paramsList.stream()
                .sorted(Comparator.comparing(Pair::getKey))
                .collect(Collectors.toList());
        return this;
    }

    /**
     * <p>filter.</p>
     *
     * @param predicate a {@link java.util.function.Predicate} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params filter(Predicate<Pair<String, Object>> predicate) {
        this.paramsList = this.paramsList.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        return this;
    }

    /**
     * <p>distinct.</p>
     *
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params distinct() {
        this.paramsList = this.paramsList.stream()
                .distinct()
                .collect(Collectors.toList());
        return this;
    }

    /**
     * <p>merge.</p>
     *
     * @param others a {@link ai.platon.pulsar.common.config.Params} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params merge(Params... others) {
        if (others != null && others.length > 0) {
            Arrays.stream(others).forEach(params -> this.paramsList.addAll(params.getParamsList()));
        }
        return this;
    }

    /**
     * <p>merge.</p>
     *
     * @param others a {@link java.util.Collection} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params merge(Collection<Params> others) {
        others.forEach(params -> this.paramsList.addAll(params.getParamsList()));
        return this;
    }

    /**
     * <p>Getter for the field <code>paramsList</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Pair<String, Object>> getParamsList() {
        return paramsList;
    }

    /**
     * <p>asMap.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, Object> asMap() {
        Map<String, Object> result = new HashMap<>();
        paramsList.forEach(p -> result.put(p.getKey(), p.getValue()));
        return result;
    }

    /**
     * <p>asStringMap.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, String> asStringMap() {
        Map<String, String> result = new HashMap<>();
        paramsList.forEach(p -> result.put(p.getKey(), p.getValue().toString()));
        return result;
    }

    /**
     * <p>withLogger.</p>
     *
     * @param logger a {@link org.slf4j.Logger} object.
     * @return a {@link ai.platon.pulsar.common.config.Params} object.
     */
    public Params withLogger(Logger logger) {
        this.defaultLog = logger;
        return this;
    }

    /**
     * <p>debug.</p>
     */
    public void debug() {
        debug(false);
    }

    /**
     * <p>debug.</p>
     *
     * @param inline a boolean.
     */
    public void debug(boolean inline) {
        if (defaultLog != null) {
            defaultLog.debug(inline ? formatAsLine() : format());
        } else {
            log.debug(inline ? formatAsLine() : format());
        }
    }

    /**
     * <p>info.</p>
     */
    public void info() {
        info(false);
    }

    /**
     * <p>info.</p>
     *
     * @param inline a boolean.
     */
    public void info(boolean inline) {
        info("", "", inline);
    }

    /**
     * <p>info.</p>
     *
     * @param prefix a {@link java.lang.String} object.
     * @param postfix a {@link java.lang.String} object.
     * @param inline a boolean.
     */
    public void info(String prefix, String postfix, boolean inline) {
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(inline ? formatAsLine() : format());
        sb.append(postfix);

        if (defaultLog != null) {
            defaultLog.info(sb.toString());
        } else {
            log.info(sb.toString());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return format();
    }

    private String format(List<Pair<String, Object>> params) {
        if (params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append('\n');
        if (captionFormat != null) sb.append(captionFormat);
        if (headerFormat != null) sb.append(headerFormat);
        int i = 0;
        for (Pair<String, Object> param : params) {
            if (i++ > 0) {
                sb.append("\n");
            }

            String key = param.getKey();
            Object value = param.getValue();
            if (value instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) value;
                value = m.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue().toString())
                        .collect(Collectors.joining(", "));
            } else if (value instanceof Collection) {
                Collection<?> c = (Collection<?>) value;
                value = StringUtils.join(c, ", ");
            }

            sb.append(String.format(rowFormat, key, value));
        }

        sb.append('\n');

        return sb.toString();
    }

    private String formatAsLine(List<Pair<String, Object>> params) {
        if (params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (Pair<String, Object> arg : params) {
            if (i++ > 0) {
                sb.append(pairDelimiter);
            }

            String key = arg.getKey();
            String value = arg.getValue().toString();
            if (arg.getValue() == null) {
                sb.append(key);
                if (!cmdLineStyle) {
                    sb.append(kvDelimiter);
                    sb.append("null");
                }
            } else if (cmdLineStyle && key.startsWith("-") && "true".equals(value)) {
                if (distinctBooleanParams != null && distinctBooleanParams.contains(key)) {
                    sb.append(key);
                    sb.append(kvDelimiter);
                    sb.append("true");
                } else {
                    sb.append(key);
                }
            } else if (cmdLineStyle && key.startsWith("-") && "false".equals(value)) {
                if (distinctBooleanParams != null && distinctBooleanParams.contains(key)) {
                    sb.append(key);
                    sb.append(kvDelimiter);
                    sb.append("false");
                }
            } else {
                sb.append(key);
                if (!value.isEmpty()) {
                    sb.append(kvDelimiter);

                    // quoted
                    // @see https://github.com/cbeust/jcommander/issues/458
                    // JCommand no longer removing double quotes when parsing arguments
                    if (value.contains(kvDelimiter) && !value.startsWith("\"") && !value.endsWith("\"")) {
                        sb.append('\"').append(value).append('\"');
                    } else {
                        sb.append(value);
                    }
                }
            }
        }

        return sb.toString().trim();
    }
}
