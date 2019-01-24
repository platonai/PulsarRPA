package fun.platonic.pulsar.crawl.index;

import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates {@link IndexWriter} implementing plugins.
 */
public class IndexWriters implements AutoCloseable {

    Logger LOG = IndexWriter.LOG;

    private ImmutableConfig conf;
    private ArrayList<IndexWriter> indexWriters = new ArrayList<>();

    public IndexWriters() {
    }

    public IndexWriters(ImmutableConfig conf) {
        this(Collections.emptyList(), conf);
    }

    public IndexWriters(List<IndexWriter> indexWriters, ImmutableConfig conf) {
        this.conf = conf;
        this.indexWriters.addAll(indexWriters);
    }

    public void open() {
        for (IndexWriter indexWriter : indexWriters) {
            try {
                indexWriter.open(conf);
            } catch (Throwable e) {
                LOG.error("Failed to open indexer. " + StringUtil.stringifyException(e));
            }
        }
    }

    public void open(String indexerUrl) {
        for (IndexWriter indexWriter : indexWriters) {
            try {
                indexWriter.open(indexerUrl);
            } catch (Throwable e) {
                LOG.error("Failed to open indexer. " + StringUtil.stringifyException(e));
            }
        }
    }

    public void write(IndexDocument doc) {
        for (IndexWriter indexWriter : indexWriters) {
            try {
                indexWriter.write(doc);
            } catch (Throwable e) {
                LOG.error("Failed to write indexer. " + StringUtil.stringifyException(e));
            }
        }
    }

    public void update(IndexDocument doc) {
        for (IndexWriter indexWriter : indexWriters) {
            try {
                indexWriter.update(doc);
            } catch (Throwable e) {
                LOG.error("Failed to update indexer. " + StringUtil.stringifyException(e));
            }
        }
    }

    public void delete(String key) {
        for (IndexWriter indexWriter : indexWriters) {
            try {
                indexWriter.delete(key);
            } catch (Throwable e) {
                LOG.error("Failed to delete indexer. " + StringUtil.stringifyException(e));
            }
        }
    }

    @Override
    public void close() {
        for (IndexWriter indexWriter : indexWriters) {
            try {
                // log.info("[Destruction] Closing IndexWriter " + indexWriter.getName() + ", ...");
                indexWriter.close();
            } catch (Throwable e) {
                LOG.error("Failed to close IndexWriter " + indexWriter.getName());
                LOG.error(StringUtil.stringifyException(e));
            }
        }

        indexWriters.clear();
    }

    public void commit() {
        for (IndexWriter indexWriter : indexWriters) {
            try {
                indexWriter.commit();
            } catch (Throwable e) {
                LOG.error("Failed to commit indexer. " + StringUtil.stringifyException(e));
            }
        }
    }

    @Override
    public String toString() {
        return indexWriters.stream().map(n -> n.getClass().getName()).collect(Collectors.joining(", "));
    }
}
