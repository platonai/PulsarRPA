module ai.platon.pulsar.skeleton {
    requires ai.platon.pulsar.common;
    requires ai.platon.pulsar.persist;
    requires ai.platon.pulsar.browser;
    requires ai.platon.pulsar.dom;
    requires ai.platon.pulsar.ql.common;
    requires pulsar.h2;

    requires com.github.oshi;
    requires kotlin.stdlib;
    requires org.slf4j;
    requires org.apache.commons.lang3;
    requires jcommander;
    requires com.google.gson;
    requires java.xml;
    requires kotlinx.coroutines.core;
    requires com.codahale.metrics;
    requires spring.beans;
    requires kotlin.reflect;
    requires java.sql;
    requires pulsar.jsoup;
    requires java.desktop;
    requires langchain4j.core;
    requires langchain4j.zhipu.ai;
    requires com.google.common;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.kotlin;
    requires spring.context;
    requires crawler.commons;

    requires com.codahale.metrics.graphite;
    requires icu4j;
    requires org.apache.tika.core;
    requires annotations;
    requires avro;
    requires hadoop.common;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.commons.collections4;

    exports ai.platon.pulsar.skeleton.common;
    exports ai.platon.pulsar.skeleton.common.collect;
    exports ai.platon.pulsar.skeleton.common.files.ext;
    exports ai.platon.pulsar.skeleton.common.message;
    exports ai.platon.pulsar.skeleton.common.metrics;
    exports ai.platon.pulsar.skeleton.common.options;
    exports ai.platon.pulsar.skeleton.common.persist.ext;
    exports ai.platon.pulsar.skeleton.common.urls;
    exports ai.platon.pulsar.skeleton.context;
    exports ai.platon.pulsar.skeleton.context.support;

    exports ai.platon.pulsar.skeleton.crawl;
    exports ai.platon.pulsar.skeleton.crawl.ai;
    exports ai.platon.pulsar.skeleton.crawl.common;
    exports ai.platon.pulsar.skeleton.crawl.common.url;
    exports ai.platon.pulsar.skeleton.crawl.component;
    exports ai.platon.pulsar.skeleton.crawl.event;
    exports ai.platon.pulsar.skeleton.crawl.fetch;
    exports ai.platon.pulsar.skeleton.crawl.fetch.driver.rpa;
    exports ai.platon.pulsar.skeleton.crawl.fetch.driver.tools;
    exports ai.platon.pulsar.skeleton.crawl.fetch.privacy;
    exports ai.platon.pulsar.skeleton.crawl.filter;
    exports ai.platon.pulsar.skeleton.crawl.impl;
    exports ai.platon.pulsar.skeleton.crawl.index;
    exports ai.platon.pulsar.skeleton.crawl.inject;
    exports ai.platon.pulsar.skeleton.crawl.parse;
    exports ai.platon.pulsar.skeleton.crawl.protocol;
    exports ai.platon.pulsar.skeleton.crawl.protocol.http;
    exports ai.platon.pulsar.skeleton.crawl.schedule;
    exports ai.platon.pulsar.skeleton.crawl.scoring;

    exports ai.platon.pulsar.skeleton.session;
}
