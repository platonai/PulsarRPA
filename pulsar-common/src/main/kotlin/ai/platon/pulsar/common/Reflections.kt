package ai.platon.pulsar.common

import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure

object ClassReflect {

    fun convertJVMMemberMethodSignatures(clazz: KClass<*>): List<String> {
        return clazz.declaredFunctions.map {
            "fun " + it.name +
                    "(" + it.parameters.joinToString { it.name + ": " + it.type.jvmErasure.simpleName } + "): " +
                    it.returnType.jvmErasure.simpleName
        }
    }

    fun isInvokable(clazz: Class<Any>): Boolean {
        return clazz.methods.any { MethodReflect.isInvokable(it) }
    }

    fun isNormalInvokable(clazz: Class<Any>): Boolean {
        return clazz.methods.any { MethodReflect.isNormalInvokable(it) }
    }

    fun isSuspendInvokable(clazz: Class<Any>): Boolean {
        return clazz.methods.any { MethodReflect.isSuspendInvokable(it) }
    }
}

object MethodReflect {

    fun isInvokable(method: Method): Boolean {
        return method.name == "invoke"
    }

    fun isNormalInvokable(method: Method): Boolean {
        if (method.name != "invoke" || method.isBridge) {
            return false
        }

        return  method.parameters.none { it.type.name == "kotlin.coroutines.Continuation" }
    }

    fun isSuspendInvokable(method: Method): Boolean {
        if (method.name != "invoke" || method.isBridge) {
            return false
        }

        return method.parameters.any { it.type.name == "kotlin.coroutines.Continuation" }
    }
}
