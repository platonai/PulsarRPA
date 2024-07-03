module ai.platon.pulsar.ql.common {
    requires jsr305;
    requires pulsar.jsoup;
    requires ai.platon.pulsar.common;
    requires hadoop.common;
    requires ai.platon.pulsar.dom;

    exports ai.platon.pulsar.ql;
    exports ai.platon.pulsar.ql.annotation;
    exports ai.platon.pulsar.ql.io;
    exports ai.platon.pulsar.ql.types;
}
