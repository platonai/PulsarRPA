package fun.platonic.pulsar.common.options.converters;

import com.beust.jcommander.Parameter;
import fun.platonic.pulsar.common.config.Params;
import fun.platonic.pulsar.common.options.CommonOptions;

import java.util.Map;

/**
 * Created by vincent on 17-7-18.
 */
public class ConvertOptions extends CommonOptions {
    @Parameter(names = {"-s", "--with-status"}, description = "Contains status.")
    boolean withStatus = false;
    @Parameter(names = {"-l", "--with-links"}, description = "Contains links.")
    boolean withLinks = false;
    @Parameter(names = {"-t", "--with-text"}, description = "Contains text.")
    boolean withText = false;
    @Parameter(names = {"-c", "--with-content"}, description = "Contains content.")
    boolean withContent = false;

    public ConvertOptions() {
    }

    public ConvertOptions(String[] args) {
        super(args);
    }

    public ConvertOptions(String args) {
        super(args);
    }

    public ConvertOptions(Map<String, String> args) {
        super(args);
    }

    public ConvertOptions(boolean withStatus, boolean withLinks, boolean withText, boolean withContent) {
        this.withStatus = withStatus;
        this.withLinks = withLinks;
        this.withText = withText;
        this.withContent = withContent;
    }

    @Override
    public Params getParams() {
        return Params.of(
                "-s", withStatus,
                "-l", withLinks,
                "-t", withText,
                "-c", withContent
        );
    }

    @Override
    public String toString() {
        return getParams().withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replaceAll("\\s+", " ");
    }
}
