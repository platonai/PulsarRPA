module ai.platon.pulsar.common {
    requires commons.math3;

    requires com.google.common;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.kotlin;

    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core;

    requires java.desktop;
    requires java.sql;

    requires org.apache.commons.lang3;
    requires org.apache.httpcomponents.httpclient;
    requires org.slf4j;
    requires org.apache.commons.io;
    requires org.apache.commons.collections4;
    requires woodstox.core.asl;
    requires org.codehaus.stax2;
    requires com.google.gson;
    requires annotations;
    requires jsr305;
    requires spring.core;
    requires org.apache.commons.codec;

    exports ai.platon.pulsar.common;
    exports ai.platon.pulsar.common.config;
    exports ai.platon.pulsar.common.ai.api;
    exports ai.platon.pulsar.common.browser;
    exports ai.platon.pulsar.common.chrono;
    exports ai.platon.pulsar.common.collect;
    exports ai.platon.pulsar.common.collect.collector;
    exports ai.platon.pulsar.common.collect.queue;
    exports ai.platon.pulsar.common.concurrent;
    exports ai.platon.pulsar.common.emoji;
    exports ai.platon.pulsar.common.event;
    exports ai.platon.pulsar.common.extractor;
    exports ai.platon.pulsar.common.http;
    exports ai.platon.pulsar.common.io;
    exports ai.platon.pulsar.common.lang;
    exports ai.platon.pulsar.common.math;
    exports ai.platon.pulsar.common.math.geometric;
    exports ai.platon.pulsar.common.math.vectors;
    exports ai.platon.pulsar.common.measure;
    exports ai.platon.pulsar.common.options;
    exports ai.platon.pulsar.common.proxy;
    exports ai.platon.pulsar.common.serialize.json;
    exports ai.platon.pulsar.common.sql;
    exports ai.platon.pulsar.common.urls;
    exports ai.platon.pulsar.common.urls.preprocess;
}
