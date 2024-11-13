/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common;

import ai.platon.pulsar.common.config.ImmutableConfig;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * TODO: use BeanFactory(Spring or Apache Configuration)
 *
 * @author vincent
 * @version $Id: $Id
 */
public class ObjectCache {

    private static final WeakHashMap<ImmutableConfig, ObjectCache> CACHE = new WeakHashMap<>();

    private final HashMap<String, Object> objectMap = new HashMap<>();

    private ObjectCache() {
    }

    /**
     * <p>get.</p>
     *
     * @return a {@link ai.platon.pulsar.common.ObjectCache} object.
     */
    public static ObjectCache get(ImmutableConfig conf) {
        ObjectCache objectCache = CACHE.get(conf);
        if (objectCache == null) {
            objectCache = new ObjectCache();
            CACHE.put(conf, objectCache);
        }

        return objectCache;
    }

    /**
     * <p>hasBean.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasBean(String key) {
        return objectMap.get(key) != null;
    }

    /**
     * <p>getBean.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public Object getBean(String key) {
        return objectMap.get(key);
    }

    /**
     * <p>getBean.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param defaultValue a T object.
     * @param <T> a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String key, T defaultValue) {
        Object obj = objectMap.get(key);
        if (obj == null) {
            return defaultValue;
        }
        return (T) obj;
    }

    /**
     * <p>getBean.</p>
     *
     * @param clazz a {@link java.lang.Class} object.
     * @param <T> a T object.
     * @return a T object.
     */
    public <T> T getBean(Class<T> clazz) {
        return (T) objectMap.get(clazz.getName());
    }

    /**
     * <p>computeIfAbsent.</p>
     *
     * @param clazz a {@link java.lang.Class} object.
     * @param mappingFunction a {@link java.util.function.Function} object.
     * @param <T> a T object.
     * @return a T object.
     */
    public <T> T computeIfAbsent(Class<T> clazz, Function<Class<T>, T> mappingFunction) {
        T value = getBean(clazz);
        if (value == null) {
            value = mappingFunction.apply(clazz);
            put(value);
        }
        return value;
    }

    /**
     * <p>put.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     */
    public void put(String key, Object value) {
        objectMap.put(key, value);
    }

    /**
     * <p>put.</p>
     *
     * @param obj a {@link java.lang.Object} object.
     */
    public void put(Object obj) {
        objectMap.put(obj.getClass().getName(), obj);
    }
}
