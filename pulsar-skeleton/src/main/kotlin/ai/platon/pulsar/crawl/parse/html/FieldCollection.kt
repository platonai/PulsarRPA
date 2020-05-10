package ai.platon.pulsar.crawl.parse.html

import java.util.*

/**
 * Created by vincent on 17-8-3.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * TODO: bad to inherit from HashMap
 */
class FieldCollection : HashMap<String, String>() {
    var name: String? = null
        set(value) {
            field = value
            if (value != null) {
                put("name", value)
            }
        }

    var required = 0
    var loss = 0

    fun increaseRequired(count: Int) {
        required += count
    }

    fun loss(loss: Int) {
        this.loss += loss
    }

    fun assertContains(key: String, value: String): FieldCollection {
        assert(value == get(key).toString())
        // If (!equals) throw Exception
        return this
    }

    fun assertContainsKey(vararg keys: String): FieldCollection {
        for (key in keys) {
            assert(containsKey(key))
            // If (!equals) throw Exception
        }
        return this
    }

    fun assertContainsValue(vararg values: String): FieldCollection {
        for (value in values) {
            assert(containsValue(value))
            // If (!equals) throw Exception
        }
        return this
    }

    fun assertContains(message: String, key: String, value: String): FieldCollection {
        return this
    }
}