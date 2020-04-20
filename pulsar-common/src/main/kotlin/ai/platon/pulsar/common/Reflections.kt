package ai.platon.pulsar.common

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure

fun convertJVMMemberMethodSignatures(clazz: KClass<*>): List<String> {
    return clazz.declaredFunctions.map {
        "fun " + it.name + "(" + it.parameters.joinToString { it.name + ": " + it.type.jvmErasure.simpleName } + "): " + it.returnType.jvmErasure.simpleName
    }
}
