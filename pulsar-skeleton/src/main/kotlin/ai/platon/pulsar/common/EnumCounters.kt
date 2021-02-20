/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.Pair
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

class EnumCounters {
    companion object {
        val DEFAULT = EnumCounters()
        val DAILY_COUNTERS = EnumCounters()

        fun <T : Enum<T>> register(counterClass: Class<T>) = DEFAULT.register(counterClass)
        fun <T : Enum<T>> getGroup(counterClass: Class<T>) = DEFAULT.getGroup(counterClass)
        fun <T : Enum<T>> getGroup(counter: T) = DEFAULT.getGroup(counter)
    }

    var LOG = LoggerFactory.getLogger(EnumCounters::class.java)
    var MAX_GROUPS = 100
    var MAX_COUNTERS_IN_GROUP = 1000
    var MAX_COUNTERS = MAX_GROUPS * MAX_COUNTERS_IN_GROUP
    var DELIMITER = "'"

    private val counterGroupSequence = AtomicInteger(0)
    private val counterSequence = AtomicInteger(0)

    // Thread safe for read/write at index
    private val registeredClasses = ArrayList<Pair<Class<*>, Int>>()

    // Thread safe for read/write at index
    private val counterNames = ArrayList<String>(MAX_COUNTERS)

    // Thread safe for read/write at index
    private val globalCounters = ArrayList<AtomicInteger>(MAX_COUNTERS)

    // Thread safe for read/write at index
    private val nativeCounters = ArrayList<AtomicInteger>(MAX_COUNTERS)

    init {
        IntRange(0, MAX_COUNTERS - 1).forEach { i: Int ->
            counterNames.add("")
            globalCounters.add(AtomicInteger(0))
            nativeCounters.add(AtomicInteger(0))
        }
    }

    /**
     * Register a counter, return the group id of this counter
     *
     * @param counterClass The counter enum class
     * @return group id
     */
    @Synchronized
    fun <T : Enum<T>> register(counterClass: Class<T>): Int {
        // TODO : use annotation
        var groupId = getGroup(counterClass)
        if (groupId > 0) {
            return groupId
        }
        groupId = counterGroupSequence.incrementAndGet()
        registeredClasses.add(Pair.of(counterClass, groupId))
        for (e in counterClass.enumConstants) {
            val counterIndex = groupId * MAX_COUNTERS_IN_GROUP + e.ordinal
            val counterName = groupId.toString() + DELIMITER + e.name
            counterNames[counterIndex] = counterName
            globalCounters[counterIndex] = AtomicInteger(0)
            nativeCounters[counterIndex] = AtomicInteger(0)
        }
        return groupId
    }

    @Synchronized
    fun <T : Enum<T>> register(counterClass: KClass<T>) = register(counterClass.java)

    fun <T : Enum<T>> getGroup(counter: T): Int {
        return getGroup(counter.javaClass)
    }

    fun <T : Enum<T>> getGroup(counterClass: Class<T>): Int {
        val entry = registeredClasses.firstOrNull { it.key == counterClass }
        return if (entry == null) -1 else entry.value
    }

    fun <T : Enum<T>> getName(e: Enum<T>): String {
        val groupId = getGroup(e.javaClass as Class<T>)
        return groupId.toString() + DELIMITER + e.name
    }

    val id = counterSequence.incrementAndGet()

    val registeredCounters get() = registeredClasses.mapNotNull { it.key }

    fun reset() {
        IntRange(0, MAX_COUNTERS - 1).forEach { _ ->
            counterNames.add("")
            globalCounters.add(AtomicInteger(0))
            nativeCounters.add(AtomicInteger(0))
        }
    }

    @JvmOverloads
    fun inc(counter: Enum<*>, value: Int = 1) {
        inc(getIndex(counter), value)
    }

    @JvmOverloads
    fun inc(vararg counters: Enum<*>, value: Int = 1) {
        counters.forEach { inc(it, value) }
    }

    @JvmOverloads
    fun inc(group: Int, counter: Enum<*>, value: Int = 1) {
        inc(getIndexUnchecked(group, counter), value)
    }

    @JvmOverloads
    fun inc(group: Int, vararg counters: Enum<*>, value: Int = 1) {
        counters.forEach { inc(group, it, value) }
    }

    fun setValue(counter: Enum<*>, value: Int) {
        setValue(getIndex(counter), value)
    }

    fun setValue(group: Int, counter: Enum<*>, value: Int) {
        setValue(getIndexUnchecked(group, counter), value)
    }

    fun getIndexUnchecked(group: Int, counter: Enum<*>): Int {
        return group * MAX_COUNTERS_IN_GROUP + counter.ordinal
    }

    /**
     * Get counter index
     *
     * Search over small vector is very fast, even faster than small tree.
     */
    fun getIndex(counter: Enum<*>): Int {
        val entry = registeredClasses.firstOrNull { it.key == counter.javaClass }
        if (entry == null) {
            LOG.warn("Counter does not registered : " + counter.javaClass.name)
            return -1
        }
        return getIndexUnchecked(entry.value, counter)
    }

    operator fun get(index: Int): Int {
        return if (!validate(index)) 0 else nativeCounters[index].get()
    }

    operator fun get(counter: Enum<*>): Int {
        return get(getIndex(counter))
    }

    fun getStatus(names: Set<String?>, verbose: Boolean): String {
        val sb = StringBuilder()
        IntRange(0, MAX_COUNTERS - 1).forEach { i: Int ->
            var name = counterNames[i]
            if (name.isNotEmpty() && (names.isEmpty() || names.contains(name))) {
                val value = nativeCounters[i].get()
                if (value != 0) {
                    if (!verbose) {
                        name = StringUtils.substringAfter(name, DELIMITER)
                    }
                    sb.append(", ").append(name).append(":").append(value)
                }
            }
        }
        // remove heading ", "
        sb.delete(0, ", ".length)
        return sb.toString()
    }

    fun getStatus(verbose: Boolean): String {
        return getStatus(hashSetOf(), verbose)
    }

    fun asMap(): Map<String, Int> {
        return IntRange(0, MAX_COUNTERS - 1).associate { i ->
            counterNames[i] to nativeCounters[i].get()
        }
    }

    private fun inc(index: Int) {
        if (!validate(index)) {
            return
        }
        globalCounters[index].incrementAndGet()
        nativeCounters[index].incrementAndGet()
    }

    private fun inc(index: Int, value: Int) {
        if (!validate(index)) {
            LOG.warn("Failed to increase unknown counter at position $index")
            return
        }
        globalCounters[index].addAndGet(value)
        nativeCounters[index].addAndGet(value)
    }

    private fun incAll(vararg indexes: Int) {
        for (index in indexes) {
            inc(index)
        }
    }

    private fun setValue(index: Int, value: Int) {
        if (!validate(index)) {
            return
        }
        globalCounters[index].set(value)
        nativeCounters[index].set(value)
    }

    private fun validate(index: Int): Boolean {
        if (index < 0 || index >= nativeCounters.size) {
            LOG.error("Counter index out of range #$index")
            return false
        }
        return true
    }
}
