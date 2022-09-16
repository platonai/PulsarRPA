package ai.platon.pulsar.common.event

import java.util.concurrent.ConcurrentHashMap

class ListenerCollection<EventType> {
    private val listeners = ConcurrentHashMap<EventType, MutableList<Any>>()

    fun <T> on(type: EventType, listener: (T) -> Unit) {
        listeners.computeIfAbsent(type) { ArrayList() }.add(listener)
    }

    fun <T, T2> on(type: EventType, listener: (T, T2) -> Unit) {
        listeners.computeIfAbsent(type) { ArrayList() }.add(listener)
    }

    fun <T, T2, T3> on(type: EventType, listener: (T, T2, T3) -> Unit) {
        listeners.computeIfAbsent(type) { ArrayList() }.add(listener)
    }

    fun <T> notify(eventType: EventType, param: T) {
        listeners[eventType]?.filterIsInstance<(T) -> Unit>()?.forEach { it(param) }
    }

    fun <T, T2> notify(eventType: EventType, param: T, param2: T2) {
        listeners[eventType]?.filterIsInstance<(T, T2) -> Unit>()?.forEach { it(param, param2) }
    }

    fun <T, T2, T3> notify(eventType: EventType, param: T, param2: T2, param3: T3) {
        listeners[eventType]?.filterIsInstance<(T, T2, T3) -> Unit>()?.forEach { it(param, param2, param3) }
    }

    fun off(type: EventType) {
        listeners.remove(type)
    }

    fun <T> off(type: EventType, listener: (T) -> Unit) {
        val list = listeners[type] ?: return
        list.removeAll { it == listener }
        if (list.isEmpty()) {
            listeners.remove(type)
        }
    }

    fun <T, T2> off(type: EventType, listener: (T, T2) -> Unit) {
        val list = listeners[type] ?: return
        list.removeAll { it == listener }
        if (list.isEmpty()) {
            listeners.remove(type)
        }
    }

    fun <T, T2, T3> off(type: EventType, listener: (T, T2, T3) -> Unit) {
        val list = listeners[type] ?: return
        list.removeAll { it == listener }
        if (list.isEmpty()) {
            listeners.remove(type)
        }
    }

    fun hasListeners(type: EventType): Boolean {
        return listeners.containsKey(type)
    }
}
