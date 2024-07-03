module ai.platon.pulsar.persist.kt {
    requires ai.platon.pulsar.common;
    requires gora.shaded.mongodb;
    requires org.slf4j;
    requires hadoop.common;
    requires org.apache.commons.lang3;
    requires jsr305;
    requires avro;
    requires org.apache.commons.collections4;
    requires annotations;
    requires java.xml;

    exports ai.platon.pulsar.persist;
    exports ai.platon.pulsar.persist.metadata;
    exports ai.platon.pulsar.persist.model;
    exports ai.platon.pulsar.persist.gora;
    exports ai.platon.pulsar.persist.gora.generated;
}
