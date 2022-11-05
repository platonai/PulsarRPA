package ai.platon.pulsar.common.event

interface EventEmitter<EventType> {

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
     * @param param - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun emit(event: EventType): Boolean
    suspend fun emit1(event: EventType): Boolean

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param param - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun <T> emit(event: EventType, param: T): Boolean
    suspend fun <T> emit1(event: EventType, param: T): Boolean

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param param - any data you'd like to emit with the event
     * @returns `true` if there are any listeners, `false` if there are not.
     */
    fun <T, T2> emit(event: EventType, param: T, param2: T2): Boolean
    suspend fun <T, T2> emit1(event: EventType, param: T, param2: T2): Boolean

    /**
     * Emit an event and call any associated listeners.
     *
     * @param event - the event you'd like to emit
     * @param param - any data you'd like to emit with the event
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
     * Like `on` but the listener will only be fired once, and then it will be removed.
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
