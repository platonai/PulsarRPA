package org.warps.pulsar.crawl.index;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.warps.pulsar.common.config.ReloadableParameterized;

import java.io.IOException;

/**
 * Created by vincent on 16-8-1.
 */
public interface IndexWriter extends ReloadableParameterized, AutoCloseable {

    Logger LOG = LoggerFactory.getLogger(IndexWriter.class);

    default String getName() {
        return getClass().getSimpleName();
    }

    default boolean isActive() {
        return true;
    }

    void open(Configuration conf) throws IOException;

    void open(String indexerUrl) throws IOException;

    void write(IndexDocument doc) throws IOException;

    void delete(String key) throws IOException;

    void update(IndexDocument doc) throws IOException;

    void commit() throws IOException;

    @Override
    void close() throws IOException;

    /**
     * Returns a String describing the IndexWriter instance and the specific
     * parameters it can take
     */
    String describe();
}
