package ai.platon.pulsar.jobs.common;

import ai.platon.pulsar.persist.WebPage;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

public class FetchEntry extends Configured {
    private String reservedUrl;
    private WebPage page;

    public FetchEntry() {}

    public FetchEntry(Configuration conf, String reservedUrl, WebPage page) {
        super(conf);
        this.reservedUrl = reservedUrl;
        this.page = page;
    }

    public String getReservedUrl() {
        return reservedUrl;
    }

    public WebPage getWebPage() {
        return page;
    }

    @Override
    public String toString() {
        return "<" + reservedUrl + ", " + page + ">";
    }
}
