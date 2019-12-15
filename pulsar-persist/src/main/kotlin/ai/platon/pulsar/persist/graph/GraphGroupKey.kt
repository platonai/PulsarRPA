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
package ai.platon.pulsar.persist.graph

import org.apache.hadoop.io.*
import org.apache.hadoop.mapreduce.Partitioner
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.*

/**
 * A writable comparable container for an url with score. Provides a
 * [Partitioner] and [RawComparator] implementations for easy
 * integration with jobs.
 */
class GraphGroupKey(
        val reversedUrl: Text = Text(),
        val score: DoubleWritable = DoubleWritable()
) : WritableComparable<GraphGroupKey> {

    /**
     * Creates instance with provided non-writable types.
     *
     * @param reversedUrl reversedUrl
     * @param score       score
     */
    constructor(reversedUrl: String, score: Double): this(Text(reversedUrl), DoubleWritable(score))

    fun setReversedUrl(reversedUrl: String) {
        this.reversedUrl.set(reversedUrl)
    }

    fun setScore(score: Double) {
        this.score.set(score)
    }

    @Throws(IOException::class)
    override fun write(out: DataOutput) {
        reversedUrl.write(out)
        score.write(out)
    }

    @Throws(IOException::class)
    override fun readFields(input: DataInput) {
        reversedUrl.readFields(input)
        score.readFields(input)
    }

    override operator fun compareTo(other: GraphGroupKey): Int {
        return comp.compare(this, other)
    }

    override fun toString(): String {
        return "<$reversedUrl, $score>"
    }

    /**
     * A partitioner by {url}.
     */
    class UrlOnlyPartitioner : Partitioner<GraphGroupKey, Writable?>() {
        override fun getPartition(key: GraphGroupKey, `val`: Writable?, reduces: Int): Int {
            return (key.reversedUrl.hashCode() and Int.MAX_VALUE) % reduces
        }
    }

    /**
     * Compares by {url}.
     */
    class UrlOnlyComparator : RawComparator<GraphGroupKey> {
        private val textComp: WritableComparator = Text.Comparator()

        override fun compare(o1: GraphGroupKey, o2: GraphGroupKey): Int {
            return o1.reversedUrl.toString().compareTo(o2.reversedUrl.toString())
        }

        override fun compare(b1: ByteArray, s1: Int, l1: Int, b2: ByteArray, s2: Int, l2: Int): Int {
            return try {
                val deptLen1 = WritableUtils.decodeVIntSize(b1[s1]) + WritableComparator.readVInt(b1, s1)
                val deptLen2 = WritableUtils.decodeVIntSize(b2[s2]) + WritableComparator.readVInt(b2, s2)
                textComp.compare(b1, s1, deptLen1, b2, s2, deptLen2)
            } catch (e: IOException) {
                throw IllegalArgumentException(e)
            }
        }
    }

    /**
     * Compares by {url,score}. Scores are sorted in descending order, that is
     * from high scores to low.
     */
    class GraphKeyComparator : RawComparator<GraphGroupKey> {
        private val urlComp: WritableComparator = Text.Comparator()
        private val scoreComp: WritableComparator = FloatWritable.Comparator()

        override fun compare(key: GraphGroupKey, key2: GraphGroupKey): Int {
            val cmp = key.reversedUrl.toString().compareTo(key2.reversedUrl.toString())
            return if (cmp != 0) {
                cmp
            } else -key.score.compareTo(key2.score)
            // reverse order
        }

        override fun compare(b1: ByteArray, s1: Int, l1: Int, b2: ByteArray, s2: Int, l2: Int): Int {
            return try {
                val deptLen1 = WritableUtils.decodeVIntSize(b1[s1]) + WritableComparator.readVInt(b1, s1)
                val deptLen2 = WritableUtils.decodeVIntSize(b2[s2]) + WritableComparator.readVInt(b2, s2)
                val cmp = urlComp.compare(b1, s1, deptLen1, b2, s2, deptLen2)
                if (cmp != 0) {
                    cmp
                } else -scoreComp.compare(b1, s1 + deptLen1, l1 - deptLen1, b2, s2 + deptLen2, l2 - deptLen2)
                // reverse order
            } catch (e: IOException) {
                throw IllegalArgumentException(e)
            }
        }
    }

    companion object {
        private val comp: Comparator<GraphGroupKey> = GraphKeyComparator()
    }
}
