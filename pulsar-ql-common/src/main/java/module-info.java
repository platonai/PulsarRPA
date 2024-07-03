module ai.platon.pulsar.ql.common {
    requires ai.platon.pulsar.common;
    requires ai.platon.pulsar.dom;
    requires pulsar.h2;

    requires jsr305;
    requires pulsar.jsoup;
    requires hadoop.common;
    requires java.sql;

    exports ai.platon.pulsar.ql.common.annotation;
    exports ai.platon.pulsar.ql.common.io;
    exports ai.platon.pulsar.ql.common.types;
    exports ai.platon.pulsar.ql.common;
}
