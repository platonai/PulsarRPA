package ai.platon.pulsar.common.event

import ai.platon.pulsar.common.warnInterruptible
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractEventEmitter<EventType>: EventEmitter<EventType> {
    protected val listenerMap = ConcurrentHashMap<EventType, CopyOnWriteArrayList<Function<Any>>>()

    val listeners: Map<EventType, List<Function<Any>>> get() = listenerMap

    open var eventExceptionHandler: (Throwable) -> Unit = {
        warnInterruptible(AbstractEventEmitter::class, it)
    }

    override fun on(event: EventType, handler: () -> Any): AbstractEventEmitter<EventType> {
        listenerMap.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun on1(event: EventType, handler: suspend () -> Any): AbstractEventEmitter<EventType> {
        listenerMap.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T> on(event: EventType, handler: (T) -> Any): AbstractEventEmitter<EventType> {
        listenerMap.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T> on1(event: EventType, handler: suspend (T) -> Any): AbstractEventEmitter<EventType> {
        listenerMap.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T, T2> on(event: EventType, handler: (T, T2) -> Any): AbstractEventEmitter<EventType> {
        listenerMap.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T, T2> on1(event: EventType, handler: suspend (T, T2) -> Any): AbstractEventEmitter<EventType> {
        listenerMap.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T, T2, T3> on(event: EventType, handler: (T, T2, T3) -> Any): AbstractEventEmitter<EventType> {
        listenerMap.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun <T, T2, T3> on1(event: EventType, handler: suspend (T, T2, T3) -> Any): AbstractEventEmitter<EventType> {
        listenerMap.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(handler)
        return this
    }

    override fun emit(event: EventType): List<Any> {
        val l = listenerMap[event]?.filterIsInstance<() -> Any>() ?: return listOf()
        return l.map { runCatching { it() }.onFailure(eventExceptionHandler).getOrElse { it } }
    }

    /**
     * In the following example, emit1 is misused, should be emit
     * suspend fun navigateTo(entry: NavigateEntry) {
     *   browser.emit1(BrowserEvents.willNavigate, entry)
     *   navigateHistory.add(entry)
     * }
     * */
    override suspend fun emit1(event: EventType): List<Any> {
        val l = listenerMap[event]?.filterIsInstance<suspend () -> Any>() ?: return listOf()
        return l.map { runCatching { it() }.onFailure(eventExceptionHandler).getOrElse { it } }
    }

    override fun <T> emit(event: EventType, param: T): List<Any> {
        val l = listenerMap[event]?.filterIsInstance<(T) -> Any>() ?: return listOf()
        return l.map { runCatching { it(param) }.onFailure(eventExceptionHandler).getOrElse { it } }
    }

    override suspend fun <T> emit1(event: EventType, param: T): List<Any> {
        val l = listenerMap[event]?.filterIsInstance<suspend (T) -> Any>() ?: return listOf()
        return l.map { runCatching { it(param) }.onFailure(eventExceptionHandler).getOrElse { it } }
    }

    override fun <T, T2> emit(event: EventType, param: T, param2: T2): List<Any> {
        val l = listenerMap[event]?.filterIsInstance<(T, T2) -> Any>() ?: return listOf()
        return l.map { runCatching { it(param, param2) }.onFailure(eventExceptionHandler).getOrElse { it } }
    }

    override suspend fun <T, T2> emit1(event: EventType, param: T, param2: T2): List<Any> {
        val l = listenerMap[event]?.filterIsInstance<suspend (T, T2) -> Any>() ?: return listOf()
        return l.map { runCatching { it(param, param2) }.onFailure(eventExceptionHandler).getOrElse { it } }
    }

    override fun <T, T2, T3> emit(event: EventType, param: T, param2: T2, param3: T3): List<Any> {
        val l = listenerMap[event]?.filterIsInstance<(T, T2, T3) -> Any>() ?: return listOf()
        return l.map { runCatching { it(param, param2, param3) }.onFailure(eventExceptionHandler).getOrElse { it } }
    }

    override suspend fun <T, T2, T3> emit1(event: EventType, param: T, param2: T2, param3: T3): List<Any> {
        val l = listenerMap[event]?.filterIsInstance<suspend (T, T2, T3) -> Any>() ?: return listOf()
        return l.map { runCatching { it(param, param2, param3) }.onFailure(eventExceptionHandler).getOrElse { it } }
    }

    override fun off(event: EventType): AbstractEventEmitter<EventType> {
        listenerMap.remove(event)
        return this
    }

    override fun off1(event: EventType): AbstractEventEmitter<EventType> {
        listenerMap.remove(event)
        return this
    }

    override fun <T> off(event: EventType, handler: () -> Any): AbstractEventEmitter<EventType> {
        val list = listenerMap[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            listenerMap.remove(event)
        }
        return this
    }

    override fun <T> off1(event: EventType, handler: suspend () -> Any): AbstractEventEmitter<EventType> {
        val list = listenerMap[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            listenerMap.remove(event)
        }
        return this
    }

    override fun <T> off(event: EventType, handler: (T) -> Any): AbstractEventEmitter<EventType> {
        val list = listenerMap[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            listenerMap.remove(event)
        }
        return this
    }

    override fun <T> off1(event: EventType, handler: suspend (T) -> Any): AbstractEventEmitter<EventType> {
        val list = listenerMap[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            listenerMap.remove(event)
        }
        return this
    }

    override fun <T, T2> off(event: EventType, handler: (T, T2) -> Any): AbstractEventEmitter<EventType> {
        val list = listenerMap[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            listenerMap.remove(event)
        }
        return this
    }

    override fun <T, T2> off1(event: EventType, handler: suspend (T, T2) -> Any): AbstractEventEmitter<EventType> {
        val list = listenerMap[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            listenerMap.remove(event)
        }
        return this
    }

    override fun <T, T2, T3> off(event: EventType, handler: (T, T2, T3) -> Any): AbstractEventEmitter<EventType> {
        val list = listenerMap[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            listenerMap.remove(event)
        }
        return this
    }

    override fun <T, T2, T3> off1(event: EventType, handler: suspend (T, T2, T3) -> Any): AbstractEventEmitter<EventType> {
        val list = listenerMap[event] ?: return this
        list.removeAll { it == handler }
        if (list.isEmpty()) {
            listenerMap.remove(event)
        }
        return this
    }

    override fun <T> once(event: EventType, handler: (T) -> Any, param: T): AbstractEventEmitter<EventType> {
        val onceHandler: (T) -> Any = { _ ->
            handler(param)
            off(event, handler)
        }

        return on(event, onceHandler)
    }

    override fun <T> once1(event: EventType, handler: suspend (T) -> Any, param: T): AbstractEventEmitter<EventType> {
        val onceHandler: suspend (T) -> Any = { _ ->
            handler(param)
            off1(event, handler)
        }

        return on1(event, onceHandler)
    }

    override fun <T, T2> once(
        event: EventType, handler: (T, T2) -> Any, param: T, param2: T2
    ): AbstractEventEmitter<EventType> {
        val onceHandler: (T, T2) -> Any = { _, _ ->
            handler(param, param2)
            off(event, handler)
        }

        return on(event, onceHandler)
    }

    override fun <T, T2> once1(
        event: EventType, handler: suspend (T, T2) -> Any, param: T, param2: T2
    ): AbstractEventEmitter<EventType> {
        val onceHandler: suspend (T, T2) -> Any = { _, _ ->
            handler(param, param2)
            off1(event, handler)
        }

        return on1(event, onceHandler)
    }

    override fun <T, T2, T3> once(
        event: EventType, handler: (T, T2, T3) -> Any, param: T, param2: T2, param3: T3
    ): AbstractEventEmitter<EventType> {
        val onceHandler: (T, T2, T3) -> Any = { _, _, _ ->
            handler(param, param2, param3)
            off(event, handler)
        }

        return on(event, onceHandler)
    }

    override fun <T, T2, T3> once1(
        event: EventType, handler: suspend (T, T2, T3) -> Any, param: T, param2: T2, param3: T3
    ): AbstractEventEmitter<EventType> {
        val onceHandler: suspend (T, T2, T3) -> Any = { _, _, _ ->
            handler(param, param2, param3)
            off1(event, handler)
        }

        return on1(event, onceHandler)
    }

    override fun hasListeners(event: EventType) = listenerMap.containsKey(event)

    override fun listeners(): List<Function<Any>> = this.listenerMap.values.flatten()

    override fun listeners(event: EventType) = this.listenerMap[event]?.toList() ?: listOf()

    override fun count(event: EventType) = listenerMap[event]?.size ?: 0
}
