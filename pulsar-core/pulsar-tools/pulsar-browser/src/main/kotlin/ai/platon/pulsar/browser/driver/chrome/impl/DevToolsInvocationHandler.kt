package ai.platon.pulsar.browser.driver.chrome.impl

import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.pulsar.browser.driver.chrome.MethodInvocation
import ai.platon.pulsar.browser.driver.chrome.RemoteDevTools
import ai.platon.pulsar.browser.driver.chrome.impl.EventDispatcher.Companion.ID_PROPERTY
import ai.platon.pulsar.browser.driver.chrome.util.ChromeIOException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeRPCException
import ai.platon.pulsar.browser.driver.chrome.util.ReflectUtils
import ai.platon.pulsar.browser.driver.chrome.util.SuspendAwareHandler
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicLong

class DevToolsInvocationHandler(impl: Any) : SuspendAwareHandler(impl) {
    companion object {
        private const val EVENT_LISTENER_PREFIX = "on"
        private val ID_SUPPLIER = AtomicLong(1L)

        fun nextId() = ID_SUPPLIER.incrementAndGet()

        /**
         *
         * */
        fun createMethodInvocation(method: String, params: Map<String, Any?>?): MethodInvocation {
            val params0 = (params ?: emptyMap()).toMutableMap()
            val methodId = params0[ID_PROPERTY]?.toString()?.toLongOrNull() ?: nextId()
            params0[ID_PROPERTY] = methodId.toString()

            val params1 = params0.filterValues { it != null } as Map<String, Any>
            return MethodInvocation(methodId, method, params1)
        }

        fun createMethodInvocation(method: Method, args: Array<out Any>? = null): MethodInvocation {
            val domainName = method.declaringClass.simpleName
            val methodName = method.name
            return MethodInvocation(nextId(), "$domainName.$methodName", buildMethodParams(method, args))
        }

        private fun buildMethodParams(method: Method, args: Array<out Any>? = null): Map<String, Any> {
            val params: MutableMap<String, Any> = HashMap()
            val parameters = method.parameters
            if (args != null) {
                for (i in args.indices) {
                    val parameter = parameters[i]
                    val javaAnnotation = parameter.getAnnotation(ParamName::class.java)
                    if (javaAnnotation != null) {
                        params[javaAnnotation.value] = args[i]
                    }
                    // the last parameter might be `kotlin.coroutines.Continuation`
                    // params[parameters[i].getAnnotation(ParamName::class.java).value] = args[i]
                }
            }
            return params
        }
    }

    lateinit var devTools: RemoteDevTools

    @Throws(ChromeIOException::class, ChromeRPCException::class)
    override fun invoke(target: Any, method: Method, args: Array<out Any>?): Any? {
        // Handle built-in Object methods locally and do NOT call devTools
        if (isJavaLangObjectMethod(method)) {
            return handleJavaLangObjectMethod(target, method, args)
        }

        if (isEventSubscription(method)) {
            return handleEventSubscription(target, method, args)
        }

        // Resolve accurate return type, especially for suspend functions whose JVM return type is Object
        val (resolvedReturnType, resolvedTypeParams) = ReflectUtils.resolveReturnType(method)
        val returnProperty = method.getAnnotation(Returns::class.java)?.value

        val returnTypeClasses = method.getAnnotation(ReturnTypeParameter::class.java)
            ?.value?.map { it.java }?.toTypedArray() ?: resolvedTypeParams

        val methodInvocation = createMethodInvocation(method, args)

        // TODO: avoid runBlocking, wrapper the function to use super.invoke()
        return runBlocking {
            // Invokes a remote method and returns the result.
            devTools.invoke(resolvedReturnType, returnProperty, returnTypeClasses, methodInvocation)
        }
    }

    private fun handleEventSubscription(target: Any, method: Method, args: Array<out Any>?): EventListener {
        require(args != null) { "args must not be null" }
        require(args.isNotEmpty()) { "Args must not be empty" }

        val domainName = method.declaringClass.simpleName
        val eventName: String = getEventName(method)
        val eventHandlerType = ReflectUtils.getJavaClass(method)

        @Suppress("UNCHECKED_CAST")
        val handler: EventHandler<Any> = when (val arg = args[0]) {
            is EventHandler<*> -> arg as EventHandler<Any>
            is Function2<*, *, *> -> {
                // suspend (T) -> Unit
                val f = arg as (suspend (Any) -> Unit)
                EventHandler { event -> f(event) }
            }
            is Function1<*, *> -> {
                // (T) -> Unit
                val f = arg as (Any) -> Unit
                EventHandler { event -> f(event) }
            }
            else -> throw IllegalArgumentException(
                "Argument must be EventHandler<T>, (T) -> Unit, or suspend (T) -> Unit"
            )
        }

        return devTools.addEventListener(domainName, eventName, handler, eventHandlerType)
    }

    private fun getEventName(method: Method): String {
        return method.getAnnotation(EventName::class.java).value
    }

    private fun getEventHandlerType1(method: Method): Type {
        // Typical actualTypeArguments:
        // 0: `? super ai.platon.cdt.kt.protocol.events.network.RequestWillBeSent`
        // 1: `? super kotlin.coroutines.Continuation<? super kotlin.Unit>`
        // 2: `?`
        val actualTypeArguments = (method.genericParameterTypes[0] as ParameterizedType).actualTypeArguments
        val actualTypeArgument = actualTypeArguments[0]
        return actualTypeArgument
    }

    /**
     * Checks if given method has signature of event subscription.
     *
     * @param method Method to check.
     * @return True if this is event subscription method that is: EventListener on*(EventHandler) or on*((payload) -> Unit)
     */
    private fun isEventSubscription(method: Method): Boolean {
        val name = method.name
        val parameters = method.parameters

        if (!name.startsWith(EVENT_LISTENER_PREFIX)) {
            return false
        }

        if (EventListener::class.java != method.returnType) {
            return false
        }

        if (parameters?.size != 1) {
            return false
        }

        // Typical `parameters`:
        // 0: `kotlin.jvm.functions.Function2<? super ai.platon.cdt.kt.protocol.events.network.RequestWillBeSent, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ?> arg0`
        //
        val parameter = parameters[0]

        val hasEventHandlerParam = EventHandler::class.java.isAssignableFrom(parameter.type)

        val hasFunctionParam = parameter.parameterizedType.typeName.startsWith("kotlin.jvm.functions.Function")

        return hasEventHandlerParam || hasFunctionParam
    }

    /**
     * Determine if the method belongs to java.lang.Object (Kotlin Any).
     */
    private fun isJavaLangObjectMethod(method: Method): Boolean {
        val dc = method.declaringClass
        return dc == Any::class.java || dc == java.lang.Object::class.java
    }

    /**
     * Handle built-in methods locally to avoid remote invocation.
     */
    private fun handleJavaLangObjectMethod(target: Any, method: Method, args: Array<out Any>?): Any? {
        return when (method.name) {
            "toString" -> target.javaClass.name + "@" + Integer.toHexString(System.identityHashCode(target))
            "hashCode" -> System.identityHashCode(target)
            "equals" -> {
                val other = if (args != null && args.isNotEmpty()) args[0] else null
                target === other
            }
            else -> null
        }
    }

}
