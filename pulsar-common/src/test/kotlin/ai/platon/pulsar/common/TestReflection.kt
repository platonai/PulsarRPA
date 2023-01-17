package ai.platon.pulsar.common

import ai.platon.pulsar.common.event.AbstractEventEmitter
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertTrue

private enum class BarEvents {
    onWillDoA,
    onWillDoADelayed,
}

private class BarEventEmitter : AbstractEventEmitter<BarEvents>() {
    fun rawListeners(): Map<BarEvents, List<Function<Any>>> {
        return listenerMap
    }
}

class TestReflection {

    @Test
    fun testAnonymousMethodsInformation() {
        val emitter = BarEventEmitter()

        emitter.on(BarEvents.onWillDoA) { 1 + 1 }
        emitter.on1(BarEvents.onWillDoADelayed) { delay(1) }

        emitter.listeners().forEach { obj ->
            println("=========== Java Class: ")
            println(obj.javaClass)
            println("Superclass: " + obj.javaClass.superclass)

            println("----------- methods: ")

            obj.javaClass.methods.forEach { method ->
                val name = method.name
                val fullName = method.toString()

                println(name)
                println(fullName)
                println("Invokable?: " + MethodReflect.isInvokable(method))
                println("NormalInvokable?: " + MethodReflect.isNormalInvokable(method))
                println("SuspendInvokable?: " + MethodReflect.isSuspendInvokable(method))
                method.parameters.forEach {
                    println(it.name + "\t" + it.type + "\t" + it.type.name)
                    println("Type: " + it.type.name + "\t" + it.type.canonicalName + "\t")
                }
            }
        }
    }

    @Test
    fun testInvokable() {
        val emitter = BarEventEmitter()

        emitter.on(BarEvents.onWillDoA) { 1 + 1 }
        emitter.on1(BarEvents.onWillDoADelayed) { delay(1) }

        val listeners = emitter.rawListeners()
        val method1 = listeners.values.first().first()
        assertTrue { ClassReflect.isInvokable(method1.javaClass) }

        val method2 = listeners.values.last().first()
        assertTrue { ClassReflect.isSuspendInvokable(method2.javaClass) }
    }
}
