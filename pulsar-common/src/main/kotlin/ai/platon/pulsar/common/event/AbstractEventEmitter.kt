package ai.platon.pulsar.common.event

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractEventEmitter<EventType>: EventEmitter<EventType> {
    protected val events = ConcurrentHashMap<EventType, CopyOnWriteArrayList<Any>>()

    protected var onFailure = { t: Throwable -> t.printStackTrace() }

    /**
     * Attach event handlers
     * */
    abstract override fun attach()
    /**
     * Detach event handlers
     * */
    abstract override fun detach()

    override fun on(event: EventType, handler: () -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun on1(event: EventType, handler: suspend () -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T> on(event: EventType, handler: (T) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T> on1(event: EventType, handler: suspend (T) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T, T2> on(event: EventType, handler: (T, T2) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T, T2> on1(event: EventType, handler: suspend (T, T2) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T, T2, T3> on(event: EventType, handler: (T, T2, T3) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T, T2, T3> on1(event: EventType, handler: suspend (T, T2, T3) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun emit(event: EventType): Boolean {
        val l = events[event]?.filterIsInstance<() -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it() }.onFailure { it.printStackTrace() } }
        return true
    }

    override suspend fun emit1(event: EventType): Boolean {
        val l = events[event]?.filterIsInstance<suspend () -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it() }.onFailure { onFailure(it) } }
        return true
    }

    override fun <T> emit(event: EventType, param: T): Boolean {
        val l = events[event]?.filterIsInstance<(T) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param) }.onFailure { onFailure(it) } }
        return true
    }

    override suspend fun <T> emit1(event: EventType, param: T): Boolean {
        val l = events[event]?.filterIsInstance<suspend (T) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param) }.onFailure { onFailure(it) } }
        return true
    }

    override fun <T, T2> emit(event: EventType, param: T, param2: T2): Boolean {
        val l = events[event]?.filterIsInstance<(T, T2) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param, param2) }.onFailure { onFailure(it) } }
        return true
    }

    override suspend fun <T, T2> emit1(event: EventType, param: T, param2: T2): Boolean {
        val l = events[event]?.filterIsInstance<suspend (T, T2) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param, param2) }.onFailure { onFailure(it) } }
        return true
    }

    override fun <T, T2, T3> emit(event: EventType, param: T, param2: T2, param3: T3): Boolean {
        val l = events[event]?.filterIsInstance<(T, T2, T3) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param, param2, param3) }.onFailure { onFailure(it) } }
        return true
    }

    override suspend fun <T, T2, T3> emit1(event: EventType, param: T, param2: T2, param3: T3): Boolean {
        val l = events[event]?.filterIsInstance<suspend (T, T2, T3) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param, param2, param3) }.onFailure { onFailure(it) } }
        return true
    }

    override fun off(event: EventType): AbstractEventEmitter<EventType> {
        events.remove(event)
        return this
    }

    override fun <T> off(event: EventType, handler: () -> Unit): AbstractEventEmitter<EventType> {
        val list = events[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            events.remove(event)
        }
        return this
    }

    override fun <T> off1(event: EventType, handler: suspend () -> Unit): AbstractEventEmitter<EventType> {
        val list = events[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            events.remove(event)
        }
        return this
    }

    override fun <T> off(event: EventType, handler: (T) -> Unit): AbstractEventEmitter<EventType> {
        val list = events[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            events.remove(event)
        }
        return this
    }

    override fun <T> off1(event: EventType, handler: suspend (T) -> Unit): AbstractEventEmitter<EventType> {
        val list = events[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            events.remove(event)
        }
        return this
    }

    override fun <T, T2> off(event: EventType, handler: (T, T2) -> Unit): AbstractEventEmitter<EventType> {
        val list = events[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            events.remove(event)
        }
        return this
    }

    override fun <T, T2> off1(event: EventType, handler: suspend (T, T2) -> Unit): AbstractEventEmitter<EventType> {
        val list = events[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            events.remove(event)
        }
        return this
    }

    override fun <T, T2, T3> off(event: EventType, handler: (T, T2, T3) -> Unit): AbstractEventEmitter<EventType> {
        val list = events[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            events.remove(event)
        }
        return this
    }

    override fun <T, T2, T3> off1(event: EventType, handler: suspend (T, T2, T3) -> Unit): AbstractEventEmitter<EventType> {
        val list = events[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            events.remove(event)
        }
        return this
    }

    override fun hasListeners(event: EventType): Boolean {
        return events.containsKey(event)
    }

    override fun count(event: EventType): Int {
        return events[event]?.size ?: 0
    }

    override fun <T> once(
        event: EventType, handler: (T) -> Unit, param: T
    ): AbstractEventEmitter<EventType> {
        val onceHandler: (T) -> Unit = { _ ->
            handler(param)
            off(event, handler)
        }

        return on(event, onceHandler)
    }

    override fun <T> once1(
        event: EventType, handler: suspend (T) -> Unit, param: T
    ): AbstractEventEmitter<EventType> {
        val onceHandler: suspend (T) -> Unit = { _ ->
            handler(param)
            off1(event, handler)
        }

        return on1(event, onceHandler)
    }

    override fun <T, T2> once(
        event: EventType, handler: (T, T2) -> Unit, param: T, param2: T2
    ): AbstractEventEmitter<EventType> {
        val onceHandler: (T, T2) -> Unit = { _, _ ->
            handler(param, param2)
            off(event, handler)
        }

        return on(event, onceHandler)
    }

    override fun <T, T2> once1(
        event: EventType, handler: suspend (T, T2) -> Unit, param: T, param2: T2
    ): AbstractEventEmitter<EventType> {
        val onceHandler: suspend (T, T2) -> Unit = { _, _ ->
            handler(param, param2)
            off1(event, handler)
        }

        return on1(event, onceHandler)
    }

    override fun <T, T2, T3> once(
        event: EventType, handler: (T, T2, T3) -> Unit, param: T, param2: T2, param3: T3
    ): AbstractEventEmitter<EventType> {
        val onceHandler: (T, T2, T3) -> Unit = { _, _, _ ->
            handler(param, param2, param3)
            off(event, handler)
        }

        return on(event, onceHandler)
    }

    override fun <T, T2, T3> once1(
        event: EventType, handler: suspend (T, T2, T3) -> Unit, param: T, param2: T2, param3: T3
    ): AbstractEventEmitter<EventType> {
        val onceHandler: suspend (T, T2, T3) -> Unit = { _, _, _ ->
            handler(param, param2, param3)
            off1(event, handler)
        }

        return on1(event, onceHandler)
    }
}
