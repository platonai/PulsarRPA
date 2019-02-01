package ai.platon.pulsar.persist;

import ai.platon.pulsar.common.SParser;
import ai.platon.pulsar.common.config.ImmutableConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by vincent on 17-4-9.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class PageCounters {

    public static String DELIMITER = "'";
    public static Map<Class, String> COUNTER_GROUPS = new HashMap<>();

    static {
        COUNTER_GROUPS.put(Self.class, "s");
        COUNTER_GROUPS.put(Ref.class, "r");
    }

    private Map<CharSequence, Integer> pageCounters;

    private PageCounters(Map<CharSequence, Integer> pageCounters) {
        this.pageCounters = pageCounters;
    }

    /**
     * Experimental
     */
    public static void loadCounterGroups(ImmutableConfig conf) {
        conf.getKvs("pulsar.stat.page.counters").entrySet().stream()
                .map(e -> Pair.of(SParser.wrap(e.getKey()).getClass(Object.class), e.getValue()))
                .filter(e -> e.getKey().equals(Object.class))
                .filter(e -> StringUtils.isNoneBlank(e.getValue()))
                .forEach(e -> COUNTER_GROUPS.put(e.getKey(), e.getValue()));
    }

    public static <E extends Enum<E>> String getGroup(Enum<E> counter) {
        return COUNTER_GROUPS.getOrDefault(counter.getClass(), "");
    }

    @Nonnull
    public static <E extends Enum<E>> String getFullName(Enum<E> counter) {
        String group = getGroup(counter);
        return (group.isEmpty() ? "" : (group + DELIMITER)) + counter.name();
    }

    public static <E extends Enum<E>> String getSlimName(Enum<E> counter) {
        return counter.name();
    }

    @Nonnull
    public static String getSlimName(String counterName) {
        return StringUtils.substringAfter(counterName, DELIMITER);
    }

    @Nonnull
    public static PageCounters box(Map<CharSequence, Integer> pageCounters) {
        Objects.requireNonNull(pageCounters);
        return new PageCounters(pageCounters);
    }

    public Map<CharSequence, Integer> unbox() {
        return pageCounters;
    }

    public <E extends Enum<E>> void set(Enum<E> counter, int value) {
        set(getFullName(counter), value);
    }

    public void set(String name, int value) {
        if (value == 0) {
            return;
        }
        pageCounters.put(WebPage.u8(name), value);
    }

    public int get(String name) {
        return pageCounters.getOrDefault(WebPage.u8(name), 0);
    }

    public <E extends Enum<E>> int get(Enum<E> counter) {
        return get(getFullName(counter));
    }

    public <E extends Enum<E>> void increase(String counterName) {
        set(counterName, get(counterName) + 1);
    }

    public <E extends Enum<E>> void increase(Enum<E> counter) {
        set(counter, get(counter) + 1);
    }

    public <E extends Enum<E>> void increase(Enum<E> counter, int value) {
        if (value == 0) {
            return;
        }
        set(counter, get(counter) + value);
    }

    public Map<String, String> asStringMap() {
        return pageCounters.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString(), (e, e2) -> e));
    }

    @Override
    public PageCounters clone() {
        return new PageCounters(new HashMap<>(this.pageCounters));
    }

    @Override
    public String toString() {
        return pageCounters.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining(", "));
    }

    /**
     * Counters accumulated by the page itself
     */
    public enum Self {
        noArticle,
        fetchErr, parseErr, extractErr, indexErr,
        missingFields, brokenSubEntity
    }

    /**
     * Counters accumulated by the incoming pages
     */
    public enum Ref {
        fetchErr, parseErr, extractErr, indexErr,
        link,
        ch, article, page,
        entity, subEntity,
        missingEntity, missingFields, brokenEntity, brokenSubEntity,
        missingEntityLastRound, missingFieldsLastRound, brokenEntityLastRound, brokenSubEntityLastRound
    }
}
