package ai.platon.pulsar.persist.graph

import ai.platon.pulsar.persist.WebPage
import org.apache.commons.collections4.CollectionUtils
import org.jgrapht.ext.*
import org.jgrapht.graph.DirectedWeightedPseudograph
import java.io.StringWriter
import java.io.Writer
import java.util.*
import java.util.function.Consumer

/**
 * Created by vincent on 16-12-21.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 *
 * A pseudograph is a non-simple graph in which both graph loops and multiple edges are permitted.
 */
class WebGraph : DirectedWeightedPseudograph<WebVertex, WebEdge> {
    var focus: WebVertex? = null
        set(focus) {
            if (!containsVertex(focus)) {
                addVertex(focus)
            }
            field = focus
        }

    constructor() : super(WebEdge::class.java) {}
    constructor(sourceVertex: WebVertex, targetVertex: WebVertex) : super(WebEdge::class.java) {
        addEdgeLenient(sourceVertex, targetVertex, 0.0)
    }

    fun addOrUpdateWebVertex(vertex: WebVertex): WebVertex {
        var v = vertex
        val added = addVertex(v)
        if (!added) {
            v = find(v)
            v.page = v.page
        }
        return v
    }

    @JvmOverloads
    fun addEdgeLenient(sourceVertex: WebVertex, targetVertex: WebVertex, weight: Double = 0.0): WebEdge {
        val v1 = addOrUpdateWebVertex(sourceVertex)
        val v2 = addOrUpdateWebVertex(targetVertex)
        val edge = addEdge(v1, v2)!!
        setEdgeWeight(edge, weight)
        return edge
    }

    fun addEdge(otherEdge: WebEdge, otherGraph: WebGraph): WebEdge {
        val sourceVertex = otherEdge.source
        val targetVertex = otherEdge.target
        val weight = otherGraph.getEdgeWeight(otherEdge)
        val metadata = otherEdge.metadata
        val edge = addEdgeLenient(sourceVertex, targetVertex, weight)
        edge.metadata = metadata
        return edge
    }

    fun combine(otherGraph: WebGraph): WebGraph {
        otherGraph.vertexSet().forEach(Consumer { vertex: WebVertex -> addOrUpdateWebVertex(vertex) })
        otherGraph.edgeSet().forEach(Consumer { edge: WebEdge -> addEdge(edge, otherGraph) })
        return this
    }

    fun find(vertex: WebVertex?): WebVertex {
        return CollectionUtils.find<WebVertex>(vertexSet()) { v: WebVertex -> v.equals(vertex) }
    }

    fun find(url: String): WebVertex {
        return CollectionUtils.find<WebVertex>(vertexSet()) { v: WebVertex -> v.url == url }
    }

    fun firstEdge(): WebEdge {
        return edgeSet().iterator().next()!!
    }

    override fun toString(): String {
        val exporter = createExporter()
        val writer: Writer = StringWriter()
        try {
            exporter.exportGraph(this, writer)
        } catch (e: ExportException) {
            return e.toString()
        }
        return writer.toString()
    }

    companion object {
        val EMPTY = WebGraph()

        fun of(edge: WebEdge, graph: WebGraph): WebGraph {
            val subgraph = WebGraph()
            subgraph.addEdge(edge, graph)
            return subgraph
        }

        /**
         * Create exporter
         */
        fun createExporter(): GraphExporter<WebVertex, WebEdge> {
            // create GraphML exporter
            val exporter = GraphMLExporter<WebVertex, WebEdge>(
                    VertexNameProvider { it.url?:""},
                    null,
                    EdgeNameProvider { it.toString() },
                    null
            )
            // set to export the internal edge weights
            exporter.isExportEdgeWeights = true
            // register additional name attribute for vertices and edges
            exporter.registerAttribute("url", GraphMLExporter.AttributeCategory.ALL, GraphMLExporter.AttributeType.STRING)
            // register additional color attribute for vertices
            exporter.registerAttribute("depth", GraphMLExporter.AttributeCategory.NODE, GraphMLExporter.AttributeType.INT)
            // create provider of vertex attributes
            val vertexAttributeProvider = ComponentAttributeProvider { v: WebVertex ->
                val m: MutableMap<String, String> = HashMap()
                m["baseUrl"] = v.page!!.location
                m["depth"] = v.page!!.distance.toString()
                m
            }
            exporter.setVertexAttributeProvider(vertexAttributeProvider)
            // create provider of edge attributes
            val edgeAttributeProvider = ComponentAttributeProvider { e: WebEdge ->
                val m: MutableMap<String, String> = HashMap()
                m["name"] = e.toString()
                m
            }
            exporter.setEdgeAttributeProvider(edgeAttributeProvider)
            return exporter
        }

        /**
         * Create importer
         */
        fun createImporter(): GraphImporter<WebVertex, WebEdge> { // create vertex provider
            val vertexProvider = VertexProvider { url: String, attributes: Map<String, String> ->
                val baseUrl = attributes["baseUrl"]
                val depth = Integer.valueOf(attributes["depth"])
                val page = WebPage.newWebPage(url)
                page.location = baseUrl
                page.distance = depth
                WebVertex(url, WebPage.newWebPage(url))
            }
            // create edge provider
            val edgeProvider = EdgeProvider<WebVertex, WebEdge> {
                from: WebVertex, to: WebVertex, label: String, attributes: Map<String, String> -> WebEdge()
            }
            // create GraphML importer
            return GraphMLImporter(vertexProvider, edgeProvider)
        }
    }
}