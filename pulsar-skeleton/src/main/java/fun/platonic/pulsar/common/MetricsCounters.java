/*******************************************************************************
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
 ******************************************************************************/
package fun.platonic.pulsar.common;

import com.beust.jcommander.internal.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fun.platonic.pulsar.common.config.CapabilityTypes.STAT_PULSAR_STATUS;

/**
 * TODO: Use org.apache.hadoop.metrics2
 */
public class MetricsCounters {

    public static Logger LOG = LoggerFactory.getLogger(MetricsCounters.class);

    public static int MAX_GROUPS = 100;
    public static int MAX_COUNTERS_IN_GROUP = 1000;
    public static int MAX_COUNTERS = MAX_GROUPS * MAX_COUNTERS_IN_GROUP;
    public static String DELIMETER = "'";

    private static int id;
    private static String hostname;

    private static AtomicInteger counterGroupSequence = new AtomicInteger(0);
    private static AtomicInteger counterSequence = new AtomicInteger(0);
    // Thread safe for read/write at index
    private static ArrayList<Pair<Class, Integer>> registeredClasses = new ArrayList<>();
    // Thread safe for read/write at index
    private static ArrayList<String> counterNames = new ArrayList<>(MAX_COUNTERS);
    // Thread safe for read/write at index
    private static ArrayList<AtomicInteger> globalCounters = new ArrayList<>(MAX_COUNTERS);
    // Thread safe for read/write at index
    private static ArrayList<AtomicInteger> nativeCounters = new ArrayList<>(MAX_COUNTERS);

    static {
        id = counterSequence.incrementAndGet();
        hostname = NetUtil.getHostname();

        IntStream.range(0, MAX_COUNTERS).forEach(i -> {
            counterNames.add("");
            globalCounters.add(new AtomicInteger(0));
            nativeCounters.add(new AtomicInteger(0));
        });
    }

    public MetricsCounters() {
    }

    /**
     * Register a counter, return the group id of this counter
     *
     * @param counterClass The counter enum class
     * @return group id
     */
    public synchronized static <T extends Enum<T>> int register(Class<T> counterClass) {
        // TODO : use annotation
        int groupId = getGroup(counterClass);
        if (groupId > 0) {
            return groupId;
        }
        groupId = counterGroupSequence.incrementAndGet();

        registeredClasses.add(Pair.of(counterClass, groupId));

        for (final T e : counterClass.getEnumConstants()) {
            int counterIndex = groupId * MAX_COUNTERS_IN_GROUP + e.ordinal();
            String counterName = groupId + DELIMETER + e.name();

            counterNames.set(counterIndex, counterName);
            globalCounters.set(counterIndex, new AtomicInteger(0));
            nativeCounters.set(counterIndex, new AtomicInteger(0));
        }

        return groupId;
    }

    public static <T extends Enum<T>> int getGroup(T counter) {
        return getGroup(counter.getClass());
    }

    public static <T extends Enum<T>> int getGroup(Class<T> counterClass) {
        Pair<Class, Integer> entry = CollectionUtils.find(registeredClasses, c -> c.getKey().equals(counterClass));
        return entry == null ? -1 : entry.getValue();
    }

    public static <T extends Enum<T>> String getName(Enum<T> e) {
        int groupId = getGroup(e.getClass());
        return groupId + DELIMETER + e.name();
    }

    public String getRegisteredCounters() {
        return registeredClasses.stream().map(Pair::getKey).map(Class::toString).collect(Collectors.joining(", "));
    }

    public void reset() {
        IntStream.range(0, MAX_COUNTERS).forEach(i -> {
            counterNames.add("");
            globalCounters.add(new AtomicInteger(0));
            nativeCounters.add(new AtomicInteger(0));
        });
    }

    public final int id() {
        return id;
    }

    public final String getHostname() {
        return hostname;
    }

    public void increase(Enum<?> counter) {
        increase(getIndex(counter));
    }

    public void increase(Enum<?> counter, int value) {
        increase(getIndex(counter), value);
    }

    public void increase(int group, Enum<?> counter) {
        increase(group, counter, 1);
    }

    public void increase(int group, Enum<?> counter, int value) {
        increase(getIndexUnchecked(group, counter), value);
    }

    public void setValue(Enum<?> counter, int value) {
        setValue(getIndex(counter), value);
    }

    public void setValue(int group, Enum<?> counter, int value) {
        setValue(getIndexUnchecked(group, counter), value);
    }

    public int getIndexUnchecked(int group, Enum<?> counter) {
        return group * MAX_COUNTERS_IN_GROUP + counter.ordinal();
    }

    /**
     * Get counter index
     * <p>
     * Search over small vector is very fast, even faster than small tree.
     */
    public int getIndex(Enum<?> counter) {
        Pair<Class, Integer> entry = CollectionUtils.find(registeredClasses, c -> c.getKey().equals(counter.getClass()));
        if (entry == null) {
            LOG.warn("Counter does not registered : " + counter.getClass().getName());
            return -1;
        }
        return getIndexUnchecked(entry.getValue(), counter);
    }

    public int get(int index) {
        if (!validate(index)) return 0;

        return nativeCounters.get(index).get();
    }

    public int get(Enum<?> counter) {
        return get(getIndex(counter));
    }

    public String getStatus(Set<String> names, boolean verbose) {
        StringBuilder sb = new StringBuilder();

        IntStream.range(0, MAX_COUNTERS).forEach(i -> {
            String name = counterNames.get(i);
            if (!name.isEmpty() && (names.isEmpty() || names.contains(name))) {
                int value = nativeCounters.get(i).get();

                if (value != 0) {
                    if (!verbose) {
                        name = StringUtils.substringAfter(name, DELIMETER);
                    }
                    sb.append(", ").append(name).append(":").append(value);
                }
            }
        });

        // remove heading ", "
        sb.delete(0, ", ".length());
        return sb.toString();
    }

    public String getStatus(boolean verbose) {
        return getStatus(Sets.newHashSet(), verbose);
    }

    public void accumulateGlobalCounters(TaskInputOutputContext context) {
        if (context == null) {
            return;
        }

        IntStream.range(0, MAX_COUNTERS).forEach(i -> {
            String name = counterNames.get(i);
            int value = globalCounters.get(i).getAndSet(0);

            if (!name.isEmpty() && value != 0) {
                // log.debug("global : " + name + " : " + value);
                context.getCounter(STAT_PULSAR_STATUS, name).increment(value);
            }
        });
    }

    protected void increase(int index) {
        if (!validate(index)) {
            return;
        }

        globalCounters.get(index).incrementAndGet();
        nativeCounters.get(index).incrementAndGet();

        // log.info("#" + index + " : " + nativeCounters.get(index).get());
    }

    protected void increase(int index, int value) {
        if (!validate(index)) {
            LOG.warn("Failed to increase unknown counter at position " + index);
            return;
        }

        globalCounters.get(index).addAndGet(value);
        nativeCounters.get(index).addAndGet(value);
    }

    protected void increaseAll(int... indexes) {
        for (int index : indexes) {
            increase(index);
        }
    }

    protected void setValue(int index, int value) {
        if (!validate(index)) {
            return;
        }

        globalCounters.get(index).set(value);
        nativeCounters.get(index).set(value);
    }

    private boolean validate(int index) {
        if (index < 0 || index >= nativeCounters.size()) {
            LOG.error("Counter index out of range #" + index);
            return false;
        }

        return true;
    }
}
