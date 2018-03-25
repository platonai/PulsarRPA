package fun.platonic.pulsar.common.options;

import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.PulsarParams;

import java.util.Map;

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class CommonOptions extends PulsarOptions {
    @Parameter(names = {PulsarParams.ARG_CRAWL_ID}, description = "crawl id, (default : \"storage.crawl.id\")")
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
