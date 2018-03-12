package org.warps.pulsar.common.options;

import com.beust.jcommander.Parameter;

import java.util.Map;

import static org.warps.pulsar.common.PulsarParams.ARG_CRAWL_ID;

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class CommonOptions extends PulsarOptions {
    @Parameter(names = {ARG_CRAWL_ID}, description = "crawl id, (default : \"storage.crawl.id\")")
    public String crawlId = "";
    @Parameter(names = {"-config"}, description = "config dir")
    public String config = "";
    @Parameter(names = {"-h", "-help", "--help"}, help = true, description = "Print help text")
    private boolean help;

    public CommonOptions() {
    }

    public CommonOptions(String[] argv) {
        super(argv);
    }

    public CommonOptions(String args) {
        super(args);
    }

    public CommonOptions(Map<String, String> argv) {
        super(argv);
    }

    public String getCrawlId() {
        return crawlId;
    }

    public String getConfig() {
        return config;
    }

    @Override
    public boolean isHelp() {
        return help;
    }
}
