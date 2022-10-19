package ai.platon.pulsar.persist.graph

import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.gora.GoraWebPage
import org.jgrapht.ext.*
import org.jgrapht.graph.DirectedWeightedPseudograph
import java.io.StringWriter
import java.io.Writer
import java.util.*

/**
 * Created by vincent on 16-12-21.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
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

    constructor() : super(WebEdge::class.java)

    constructor(sourceVertex: WebVertex, targetVertex: WebVertex) : super(WebEdge::class.java) {
        addEdgeLenient(sourceVertex, targetVertex, 0.0)
    }

    fun addOrUpdateWebVertex(vertex: WebVertex): WebVertex {
        val added = addVertex(vertex)

        if (!added) {
            val v = find(vertex)
            if (v != null) {
                v.page = vertex.page
                return v
            } else {
                throw IllegalStateException("Vertex must be in the graph | ${vertex.url}")
            }
        }

        return vertex
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
        otherGraph.vertexSet().forEach { addOrUpdateWebVertex(it) }
        otherGraph.edgeSet().forEach { addEdge(it, otherGraph) }
        return this
    }

    fun find(vertex: WebVertex): WebVertex? {
        return vertexSet().firstOrNull { it == vertex }
    }

    fun find(url: String): WebVertex? {
        return vertexSet().firstOrNull { it.url == url }
    }

    fun firstEdge(): WebEdge? {
        val it = edgeSet().iterator()
        return it.takeIf { it.hasNext() }?.next()
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
                { it.url },
                    null,
                { it.toString() },
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
            exporter.vertexAttributeProvider = vertexAttributeProvider
            // create provider of edge attributes
            val edgeAttributeProvider = ComponentAttributeProvider { e: WebEdge ->
                val m: MutableMap<String, String> = HashMap()
                m["name"] = e.toString()
                m
            }
            exporter.edgeAttributeProvider = edgeAttributeProvider
            return exporter
        }

        /**
         * Create importer
         */
        fun createImporter(): GraphImporter<WebVertex, WebEdge> { // create vertex provider
            val vertexProvider = VertexProvider { url: String, attributes: Map<String, String> ->
                val baseUrl = attributes["baseUrl"]
                val depth = Integer.valueOf(attributes["depth"])
                val page = GoraWebPage.newWebPage(url, VolatileConfig.UNSAFE)
                page.location = baseUrl
                page.distance = depth
                WebVertex(url, GoraWebPage.newWebPage(url, VolatileConfig.UNSAFE))
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
