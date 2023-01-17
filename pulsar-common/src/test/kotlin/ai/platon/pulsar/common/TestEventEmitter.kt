package ai.platon.pulsar.common

import ai.platon.pulsar.common.event.AbstractEventEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.*

private enum class FooEvents {
    onWillDoA, onDidA, onWillDoB, onDidB, onWillDoC, onDidC,
    onWillDoADelayed, onDidADelayed, onWillDoBDelayed, onDidBDelayed, onWillDoCDelayed, onDidCDelayed
}

private class FooEventEmitter : AbstractEventEmitter<FooEvents>() {
}

class TestEventEmitter {

    @Test
    fun testEventRegister() {
        val emitter = FooEventEmitter()
        emitter.on(FooEvents.onWillDoA) { 1 + 1 }
        emitter.on1(FooEvents.onWillDoADelayed) { delay(1) }

        assertTrue { emitter.hasListeners(FooEvents.onWillDoA) }
        assertTrue { emitter.hasListeners(FooEvents.onWillDoADelayed) }

        assertEquals(1, emitter.count(FooEvents.onWillDoA))
        assertEquals(1, emitter.count(FooEvents.onWillDoADelayed))

        emitter.off(FooEvents.onWillDoA)
        emitter.off1(FooEvents.onWillDoADelayed)
        assertEquals(0, emitter.count(FooEvents.onWillDoA))
        assertEquals(0, emitter.count(FooEvents.onWillDoADelayed))
    }

    @Test
    fun testEventExists() {
        val emitter = FooEventEmitter()
        emitter.on(FooEvents.onWillDoA) { 1 + 1 }
        emitter.on1(FooEvents.onWillDoADelayed) { delay(1) }

        assertTrue { emitter.hasListeners(FooEvents.onWillDoA) }
        assertTrue { emitter.hasListeners(FooEvents.onWillDoADelayed) }
        assertTrue { emitter.hasNormalListeners(FooEvents.onWillDoA) }
        assertTrue { emitter.hasSuspendListeners(FooEvents.onWillDoADelayed) }
    }

    @Test
    fun testEventLists() {
        val emitter = FooEventEmitter()
        emitter.on(FooEvents.onWillDoA) { 1 + 1 }
        emitter.on1(FooEvents.onWillDoADelayed) { delay(1) }

        assertEquals(2, emitter.listeners().size)
        assertEquals(1, emitter.listeners(FooEvents.onWillDoA).size)
        assertEquals(1, emitter.listeners(FooEvents.onWillDoADelayed).size)

        assertEquals(1, emitter.normalListeners().size)
        assertEquals(1, emitter.normalListeners(FooEvents.onWillDoA).size)

        assertEquals(1, emitter.suspendListeners().size)
        assertEquals(1, emitter.suspendListeners(FooEvents.onWillDoADelayed).size)
    }

    @Test
    fun testEmitEvents() {
        val emitter = FooEventEmitter()
        emitter.on(FooEvents.onWillDoA) { 1 + 1 }
        assertEquals(2, emitter.emit(FooEvents.onWillDoA).first())

        emitter.on1(FooEvents.onWillDoADelayed) { withContext(Dispatchers.Default) { 2 + 3 } }
        assertEquals(5, runBlocking { emitter.emit1(FooEvents.onWillDoADelayed).first() } )
    }

    @Test
    fun testEmitMultipleEvents() {
        val emitter = FooEventEmitter()
        emitter.on(FooEvents.onWillDoA) { 1 + 1 }
        emitter.on(FooEvents.onWillDoA) { 2 + 2 }
        emitter.on(FooEvents.onWillDoA) { 3 + 3 }
        assertContentEquals(listOf(2, 4, 6), emitter.emit(FooEvents.onWillDoA))

        emitter.on1(FooEvents.onWillDoADelayed) { withContext(Dispatchers.Default) { 10 + 10 } }
        emitter.on1(FooEvents.onWillDoADelayed) { withContext(Dispatchers.Default) { 20 + 20 } }
        emitter.on1(FooEvents.onWillDoADelayed) { withContext(Dispatchers.Default) { 30 + 30 } }
        assertContentEquals(listOf(20, 40, 60), runBlocking { emitter.emit1(FooEvents.onWillDoADelayed) } )
    }

    @Test
    fun testEmitException() {
        val emitter = FooEventEmitter()
        emitter.on(FooEvents.onWillDoA) { throw IllegalArgumentException("emit with exception") }
        assertTrue { emitter.emit(FooEvents.onWillDoA).first() is Exception }

        emitter.on1(FooEvents.onWillDoADelayed) { withContext(Dispatchers.Default) { throw IllegalArgumentException("emit with exception") } }
        assertTrue { runBlocking { emitter.emit1(FooEvents.onWillDoADelayed).first() is Exception } }
    }
}
