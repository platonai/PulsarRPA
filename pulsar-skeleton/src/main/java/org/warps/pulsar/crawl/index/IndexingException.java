package org.warps.pulsar.crawl.index;

/**
 * Created by vincent on 16-8-1.
 */
public class IndexingException extends Exception {

    public IndexingException() {
        super();
    }

    public IndexingException(String message) {
        super(message);
    }

    public IndexingException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexingException(Throwable cause) {
        super(cause);
    }
}
