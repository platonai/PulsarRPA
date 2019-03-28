package ai.platon.pulsar.rest;

import ai.platon.pulsar.rest.resources.WelcomeResource;
import ai.platon.pulsar.rest.filters.CORSResponseFilter;
import ai.platon.pulsar.common.config.ImmutableConfig;
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
public class MasterApplication extends ResourceConfig {
  private URI baseUri;

  public static final String ROOT_PATH = "/api";

  public MasterApplication() {
    this(new ImmutableConfig(), WelcomeResource.class.getPackage().getName());
  }

  public MasterApplication(ImmutableConfig conf) {
    this(conf, WelcomeResource.class.getPackage().getName());
  }

  public MasterApplication(ImmutableConfig conf, String... packages) {
    int port = conf.getInt(PULSAR_MASTER_PORT, DEFAULT_PULSAR_MASTER_PORT);
    baseUri = URI.create(String.format("http://%s:%d%s", "0.0.0.0", port, ROOT_PATH));

    packages(packages);
    property(JsonGenerator.PRETTY_PRINTING, true);
    register(CORSResponseFilter.class);
  }

  public URI getBaseUri() {
    return baseUri;
  }
}
