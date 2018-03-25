/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fun.platonic.pulsar.crawl.protocol;

import fun.platonic.pulsar.common.ObjectCache;
import fun.platonic.pulsar.common.ResourceLoader;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.metadata.FetchMode;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates and caches {@link Protocol} plugins. Protocol plugins should define
 * the attribute "protocolName" with the name of the protocol that they
 * implement. Configuration object is used for caching. Cache key is constructed
 * from appending protocol name (eg. http) to constant
 */
@Component
public class ProtocolFactory implements AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(ProtocolFactory.class);

    private ImmutableConfig immutableConfig;
    private Map<String, Protocol> protocols = Collections.synchronizedMap(new HashMap<>());

    public ProtocolFactory(ImmutableConfig immutableConfig) {
        this.immutableConfig = immutableConfig;

        Map<String, Protocol> results = new ResourceLoader()
                .readAllLines("", "protocol-plugins.txt")
                .stream()
                .map(String::trim)
                .filter(line -> !line.startsWith("#"))
                .map(line -> line.split("\\s+"))
                .filter(a -> a.length >= 2)
                .map(a -> Pair.of(a[0], getInstance(a)))
                .filter(p -> p.getValue() != null)
                .peek(p -> p.getValue().setConf(immutableConfig))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        protocols.putAll(results);

        LOG.info(protocols.keySet().stream()
                .collect(Collectors.joining(", ", "Supported protocols: ", "")));
    }

    @Deprecated
    public static ProtocolFactory create(ImmutableConfig conf) {
        ObjectCache cache = ObjectCache.get(conf);

        ProtocolFactory protocolFactory = cache.getBean(ProtocolFactory.class);
        if (protocolFactory == null) {
            protocolFactory = new ProtocolFactory(conf);
            cache.put(protocolFactory);
        }

        return protocolFactory;
    }

    /**
     * TODO: configurable, using major protocol/sub protocol is a good idea
     * Using major protocol/sub protocol is a good idea, for example:
     * selenium:http://www.baidu.com/
     * jdbc:h2:tcp://localhost/~/test
     */
    @Nullable
    public Protocol getProtocol(WebPage page) {
        FetchMode mode = page.getFetchMode();
        Protocol protocol;
        if (mode == FetchMode.SELENIUM) {
            protocol = getProtocol("selenium:" + page.getUrl());
        } else if (mode == FetchMode.CROWDSOURCING) {
            protocol = getProtocol("crowd:" + page.getUrl());
        } else {
            protocol = getProtocol(page.getUrl());
        }

        if (protocol != null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Protocol: " + protocol.getClass().getName());
            }
        }

        return protocol;
    }

    /**
     * Returns the appropriate {@link Protocol} implementation for a url.
     *
     * @param url The url
     * @return The appropriate {@link Protocol} implementation for a given
     * {@link URL}.
     */
    @Nullable
    public Protocol getProtocol(String url) {
        String protocolName = StringUtils.substringBefore(url, ":");
        // sub protocol can be supported by main:sub://example.com later
        return protocols.get(protocolName);
    }

    @Nullable
    public Protocol getProtocol(FetchMode mode) {
        return getProtocol(mode.name().toLowerCase() + "://");
    }

    @Nullable
    private Protocol getInstance(String[] config) {
        try {
            // config[0] is the protocol name, config[1] is the class name, and the rest are properties
            String className = config[1];
            return (Protocol) Class.forName(className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error(StringUtil.stringifyException(e));
        }

        return null;
    }

    @Override
    public void close() throws Exception {
        Iterator<Protocol> it = protocols.values().iterator();
        while (it.hasNext()) {
            Protocol protocol = it.next();

            try {
                protocol.close();
            } catch (Throwable e) {
                LOG.error(e.toString());
            }

            it.remove();
        }
    }
}
