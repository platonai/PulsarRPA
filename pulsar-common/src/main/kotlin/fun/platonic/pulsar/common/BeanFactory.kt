package `fun`.platonic.pulsar.common

import `fun`.platonic.pulsar.common.ObjectCache
import `fun`.platonic.pulsar.common.config.CapabilityTypes
import `fun`.platonic.pulsar.common.config.ImmutableConfig
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

open class BeanFactory(val conf: ImmutableConfig) {

    val objectCache = ObjectCache.get(conf)
    val domain = conf.get(CapabilityTypes.SCENT_DOMAIN, "default")

    fun cacheId(clazz: KClass<*>, vararg modifiers: String): String {
        return modifiers.joinToString(", ", clazz.jvmName + " - ")
    }

    fun cacheId(clazz: KClass<*>): String {
        return clazz.jvmName
    }

    fun putBean(cacheId: String, obj: Any) {
        objectCache.put(cacheId, obj)
    }

    fun putBean(obj: Any) {
        objectCache.put(obj::class.java.name, obj)
    }

    fun <T> getJvmBean(cacheId: String, clazz: Class<T>): T? {
        val obj = objectCache.getBean(cacheId)
        return if (obj == null || obj.javaClass != clazz) null else obj as T
    }

    inline fun <reified T> getBean(cacheId: String): T? {
        val obj = objectCache.getBean(cacheId)
        return if (obj == null || obj !is T) null else objectCache.getBean(cacheId) as T
    }

    inline fun <reified T> getBean(): T? {
        val obj = objectCache.getBean(T::class.java)
        return if (obj == null || obj !is T) null else obj
    }

    inline fun <reified T> computeIfAbsent(cacheId: String, generator: () -> T): T {
        var obj = getBean<T>(cacheId)
        if (obj == null) {
            obj = generator()
            putBean(cacheId, obj!!)
        }

        return obj
    }

    inline fun <reified T> computeIfAbsent(generator: () -> T): T {
        return computeIfAbsent(cacheId(T::class), generator)
    }
}
