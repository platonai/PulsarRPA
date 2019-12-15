package ai.platon.pulsar.persist.io

import ai.platon.pulsar.persist.metadata.MultiMetadata
import org.apache.hadoop.io.Text
import org.apache.hadoop.io.Writable
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

/**
 * Created by vincent on 17-3-15.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class MetadataWritable(var metadata: MultiMetadata = MultiMetadata()) : Writable {

    fun get(): MultiMetadata {
        return metadata
    }

    @Throws(IOException::class)
    override fun write(out: DataOutput) {
        val names: Collection<String> = metadata.names()
        out.writeInt(names.size)
        for (name in names) {
            val values = metadata.getNonNullValues(name)
            Text.writeString(out, name)
            out.writeInt(values.size)
            for (value in values) {
                Text.writeString(out, value)
            }
        }
    }

    @Throws(IOException::class)
    override fun readFields(input: DataInput) {
        val nameCount = input.readInt()
        for (i in 0 until nameCount) {
            val name = Text.readString(input)
            val valueCount = input.readInt()
            for (j in 0 until valueCount) {
                metadata.put(name, Text.readString(input))
            }
        }
    }
}
