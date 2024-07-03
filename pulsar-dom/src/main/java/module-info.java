module ai.platon.pulsar.dom {
    requires ai.platon.pulsar.common;
    requires pulsar.jsoup;

    requires commons.math3;

    requires kotlin.stdlib;
    requires java.compiler;
    requires org.apache.commons.lang3;
    requires org.slf4j;
    requires com.google.common;
    requires transitive java.desktop;

    exports ai.platon.pulsar.dom;
    exports ai.platon.pulsar.dom.features;
    exports ai.platon.pulsar.dom.features.defined;
    exports ai.platon.pulsar.dom.model;
    exports ai.platon.pulsar.dom.select;
    exports ai.platon.pulsar.dom.nodes;
    exports ai.platon.pulsar.dom.nodes.node.ext;
}
