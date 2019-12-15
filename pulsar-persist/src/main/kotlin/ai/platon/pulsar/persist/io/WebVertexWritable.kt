package ai.platon.pulsar.persist.io

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.graph.WebVertex
import org.apache.gora.util.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.BooleanWritable
import org.apache.hadoop.io.Text
import org.apache.hadoop.io.Writable
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.*

/**
 * Created by vincent on 16-12-30.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class WebVertexWritable(
        val conf: Configuration
) : Writable {
    lateinit var vertex: WebVertex

    constructor(vertex: WebVertex, conf: Configuration): this(conf) {
        this.vertex = vertex
    }

    @Throws(IOException::class)
    override fun write(output: DataOutput) {
        Text.writeString(output, vertex.url)
        val hasWebPage = BooleanWritable(vertex.hasWebPage())
        hasWebPage.write(output)
        if (hasWebPage.get()) {
            IOUtils.serialize(conf, output, Objects.requireNonNull(vertex.page)!!.unbox(), GWebPage::class.java)
        }
    }

    @Throws(IOException::class)
    override fun readFields(input: DataInput) {
        val url = Text.readString(input)
        val hasWebPage = BooleanWritable()
        hasWebPage.readFields(input)
        val page: WebPage
        page = if (hasWebPage.get()) {
            WebPage.box(url, IOUtils.deserialize(conf, input, null, GWebPage::class.java))
        } else { // TODO: is it right to create a new web page?
            WebPage.newWebPage(url)
        }
        vertex = WebVertex(page)
    }
}