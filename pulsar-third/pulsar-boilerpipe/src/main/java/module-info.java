module ai.platon.pulsar.boilerpipe {
    requires com.google.common;
    requires org.apache.commons.lang3;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires ai.platon.pulsar.common;
    requires nekohtml;

    // xercesImpl conflicts with jdk.xml.dom, both module has package w3c.dom.html.
    // xercesImpl.jar does not adhere to the java 11 module system
    // see https://issues.apache.org/jira/browse/XERCESJ-1706
    requires xercesImpl;
    requires java.xml;
//    requires jdk.xml.dom;

    exports ai.platon.pulsar.boilerpipe.sax;
    exports ai.platon.pulsar.boilerpipe.extractors;
    exports ai.platon.pulsar.boilerpipe.document;
    exports ai.platon.pulsar.boilerpipe.filters;
    exports ai.platon.pulsar.boilerpipe.filters.heuristics;
    exports ai.platon.pulsar.boilerpipe.filters.simple;
    exports ai.platon.pulsar.boilerpipe.filters.statistics;
}
