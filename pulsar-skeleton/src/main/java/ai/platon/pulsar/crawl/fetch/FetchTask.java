package ai.platon.pulsar.crawl.fetch;

import ai.platon.pulsar.common.URLUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.persist.WebPage;

import javax.annotation.Nonnull;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class described the item to be fetched.
 */
public class FetchTask implements Comparable<FetchTask> {

    /**
     * The initial value is the current timestamp in second, to make it
     * an unique id in the current host
     */
    private static AtomicInteger instanceSequence = new AtomicInteger(0);
    private Key key;
    private WebPage page;
    private Instant pendingStart = Instant.EPOCH;

    public FetchTask(int jobID, int priority, String protocol, String host, WebPage page, URL u) {
        this.key = new Key(jobID, priority, protocol, host, u.toString());
        this.page = page;
    }

    /**
     * Create an item. Queue id will be created based on <code>groupMode</code>
     * argument, either as a protocol + hostname pair, protocol + IP
     * address pair or protocol+domain pair.
     */
    public static FetchTask create(int jobId, int priority, String url, WebPage page, URLUtil.GroupMode groupMode) {
        final URL u = Urls.getURLOrNull(url);

        if (u == null) {
            return null;
        }

        final String proto = u.getProtocol();
        final String host = URLUtil.getHost(u, groupMode);

        if (proto == null || host.isEmpty()) {
            return null;
        }

        return new FetchTask(jobId, priority, proto.toLowerCase(), host.toLowerCase(), page, u);
    }

    public WebPage getPage() {
        return page;
    }

    public void setPage(WebPage page) {
        this.page = page;
    }

    public Key getKey() {
        return key;
    }

    public int getPriority() {
        return key.priority;
    }

    public String getProtocol() {
        return key.protocol;
    }

    public String getHost() {
        return key.host;
    }

    public int getItemId() {
        return key.itemId;
    }

    public String getUrl() {
        return key.url;
    }

    public Instant getPendingStart() {
        return pendingStart;
    }

    public void setPendingStart(Instant pendingStart) {
        this.pendingStart = pendingStart;
    }

    /**
     * Generate an unique numeric id for the fetch item
     * <p>
     * fetch item id is used to easy item searching
     */
    private int nextId() {
        return instanceSequence.incrementAndGet();
    }

    @Override
    public String toString() {
        return "[itemId=" + key.itemId + ", priority=" + key.priority + ", url=" + key.url + "]";
    }

    @Override
    public int compareTo(@Nonnull FetchTask fetchTask) {
        return getKey().compareTo(fetchTask.getKey());
    }

    public class Key implements Comparable<Key> {
        public int jobID;
        public int priority;
        public String protocol;
        public String host;
        public int itemId;
        public String url;

        public Key(int jobID, int priority, String protocol, String host, String url) {
            this.jobID = jobID;
            this.priority = priority;
            this.protocol = protocol;
            this.host = host;
            this.itemId = nextId();
            this.url = url;
        }

        @Override
        public int compareTo(Key key) {
            return itemId - key.itemId;
        }
    }
}
