package ai.platon.pulsar.common.options;

import ai.platon.pulsar.common.config.Params;
import com.beust.jcommander.Parameter;

/**
 * Created by vincent on 17-7-14.
 */
public class ParseOptions extends CommonOptions {
    @Parameter(names = {"-ps", "--parse"}, description = "Parse the page.")
    private boolean parse = false;
    @Parameter(names = {"-rpl", "--reparse-links"}, description = "Re-parse all links if the parsed.")
    private boolean reparseLinks = false;
    @Parameter(names = {"-nlf", "--no-link-filter"}, description = "No filters applied to parse links.")
    private boolean noLinkFilter = false;
    @Parameter(names = {"-prst", "--persist"}, description = "Persist the page.")
    private boolean persist = false;

    public ParseOptions() {
    }

    public ParseOptions(String args) {
        super(args);
    }

    public static ParseOptions parse(String args) {
        ParseOptions options = new ParseOptions(args);
        options.parse();
        return options;
    }

    public boolean isParse() {
        return parse;
    }

    public void setParse(boolean parse) {
        this.parse = parse;
    }

    public boolean isReparseLinks() {
        return reparseLinks;
    }

    public void setReparseLinks(boolean reparseLinks) {
        this.reparseLinks = reparseLinks;
    }

    public boolean isNoLinkFilter() {
        return noLinkFilter;
    }

    public void setNoLinkFilter(boolean noLinkFilter) {
        this.noLinkFilter = noLinkFilter;
    }

    public boolean isPersist() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    @Override
    public Params getParams() {
        return Params.of(
                "-ps", parse,
                "-rpl", reparseLinks,
                "-nlf", noLinkFilter,
                "-prst", persist
        );
    }

    @Override
    public String toString() {
        return getParams().withCmdLineStyle(true).withKVDelimiter(" ")
                .formatAsLine().replaceAll("\\s+", " ");
    }
}
