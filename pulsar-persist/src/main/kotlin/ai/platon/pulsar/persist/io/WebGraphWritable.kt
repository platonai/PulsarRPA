package ai.platon.pulsar.persist.io

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.graph.WebGraph
import org.apache.gora.util.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.Text
import org.apache.hadoop.io.Writable
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

/**
 * Created by vincent on 16-12-30.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class WebGraphWritable(
        var graph: WebGraph = WebGraph.EMPTY,
        var conf: Configuration = ImmutableConfig.DEFAULT.unbox()
) : Writable {
    var optimizeMode = OptimizeMode.NONE
        private set

    constructor(graph: WebGraph, optimizeMode: OptimizeMode, conf: Configuration): this(graph, conf) {
        this.optimizeMode = optimizeMode
    }

    fun reset(graph: WebGraph): WebGraphWritable {
        this.graph = graph
        return this
    }

    fun get(): WebGraph {
        return graph
    }

    @Throws(IOException::class)
    override fun write(output: DataOutput) {
        output.writeChar(optimizeMode.value().toInt())
        output.writeInt(graph.edgeSet().size)
        for (edge in graph.edgeSet()) {
            output.writeBoolean(edge.isLoop)
            if (optimizeMode != OptimizeMode.IGNORE_EDGE) {
                Text.writeString(output, edge.options)
                Text.writeString(output, edge.anchorText)
                output.writeInt(edge.order)
                IOUtils.serialize(conf, output, MetadataWritable(edge.metadata), MetadataWritable::class.java)
                output.writeDouble(graph.getEdgeWeight(edge))
            }
            if (optimizeMode != OptimizeMode.IGNORE_SOURCE) {
                IOUtils.serialize(conf, output, WebVertexWritable(edge.source, conf), WebVertexWritable::class.java)
            }
            if (optimizeMode != OptimizeMode.IGNORE_TARGET) {
                IOUtils.serialize(conf, output, WebVertexWritable(edge.target, conf), WebVertexWritable::class.java)
            }
        }
    }

    @Throws(IOException::class)
    override fun readFields(input: DataInput) {
        graph = WebGraph()
        optimizeMode = OptimizeMode.of(input.readChar())
        val edgeSize = input.readInt()
        var options = ""
        var anchor = ""
        var order = -1
        var metadataWritable = MetadataWritable()
        var weight = 0.0
        for (i in 0 until edgeSize) {
            val isLoop = input.readBoolean()
            if (optimizeMode != OptimizeMode.IGNORE_EDGE) {
                options = Text.readString(input)
                anchor = Text.readString(input)
                order = input.readInt()
                metadataWritable = IOUtils.deserialize(conf, input, null, MetadataWritable::class.java)
                weight = input.readDouble()
            }
            var source = WebVertexWritable(conf)
            var target = WebVertexWritable(conf)
            if (optimizeMode != OptimizeMode.IGNORE_SOURCE) {
                source = IOUtils.deserialize(conf, input, null, WebVertexWritable::class.java)
                if (isLoop) target = source
            }
            if (optimizeMode != OptimizeMode.IGNORE_TARGET) {
                target = IOUtils.deserialize(conf, input, null, WebVertexWritable::class.java)
                if (isLoop) source = target
            }

            val edge = graph.addEdgeLenient(source.vertex, target.vertex, weight)
            edge.options = options
            edge.anchorText = anchor
            edge.order = order
            edge.metadata = metadataWritable.get()
        }
    }

    enum class OptimizeMode(private val mode: Char) {
        NONE('n'), IGNORE_SOURCE('s'), IGNORE_TARGET('t'), IGNORE_EDGE('e');

        fun value(): Char {
            return mode
        }

        companion object {
            fun of(b: Char): OptimizeMode {
                when (b) {
                    'n' -> return NONE
                    's' -> return IGNORE_SOURCE
                    't' -> return IGNORE_TARGET
                    'e' -> return IGNORE_EDGE
                }
                return NONE
            }
        }

    }
}