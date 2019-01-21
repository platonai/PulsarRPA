package fun.platonic.pulsar.persist.graph;

import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.MultiMetadata;
import org.apache.commons.collections4.CollectionUtils;
import org.jgrapht.ext.*;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vincent on 16-12-21.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 * <p>
 * A pseudograph is a non-simple graph in which both graph loops and multiple edges are permitted.
 */
public class WebGraph extends DirectedWeightedPseudograph<WebVertex, WebEdge> {

    public final static WebGraph EMPTY_WEB_GRAPH = new WebGraph();

    private WebVertex focus;

    public WebGraph() {
        super(WebEdge.class);
    }

    public WebGraph(WebVertex sourceVertex, WebVertex targetVertex) {
        super(WebEdge.class);
        addEdgeLenient(sourceVertex, targetVertex, 0);
    }

    public static WebGraph of(WebEdge edge, WebGraph graph) {
        WebGraph subgraph = new WebGraph();
        subgraph.addEdge(edge, graph);
        return subgraph;
    }

    /**
     * Create exporter
     */
    public static GraphExporter<WebVertex, WebEdge> createExporter() {
        // create GraphML exporter
        GraphMLExporter<WebVertex, WebEdge> exporter = new GraphMLExporter<>(WebVertex::getUrl, null, DefaultEdge::toString, null);

        // set to export the internal edge weights
        exporter.setExportEdgeWeights(true);

        // register additional name attribute for vertices and edges
        exporter.registerAttribute("url", GraphMLExporter.AttributeCategory.ALL, GraphMLExporter.AttributeType.STRING);

        // register additional color attribute for vertices
        exporter.registerAttribute("depth", GraphMLExporter.AttributeCategory.NODE, GraphMLExporter.AttributeType.INT);

        // create provider of vertex attributes
        ComponentAttributeProvider<WebVertex> vertexAttributeProvider = v -> {
            Map<String, String> m = new HashMap<>();
            m.put("baseUrl", v.getWebPage().getBaseUrl());
            m.put("depth", String.valueOf(v.getWebPage().getDistance()));
            return m;
        };
        exporter.setVertexAttributeProvider(vertexAttributeProvider);

        // create provider of edge attributes
        ComponentAttributeProvider<WebEdge> edgeAttributeProvider =
                e -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("name", e.toString());
                    return m;
                };
        exporter.setEdgeAttributeProvider(edgeAttributeProvider);

        return exporter;
    }

    /**
     * Create importer
     */
    public static GraphImporter<WebVertex, WebEdge> createImporter() {
        // create vertex provider
        VertexProvider<WebVertex> vertexProvider = (url, attributes) -> {
            String baseUrl = attributes.get("baseUrl");
            int depth = Integer.valueOf(attributes.get("depth"));

            WebPage page = WebPage.newWebPage(url);
            page.setBaseUrl(baseUrl);
            page.setDistance(depth);

            return new WebVertex(WebPage.newWebPage(url));
        };

        // create edge provider
        EdgeProvider<WebVertex, WebEdge> edgeProvider = (from, to, label, attributes) -> new WebEdge();

        // create GraphML importer
        return new GraphMLImporter<>(vertexProvider, edgeProvider);
    }

    public WebVertex addOrUpdateWebVertex(WebVertex vertex) {
        boolean added = addVertex(vertex);
        if (!added) {
            vertex = find(vertex);
            vertex.setWebPage(vertex.getWebPage());
        }

        return vertex;
    }

    public WebEdge addEdgeLenient(WebVertex sourceVertex, WebVertex targetVertex, double weight) {
        WebVertex v1 = addOrUpdateWebVertex(sourceVertex);
        WebVertex v2 = addOrUpdateWebVertex(targetVertex);
        WebEdge edge = addEdge(v1, v2);
        setEdgeWeight(edge, weight);
        return edge;
    }

    public WebEdge addEdgeLenient(WebVertex sourceVertex, WebVertex targetVertex) {
        return addEdgeLenient(sourceVertex, targetVertex, 0.0);
    }

    public WebEdge addEdge(WebEdge otherEdge, WebGraph otherGraph) {
        WebVertex sourceVertex = otherEdge.getSource();
        WebVertex targetVertex = otherEdge.getTarget();
        double weight = otherGraph.getEdgeWeight(otherEdge);
        MultiMetadata metadata = otherEdge.getMetadata();

        WebEdge edge = addEdgeLenient(sourceVertex, targetVertex, weight);
        edge.setMetadata(metadata);

        return edge;
    }

    public WebGraph combine(WebGraph otherGraph) {
        otherGraph.vertexSet().forEach(this::addOrUpdateWebVertex);
        otherGraph.edgeSet().forEach(edge -> addEdge(edge, otherGraph));
        return this;
    }

    public WebVertex getFocus() {
        return focus;
    }

    public void setFocus(WebVertex focus) {
        if (!containsVertex(focus)) {
            addVertex(focus);
        }

        this.focus = focus;
    }

    public WebVertex find(WebVertex vertex) {
        return CollectionUtils.find(vertexSet(), v -> v.equals(vertex));
    }

    public WebVertex find(String url) {
        return CollectionUtils.find(vertexSet(), v -> v.getUrl().equals(url));
    }

    public WebEdge firstEdge() {
        return edgeSet().iterator().next();
    }

    @Override
    public String toString() {
        GraphExporter<WebVertex, WebEdge> exporter = WebGraph.createExporter();
        Writer writer = new StringWriter();
        try {
            exporter.exportGraph(this, writer);
        } catch (ExportException e) {
            return e.toString();
        }
        return writer.toString();
    }
}
