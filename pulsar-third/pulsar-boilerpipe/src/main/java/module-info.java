module ai.platon.pulsar.boilerpipe {
    requires com.google.common;
    requires org.apache.commons.lang3;
    requires java.xml;
    requires xercesImpl;
    requires nekohtml;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires ai.platon.pulsar.common;

    exports ai.platon.pulsar.boilerpipe.sax;
    exports ai.platon.pulsar.boilerpipe.extractors;
    exports ai.platon.pulsar.boilerpipe.document;
    exports ai.platon.pulsar.boilerpipe.filters;
    exports ai.platon.pulsar.boilerpipe.filters.heuristics;
    exports ai.platon.pulsar.boilerpipe.filters.simple;
    exports ai.platon.pulsar.boilerpipe.filters.statistics;
}
