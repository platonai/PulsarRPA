package ai.platon.pulsar.crawl.index.io

import org.apache.hadoop.io.Writable
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.index.IndexField
import kotlin.Throws
import java.io.IOException
import org.apache.hadoop.io.Text
import org.apache.hadoop.io.VersionMismatchException
import org.apache.hadoop.io.WritableUtils
import java.io.DataInput
import java.io.DataOutput

/**
 * Created by vincent on 17-3-17.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class IndexDocumentWritable(
    var doc: IndexDocument = IndexDocument()
) : Writable {

    fun get(): IndexDocument {
        return doc
    }

    @Throws(IOException::class)
    override fun readFields(input: DataInput) {
        val version = input.readByte()
        if (version != VERSION) {
            throw VersionMismatchException(VERSION, version)
        }
        val size = WritableUtils.readVInt(input)
        for (i in 0 until size) {
            val name = Text.readString(input)
            val field = IndexField()
            field.readFields(input)
            doc.add(name, field)
            // fields.put(name, field);
        }
        doc.weight = input.readFloat()
    }

    @Throws(IOException::class)
    override fun write(out: DataOutput) {
        out.writeByte(VERSION.toInt())
        val fields = doc.fields
        WritableUtils.writeVInt(out, fields.size)
        for ((key, field) in fields) {
            Text.writeString(out, key)
            field.write(out)
        }
        out.writeFloat(doc.weight)
    }

    companion object {
        const val VERSION: Byte = 2
    }
}
