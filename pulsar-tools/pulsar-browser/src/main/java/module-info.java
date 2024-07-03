module ai.platon.pulsar.browser {
    requires kotlin.stdlib;
    requires ai.platon.pulsar.common;
    requires com.codahale.metrics;
    requires org.slf4j;
    requires org.apache.commons.lang3;
    requires javax.websocket.api;
    requires kotlinx.coroutines.core;
    requires commons.math3;
    requires java.desktop;
    requires org.apache.commons.io;
    requires tyrus.client;
    requires tyrus.container.grizzly.client;
    requires com.google.gson;
    requires org.javassist;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.kotlin;

    exports ai.platon.pulsar.browser.common;
    exports ai.platon.pulsar.browser.driver.chrome;
    exports ai.platon.pulsar.browser.driver.chrome.impl;
}
