package ai.platon.pulsar.crawl.scoring.io

import ai.platon.pulsar.common.ScoreVector
import org.apache.hadoop.io.Text
import org.apache.hadoop.io.Writable
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

/**
 * Created by vincent on 17-4-20.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class ScoreVectorWritable(var scoreVector: ScoreVector) : Writable {
    fun get(): ScoreVector {
        return scoreVector
    }

    @Throws(IOException::class)
    override fun write(out: DataOutput) {
        out.writeInt(scoreVector.dimension)
        Text.writeString(out, scoreVector.toString())
    }

    @Throws(IOException::class)
    override fun readFields(input: DataInput) {
        val arity = input.readInt()
        scoreVector = ScoreVector.parse(Text.readString(input))
    }
}
