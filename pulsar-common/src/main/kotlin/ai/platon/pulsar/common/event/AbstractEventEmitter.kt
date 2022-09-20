package ai.platon.pulsar.common.event

import ai.platon.pulsar.common.lang.*
import java.util.concurrent.ConcurrentHashMap

interface EventEmitter<EventType> {
    /**
     * Attach event handlers
     * */
    fun attach()
    /**
     * Detach event handlers
     * */
    fun detach()

    /**
     * Bind an event listener to fire when an event occurs.
     * @param event - the event type you'd like to listen to. Can be a string or symbol.
     * @param handler  - the function to be called when the event occurs.
     * @returns `this` to enable you to chain method calls.
     */
    fun on(event: EventType, handler: () -> Unit): EventEmitter<EventType>
    fun on1(event: EventType, handler: suspend () -> Unit): EventEmitter<EventType>

    /**
     * Bind an event listener to fire when an event occurs.
     * @param event - the event type you'd like to listen to. Can be a string or symbol.
     * @param handler  - the function to be called when the event occurs.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T> on(event: EventType, handler: (T) -> Unit): EventEmitter<EventType>
    fun <T> on1(event: EventType, handler: suspend (T) -> Unit): EventEmitter<EventType>

    /**
     * Bind an event listener to fire when an event occurs.
     * @param event - the event type you'd like to listen to. Can be a string or symbol.
     * @param handler  - the function to be called when the event occurs.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2> on(event: EventType, handler: (T, T2) -> Unit): EventEmitter<EventType>
    fun <T, T2> on1(event: EventType, handler: suspend (T, T2) -> Unit): EventEmitter<EventType>

    /**
     * Bind an event listener to fire when an event occurs.
     * @param event - the event type you'd like to listen to. Can be a string or symbol.
     * @param handler  - the function to be called when the event occurs.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2, T3> on(event: EventType, handler: (T, T2, T3) -> Unit): EventEmitter<EventType>
    fun <T, T2, T3> on1(event: EventType, handler: suspend (T, T2, T3) -> Unit): EventEmitter<EventType>

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param eventData - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun emit(event: EventType): Boolean
    suspend fun emit1(event: EventType): Boolean

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param eventData - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun <T> emit(event: EventType, param: T): Boolean
    suspend fun <T> emit1(event: EventType, param: T): Boolean

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param eventData - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun <T, T2> emit(event: EventType, param: T, param2: T2): Boolean
    suspend fun <T, T2> emit1(event: EventType, param: T, param2: T2): Boolean

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param eventData - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun <T, T2, T3> emit(event: EventType, param: T, param2: T2, param3: T3): Boolean
    suspend fun <T, T2, T3> emit1(event: EventType, param: T, param2: T2, param3: T3): Boolean

    /**
     * Removes all listeners. If given an event argument, it will remove only
     * listeners for that event.
     * @param event - the event to remove listeners for.
     * @returns `this` to enable you to chain method calls.
     */
    fun off(event: EventType): EventEmitter<EventType>

    /**
     * Remove an event listener from firing.
     * @param event - the event type you'd like to stop listening to.
     * @param handler  - the function that should be removed.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T> off(event: EventType, handler: () -> Unit): EventEmitter<EventType>
    fun <T> off1(event: EventType, handler: suspend () -> Unit): EventEmitter<EventType>

    /**
     * Remove an event listener from firing.
     * @param event - the event type you'd like to stop listening to.
     * @param handler  - the function that should be removed.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T> off(event: EventType, handler: (T) -> Unit): EventEmitter<EventType>
    fun <T> off1(event: EventType, handler: suspend (T) -> Unit): EventEmitter<EventType>

    /**
     * Remove an event listener from firing.
     * @param event - the event type you'd like to stop listening to.
     * @param handler  - the function that should be removed.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2> off(event: EventType, handler: (T, T2) -> Unit): EventEmitter<EventType>
    fun <T, T2> off1(event: EventType, handler: suspend (T, T2) -> Unit): EventEmitter<EventType>

    /**
     * Remove an event listener from firing.
     * @param event - the event type you'd like to stop listening to.
     * @param handler  - the function that should be removed.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2, T3> off(event: EventType, handler: (T, T2, T3) -> Unit): EventEmitter<EventType>
    fun <T, T2, T3> off1(event: EventType, handler: suspend (T, T2, T3) -> Unit): EventEmitter<EventType>

    /**
     * Like `on` but the listener will only be fired once and then it will be removed.
     * @param event - the event you'd like to listen to
     * @param handler - the handler function to run when the event occurs
     * @returns `this` to enable you to chain method calls.
     */
    fun <T> once(event: EventType, handler: (T) -> Unit, param: T): EventEmitter<EventType>
    fun <T> once1(event: EventType, handler: suspend (T) -> Unit, param: T): EventEmitter<EventType>
    /**
     * Like `on` but the listener will only be fired once and then it will be removed.
     * @param event - the event you'd like to listen to
     * @param handler - the handler function to run when the event occurs
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2> once(event: EventType, handler: (T, T2) -> Unit, param: T, param2: T2): EventEmitter<EventType>
    fun <T, T2> once1(event: EventType, handler: suspend (T, T2) -> Unit, param: T, param2: T2): EventEmitter<EventType>
    /**
     * Like `on` but the listener will only be fired once and then it will be removed.
     * @param event - the event you'd like to listen to
     * @param handler - the handler function to run when the event occurs
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2, T3> once(event: EventType, handler: (T, T2, T3) -> Unit, param: T, param2: T2, param3: T3): EventEmitter<EventType>
    fun <T, T2, T3> once1(event: EventType, handler: suspend (T, T2, T3) -> Unit, param: T, param2: T2, param3: T3): EventEmitter<EventType>

    fun hasListeners(event: EventType): Boolean
    /**
     * Gets the number of listeners for a given event.
     *
     * @param event - the event to get the listener count for
     * @returns the number of listeners bound to the given event
     */
    fun count(event: EventType): Int
}

abstract class AbstractEventEmitter<EventType>: EventEmitter<EventType> {
    private val events = ConcurrentHashMap<EventType, MutableList<Any>>()

    /**
     * Attach event handlers
     * */
    abstract override fun attach()
    /**
     * Detach event handlers
     * */
    abstract override fun detach()

    override fun on(event: EventType, handler: () -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { ArrayList() }.add(handler)
        return this
    }

    override fun on1(event: EventType, handler: suspend () -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { ArrayList() }.add(handler)
        return this
    }

    override fun <T> on(event: EventType, handler: (T) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { ArrayList() }.add(handler)
        return this
    }

    override fun <T> on1(event: EventType, handler: suspend (T) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { ArrayList() }.add(handler)
        return this
    }

    override fun <T, T2> on(event: EventType, handler: (T, T2) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { ArrayList() }.add(handler)
        return this
    }

    override fun <T, T2> on1(event: EventType, handler: suspend (T, T2) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { ArrayList() }.add(handler)
        return this
    }

    override fun <T, T2, T3> on(event: EventType, handler: (T, T2, T3) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { ArrayList() }.add(handler)
        return this
    }

    override fun <T, T2, T3> on1(event: EventType, handler: suspend (T, T2, T3) -> Unit): AbstractEventEmitter<EventType> {
        events.computeIfAbsent(event) { ArrayList() }.add(handler)
        return this
    }

    override fun emit(event: EventType): Boolean {
        val l = events[event]?.filterIsInstance<() -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it() }.onFailure { it.printStackTrace() } }
        return true
    }

    override suspend fun emit1(event: EventType): Boolean {
        val l = events[event]?.filterIsInstance<suspend () -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it() }.onFailure { it.printStackTrace() } }
        return true
    }

    override fun <T> emit(event: EventType, param: T): Boolean {
        val l = events[event]?.filterIsInstance<(T) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param) }.onFailure { it.printStackTrace() } }
        return true
    }

    override suspend fun <T> emit1(event: EventType, param: T): Boolean {
        val l = events[event]?.filterIsInstance<suspend (T) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param) }.onFailure { it.printStackTrace() } }
        return true
    }

    override fun <T, T2> emit(event: EventType, param: T, param2: T2): Boolean {
        val l = events[event]?.filterIsInstance<(T, T2) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param, param2) }.onFailure { it.printStackTrace() } }
        return true
    }

    override suspend fun <T, T2> emit1(event: EventType, param: T, param2: T2): Boolean {
        val l = events[event]?.filterIsInstance<suspend (T, T2) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param, param2) }.onFailure { it.printStackTrace() } }
        return true
    }

    override fun <T, T2, T3> emit(event: EventType, param: T, param2: T2, param3: T3): Boolean {
        val l = events[event]?.filterIsInstance<(T, T2, T3) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param, param2, param3) }.onFailure { it.printStackTrace() } }
        return true
    }

    override suspend fun <T, T2, T3> emit1(event: EventType, param: T, param2: T2, param3: T3): Boolean {
        val l = events[event]?.filterIsInstance<suspend (T, T2, T3) -> Unit>() ?: return false
        l.forEach { kotlin.runCatching { it(param, param2, param3) }.onFailure { it.printStackTrace() } }
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
