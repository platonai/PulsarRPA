/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common

import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.*

/**
 * Object cache, used to cache objects that are expensive to create.
 */
class ObjectCache private constructor() {
    private val objectMap = HashMap<String, Any?>()
    
    fun hasBean(key: String): Boolean {
        return objectMap[key] != null
    }
    
    fun getBean(key: String): Any? {
        return objectMap[key]
    }
    
    fun <T> getBean(key: String, defaultValue: T): T {
        val obj = objectMap[key] ?: return defaultValue
        return obj as T
    }
    
    fun <T> getBean(clazz: Class<T>): T? {
        return objectMap[clazz.name] as T?
    }
    
    inline fun <reified T: Any> getBean(): T? = getBean(T::class.java)

    inline fun <reified T: Any> computeIfAbsent(mappingFunction: () -> T): T {
        val oldValue = getBean<T>()
        val value: T = oldValue ?: mappingFunction()
        if (oldValue == null) {
            putBean(value)
        }
        return value
    }
    
    fun putBean(obj: Any) {
        objectMap[obj.javaClass.name] = obj
    }
    
    companion object {
        private val CACHE = WeakHashMap<ImmutableConfig, ObjectCache>()
        
        @JvmStatic
        fun get(conf: ImmutableConfig): ObjectCache {
            return CACHE.computeIfAbsent(conf) { ObjectCache() }
        }
    }
}
