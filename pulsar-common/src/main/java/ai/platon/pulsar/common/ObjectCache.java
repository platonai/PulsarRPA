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
import org.apache.hadoop.conf.Configuration;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * TODO: use BeanFactory(Spring or Apache Configuration)
 * */
public class ObjectCache {

    private static final WeakHashMap<Configuration, ObjectCache> CACHE = new WeakHashMap<>();

    private final HashMap<String, Object> objectMap = new HashMap<>();

    private ObjectCache() {
    }

    public static ObjectCache get(ImmutableConfig conf) {
        return get(conf.unbox());
    }

    public static ObjectCache get(Configuration conf) {
        ObjectCache objectCache = CACHE.get(conf);
        if (objectCache == null) {
            objectCache = new ObjectCache();
            CACHE.put(conf, objectCache);
        }

        return objectCache;
    }

    public boolean hasBean(String key) {
        return objectMap.get(key) != null;
    }

    public Object getBean(String key) {
        return objectMap.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String key, T defaultValue) {
        Object obj = objectMap.get(key);
        if (obj == null) {
            return defaultValue;
        }
        return (T) obj;
    }

    public <T> T getBean(Class<T> clazz) {
        return (T) objectMap.get(clazz.getName());
    }

    public <T> T computeIfAbsent(Class<T> clazz, Function<Class<T>, T> mappingFunction) {
        T value = getBean(clazz);
        if (value == null) {
            value = mappingFunction.apply(clazz);
            put(value);
        }
        return value;
    }

    public void put(String key, Object value) {
        objectMap.put(key, value);
    }

    public void put(Object obj) {
        objectMap.put(obj.getClass().getName(), obj);
    }
}
