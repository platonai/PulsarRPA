/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.index

import org.apache.hadoop.io.Text
import org.apache.hadoop.io.Writable
import java.io.DataInput
import java.io.DataOutput
import kotlin.jvm.JvmOverloads
import kotlin.Throws
import java.lang.CloneNotSupportedException
import java.io.IOException
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

/**
 * This class represents a multi-valued field with a weight. Values are
 * arbitrary objects.
 */
class IndexField : Writable, Cloneable {
    private var weight = 0f
    private var values: MutableList<Any> = ArrayList()

    constructor() {}

    @JvmOverloads
    constructor(value: Any, weight: Float = 1.0f) {
        this.weight = weight
        if (value is Collection<*>) {
            values.addAll(value as Collection<Any>)
        } else {
            values.add(value)
        }
    }

    fun add(value: Any) {
        values.add(value)
    }

    fun getWeight(): Float {
        return weight
    }

    fun setWeight(weight: Float) {
        this.weight = weight
    }

    fun getValues(): List<Any> {
        return values
    }

    val stringValues: List<String>
        get() = values.stream().map { obj: Any -> obj.toString() }.collect(Collectors.toList())

    fun reset() {
        weight = 1.0f
        values.clear()
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val result = super.clone() as IndexField
        result.weight = weight
        result.values = values
        return result
    }

    @Throws(IOException::class)
    override fun readFields(`in`: DataInput) {
        weight = `in`.readFloat()
        val count = `in`.readInt()
        values = ArrayList()
        for (i in 0 until count) {
            val type = Text.readString(`in`)
            if (type == "java.lang.String") {
                values.add(Text.readString(`in`))
            } else if (type == "java.lang.Boolean") {
                values.add(`in`.readBoolean())
            } else if (type == "java.lang.Integer") {
                values.add(`in`.readInt())
            } else if (type == "java.lang.Float") {
                values.add(`in`.readFloat())
            } else if (type == "java.lang.Long") {
                values.add(`in`.readLong())
            } else if (type == "java.util.Date") {
                values.add(Date(`in`.readLong()))
            } else if (type == "java.time.Instant") {
                values.add(Instant.ofEpochMilli(`in`.readLong()))
            }
        }
    }

    @Throws(IOException::class)
    override fun write(out: DataOutput) {
        out.writeFloat(weight)
        out.writeInt(values.size)
        for (value in values) {
            Text.writeString(out, value.javaClass.name)
            if (value is Boolean) {
                out.writeBoolean(value)
            } else if (value is Int) {
                out.writeInt(value)
            } else if (value is Long) {
                out.writeLong(value)
            } else if (value is Float) {
                out.writeFloat(value)
            } else if (value is String) {
                Text.writeString(out, value)
            } else if (value is Date) {
                out.writeLong(value.time)
            } else if (value is Instant) {
                out.writeLong(value.toEpochMilli())
            }
        }
    }

    override fun toString(): String {
        return values.toString()
    }
}