package ai.platon.pulsar.browser.driver.chrome

import javassist.Modifier
import javassist.util.proxy.ProxyFactory
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

object ProxyClasses {
    private val LOG = LoggerFactory.getLogger(ProxyClasses::class.java)
    /**
     * Creates a proxy class to a given interface clazz supplied with invocation handler.
     *
     * @param clazz Proxy to class.
     * @param invocationHandler Invocation handler.
     * @param <T> Class type.
     * @return Proxy instance.
    </T> */
    fun <T> createProxy(clazz: Class<T>, invocationHandler: InvocationHandler?): T {
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf<Class<*>>(clazz), invocationHandler) as T
    }

    /**
     * Creates a proxy class to a given abstract clazz supplied with invocation handler for
     * un-implemented/abstrat methods.
     *
     * @param clazz Proxy to class.
     * @param paramTypes Ctor param types.
     * @param args Ctor args.
     * @param invocationHandler Invocation handler.
     * @param <T> Class type.
     * @return Proxy instance.
    </T> */
    fun <T> createProxyFromAbstract(
            clazz: Class<T>,
            paramTypes: Array<Class<*>>,
            args: Array<Any>? = null,
            invocationHandler: InvocationHandler
    ): T {
        val proxyFactory = ProxyFactory()
        proxyFactory.superclass = clazz
        proxyFactory.setFilter { Modifier.isAbstract(it.modifiers) }
        return try {
            proxyFactory.create(paramTypes, args) { o, method, _, objects ->
                invocationHandler.invoke(o, method, objects)
            } as T
        } catch (e: Exception) {
            LOG.error("Failed creating proxy from abstract class", e)
            throw RuntimeException("Failed creating proxy from abstract class", e)
        }
    }
}
