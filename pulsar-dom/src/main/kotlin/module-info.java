module ai.platon.pulsar.dom {
    requires java.desktop;
    requires commons.math3;

    requires ai.platon.pulsar.common;
    requires pulsar.jsoup;
    requires kotlin.stdlib;

    exports ai.platon.pulsar.dom;
    exports ai.platon.pulsar.dom.features;
    exports ai.platon.pulsar.dom.model;
    exports ai.platon.pulsar.dom.select;
    exports ai.platon.pulsar.dom.nodes;
    exports ai.platon.pulsar.dom.nodes.node.ext;
}
