package ai.platon.pulsar.common

import ai.platon.pulsar.common.logPrintln
import ai.platon.pulsar.common.event.AbstractEventEmitter
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertTrue

private enum class BarEvents {
    onWillDoA,
    onWillDoADelayed,
}

private class BarEventEmitter : AbstractEventEmitter<BarEvents>() {
}

class TestReflection {

    @Test
    fun testAnonymousMethodsInformation() {
        val emitter = BarEventEmitter()

        emitter.on(BarEvents.onWillDoA) { 1 + 1 }
        emitter.on1(BarEvents.onWillDoADelayed) { delay(1) }

        emitter.listeners().forEach { obj ->
//            logPrintln("=========== Java Class: ")
//            logPrintln(obj.javaClass)
//            logPrintln("Superclass: " + obj.javaClass.superclass)
//
//            logPrintln("----------- methods: ")

            obj.javaClass.methods.forEach { method ->
                val name = method.name
                val fullName = method.toString()

//                logPrintln(name)
//                logPrintln(fullName)
//                logPrintln("Invokable?: " + MethodReflect.isInvokable(method))
//                logPrintln("NormalInvokable?: " + MethodReflect.isNormalInvokable(method))
//                logPrintln("SuspendInvokable?: " + MethodReflect.isSuspendInvokable(method))
//                method.parameters.forEach {
//                    logPrintln(it.name + "\t" + it.type + "\t" + it.type.name)
//                    logPrintln("Type: " + it.type.name + "\t" + it.type.canonicalName + "\t")
//                }
            }
        }
    }

    @Test
    fun testInvokable() {
        val emitter = BarEventEmitter()

        emitter.on(BarEvents.onWillDoA) { 1 + 1 }
        emitter.on1(BarEvents.onWillDoADelayed) { delay(1) }

        val method1 = emitter.normalListeners().first()
        assertTrue { ClassReflect.isNormalInvokable(method1.javaClass) }

        val method2 = emitter.suspendListeners().last()
        assertTrue { method1 != method2 }
        assertTrue { ClassReflect.isSuspendInvokable(method2.javaClass) }
    }
}

