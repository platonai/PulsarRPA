package ai.platon.pulsar.crawl.parse.html

import java.util.*

/**
 * Created by vincent on 17-8-3.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class OpenMapFields {
    var name: String = ""
    val map = HashMap<String, String?>()

    var required = 0
    var loss = 0

    fun put(key: String, value: String?): String? = map.put(key, value)

    operator fun set(key: String, value: String?): String? = map.put(key, value)

    operator fun get(key: String): String? = map[key]

    fun isEmpty() = map.isEmpty()

    fun increaseRequired(count: Int) {
        required += count
    }

    fun loss(loss: Int) {
        this.loss += loss
    }

    fun assertContains(key: String, value: String): OpenMapFields {
        assert(value == map.get(key).toString())
        // If (!equals) throw Exception
        return this
    }

    fun assertContainsKey(vararg keys: String): OpenMapFields {
        for (key in keys) {
            assert(map.containsKey(key))
            // If (!equals) throw Exception
        }
        return this
    }

    fun assertContainsValue(vararg values: String): OpenMapFields {
        for (value in values) {
            assert(map.containsValue(value))
            // If (!equals) throw Exception
        }
        return this
    }

    fun assertContains(message: String, key: String, value: String): OpenMapFields {
        return this
    }
}
