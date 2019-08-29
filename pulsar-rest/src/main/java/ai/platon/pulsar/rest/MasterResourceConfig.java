package ai.platon.pulsar.rest;

import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.rest.filters.CORSResponseFilter;
import ai.platon.pulsar.rest.resources.SeedResource;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import javax.json.stream.JsonGenerator;
import java.net.URI;

import static ai.platon.pulsar.common.config.CapabilityTypes.PULSAR_MASTER_PORT;
import static ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_PULSAR_MASTER_PORT;

/**
 * Created by vincent on 17-4-25.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
@Singleton
public class MasterResourceConfig extends ResourceConfig {
    private URI baseUri;
    private int port;

    public static final String ROOT_PATH = "/api";

    public MasterResourceConfig() {
        this(new ImmutableConfig(), SeedResource.class.getPackage().getName());
    }

    public MasterResourceConfig(ImmutableConfig conf) {
        this(conf, SeedResource.class.getPackage().getName());
    }

    public MasterResourceConfig(ImmutableConfig conf, String... packages) {
        port = conf.getInt(PULSAR_MASTER_PORT, DEFAULT_PULSAR_MASTER_PORT);
        baseUri = URI.create(String.format("http://%s:%d%s", "0.0.0.0", port, ROOT_PATH));

        packages(packages);
        property(JsonGenerator.PRETTY_PRINTING, true);
        register(CORSResponseFilter.class);
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public int getPort() {
        return port;
    }
}
