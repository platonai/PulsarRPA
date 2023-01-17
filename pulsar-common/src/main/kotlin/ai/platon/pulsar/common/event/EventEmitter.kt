package ai.platon.pulsar.common.event

import ai.platon.pulsar.common.ClassReflect
import kotlin.reflect.KFunction

interface EventEmitter<EventType> {

    /**
     * Bind an event listener to fire when an event occurs.
     * @param event - the event type you'd like to listen to. Can be a string or symbol.
     * @param handler  - the function to be called when the event occurs.
     * @returns `this` to enable you to chain method calls.
     */
    fun on(event: EventType, handler: () -> Any): EventEmitter<EventType>
    fun on1(event: EventType, handler: suspend () -> Any): EventEmitter<EventType>

    /**
     * Bind an event listener to fire when an event occurs.
     * @param event - the event type you'd like to listen to. Can be a string or symbol.
     * @param handler  - the function to be called when the event occurs.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T> on(event: EventType, handler: (T) -> Any): EventEmitter<EventType>
    fun <T> on1(event: EventType, handler: suspend (T) -> Any): EventEmitter<EventType>

    /**
     * Bind an event listener to fire when an event occurs.
     * @param event - the event type you'd like to listen to. Can be a string or symbol.
     * @param handler  - the function to be called when the event occurs.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2> on(event: EventType, handler: (T, T2) -> Any): EventEmitter<EventType>
    fun <T, T2> on1(event: EventType, handler: suspend (T, T2) -> Any): EventEmitter<EventType>

    /**
     * Bind an event listener to fire when an event occurs.
     * @param event - the event type you'd like to listen to. Can be a string or symbol.
     * @param handler  - the function to be called when the event occurs.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2, T3> on(event: EventType, handler: (T, T2, T3) -> Any): EventEmitter<EventType>
    fun <T, T2, T3> on1(event: EventType, handler: suspend (T, T2, T3) -> Any): EventEmitter<EventType>

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param param - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun emit(event: EventType): List<Any>
    suspend fun emit1(event: EventType): List<Any>

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param param - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun <T> emit(event: EventType, param: T): List<Any>
    suspend fun <T> emit1(event: EventType, param: T): List<Any>

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param param - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun <T, T2> emit(event: EventType, param: T, param2: T2): List<Any>
    suspend fun <T, T2> emit1(event: EventType, param: T, param2: T2): List<Any>

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param param - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun <T, T2, T3> emit(event: EventType, param: T, param2: T2, param3: T3): List<Any>
    suspend fun <T, T2, T3> emit1(event: EventType, param: T, param2: T2, param3: T3): List<Any>

    /**
     * Removes all listeners. If given an event argument, it will remove only
     * listeners for that event.
     * @param event - the event to remove listeners for.
     * @returns `this` to enable you to chain method calls.
     */
    fun off(event: EventType): EventEmitter<EventType>
    fun off1(event: EventType): EventEmitter<EventType>
    /**
     * Remove an event listener from firing.
     * @param event - the event type you'd like to stop listening to.
     * @param handler  - the function that should be removed.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T> off(event: EventType, handler: () -> Any): EventEmitter<EventType>
    fun <T> off1(event: EventType, handler: suspend () -> Any): EventEmitter<EventType>

    /**
     * Remove an event listener from firing.
     * @param event - the event type you'd like to stop listening to.
     * @param handler  - the function that should be removed.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T> off(event: EventType, handler: (T) -> Any): EventEmitter<EventType>
    fun <T> off1(event: EventType, handler: suspend (T) -> Any): EventEmitter<EventType>

    /**
     * Remove an event listener from firing.
     * @param event - the event type you'd like to stop listening to.
     * @param handler  - the function that should be removed.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2> off(event: EventType, handler: (T, T2) -> Any): EventEmitter<EventType>
    fun <T, T2> off1(event: EventType, handler: suspend (T, T2) -> Any): EventEmitter<EventType>

    /**
     * Remove an event listener from firing.
     * @param event - the event type you'd like to stop listening to.
     * @param handler  - the function that should be removed.
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2, T3> off(event: EventType, handler: (T, T2, T3) -> Any): EventEmitter<EventType>
    fun <T, T2, T3> off1(event: EventType, handler: suspend (T, T2, T3) -> Any): EventEmitter<EventType>

    /**
     * Like `on` but the listener will only be fired once and then it will be removed.
     * @param event - the event you'd like to listen to
     * @param handler - the handler function to run when the event occurs
     * @returns `this` to enable you to chain method calls.
     */
    fun <T> once(event: EventType, handler: (T) -> Any, param: T): EventEmitter<EventType>
    fun <T> once1(event: EventType, handler: suspend (T) -> Any, param: T): EventEmitter<EventType>
    /**
     * Like `on` but the listener will only be fired once and then it will be removed.
     * @param event - the event you'd like to listen to
     * @param handler - the handler function to run when the event occurs
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2> once(event: EventType, handler: (T, T2) -> Any, param: T, param2: T2): EventEmitter<EventType>
    fun <T, T2> once1(event: EventType, handler: suspend (T, T2) -> Any, param: T, param2: T2): EventEmitter<EventType>
    /**
     * Like `on` but the listener will only be fired once, and then it will be removed.
     * @param event - the event you'd like to listen to
     * @param handler - the handler function to run when the event occurs
     * @returns `this` to enable you to chain method calls.
     */
    fun <T, T2, T3> once(event: EventType, handler: (T, T2, T3) -> Any, param: T, param2: T2, param3: T3): EventEmitter<EventType>
    fun <T, T2, T3> once1(event: EventType, handler: suspend (T, T2, T3) -> Any, param: T, param2: T2, param3: T3): EventEmitter<EventType>
    /**
     * Check if the given event listener is registered
     * */
    fun hasListeners(event: EventType): Boolean
    /**
     * Check if the given normal event listener is registered
     * */
    fun hasNormalListeners(event: EventType) = normalListeners(event).isNotEmpty()
    /**
     * Check if the given suspend event listener is registered
     * */
    fun hasSuspendListeners(event: EventType) = suspendListeners(event).isNotEmpty()
    /**
     * Get a list of the registered event listeners
     * */
    fun listeners(): List<Function<Any>>
    /**
     * Get a list of the registered normal event listeners
     * */
    fun normalListeners(): List<Function<Any>> = listeners().filter { ClassReflect.isNormalInvokable(it.javaClass) }
    /**
     * Get a list of the registered suspend event listeners
     * */
    fun suspendListeners(): List<Function<Any>> = listeners().filter { ClassReflect.isSuspendInvokable(it.javaClass) }
    /**
     * Get a list of the registered event listeners with the given type
     * */
    fun listeners(event: EventType): List<Function<Any>>
    /**
     * Get a list of the registered normal event listeners with the given type
     * */
    fun normalListeners(event: EventType): List<Function<Any>> = listeners(event).filter { ClassReflect.isNormalInvokable(it.javaClass) }
    /**
     * Get a list of the registered suspend event listeners with the given type
     * */
    fun suspendListeners(event: EventType): List<Function<Any>> = listeners(event).filter { ClassReflect.isSuspendInvokable(it.javaClass) }
    /**
     * Gets the number of listeners for a given event.
     *
     * @param event - the event to get the listener count for
     * @returns the number of listeners bound to the given event
     */
    fun count(event: EventType): Int
}
