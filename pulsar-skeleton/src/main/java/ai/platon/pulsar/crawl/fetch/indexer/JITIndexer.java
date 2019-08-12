package ai.platon.pulsar.crawl.fetch.indexer;

import ai.platon.pulsar.common.StringUtil;
import ai.platon.pulsar.common.Urls;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.config.ReloadableParameterized;
import ai.platon.pulsar.crawl.fetch.FetchMonitor;
import ai.platon.pulsar.crawl.fetch.FetchTask;
import ai.platon.pulsar.crawl.index.IndexDocument;
import ai.platon.pulsar.crawl.index.IndexWriters;
import ai.platon.pulsar.crawl.index.IndexingFilters;
import ai.platon.pulsar.crawl.scoring.ScoringFilters;
import ai.platon.pulsar.persist.ParseStatus;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.ParseStatusCodes;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import static ai.platon.pulsar.common.PulsarParams.DOC_FIELD_TEXT_CONTENT;
import static ai.platon.pulsar.common.config.CapabilityTypes.INDEX_JIT;

/**
 * Created by vincent on 16-8-23.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class JITIndexer implements ReloadableParameterized, AutoCloseable {

  public static final Logger LOG = FetchMonitor.LOG;

  private static AtomicInteger instanceSequence = new AtomicInteger(0);

  private final int id;
  private ImmutableConfig conf;
  private boolean indexJIT;
  private final IndexWriters indexWriters;
  private final IndexingFilters indexingFilters;
  private final ScoringFilters scoringFilters;
  private IndexDocument.Builder indexDocumentBuilder;

  private int batchSize = 2000;
  private int indexThreadCount;
  private int minTextLenght;
  // All object inside a process shares the same counters
  private AtomicInteger indexedPages = new AtomicInteger(0);
  private AtomicInteger ignoredPages = new AtomicInteger(0);

  private final Set<IndexThread> activeIndexThreads = new ConcurrentSkipListSet<>();
  private final BlockingQueue<FetchTask> indexTasks = Queues.newLinkedBlockingQueue(batchSize);

  private final LinkedList<IndexThread> indexThreads = Lists.newLinkedList();

  public JITIndexer(ScoringFilters scoringFilters, IndexingFilters indexingFilters, IndexWriters indexWriters, ImmutableConfig conf) {
    this.id = instanceSequence.incrementAndGet();
    this.scoringFilters = scoringFilters;
    this.indexingFilters = indexingFilters;
    this.indexWriters = indexWriters;

    reload(conf);
  }

  @Override
  public ImmutableConfig getConf() {
    return conf;
  }

  @Override
  public Params getParams() {
    return Params.of(
        "batchSize", batchSize,
        "indexThreadCount", indexThreadCount,
        "minTextLenght", minTextLenght
    );
  }

  @Override
  public void reload(ImmutableConfig conf) {
    this.conf = conf;

    this.indexJIT = conf.getBoolean(INDEX_JIT, false);
    this.batchSize = conf.getInt("index.index.batch.size", this.batchSize);
    this.indexThreadCount = conf.getInt("index.index.thread.count", 1);
    this.minTextLenght = conf.getInt("index.minimal.text.length", 300);

    if (indexJIT) {
      this.indexDocumentBuilder = new IndexDocument.Builder(conf).with(indexingFilters).with(scoringFilters);
      this.indexWriters.open();
    }
  }

  public int id() { return id; }

  void registerFetchThread(IndexThread indexThread) {
    activeIndexThreads.add(indexThread);
  }

  void unregisterFetchThread(IndexThread indexThread) {
    activeIndexThreads.remove(indexThread);
  }

  public int getIndexThreadCount() {
    return indexThreadCount;
  }

  public int getIndexedPages() {
    return indexedPages.get();
  }

  public int getIngoredPages() {
    return ignoredPages.get();
  }

  /**
   * Add fetch item to index indexTasks
   * Thread safe
   */
  public void produce(FetchTask fetchTask) {
    if (!indexJIT) {
      return;
    }

    WebPage page = fetchTask.getPage();
    if (page == null) {
      LOG.warn("Invalid FetchTask to index, ignore it");
      return;
    }

    if (!shouldProduce(page)) {
      return;
    }

    indexTasks.add(fetchTask);
  }

  /**
   * Thread safe
   */
  public FetchTask consume() {
    return indexTasks.poll();
  }

  @Override
  public void close() {
    LOG.info("[Destruction] Closing JITIndexer #" + id + " ...");

    indexThreads.forEach(IndexThread::exitAndJoin);

    try {
      FetchTask fetchTask = consume();
      while (fetchTask != null) {
        index(fetchTask);
        fetchTask = consume();
      }
    } catch (Throwable e) {
      LOG.error(e.toString());
    }

    LOG.info("There are " + ignoredPages + " not indexed short pages out of total " + indexedPages + " pages");
  }

  /**
   * Thread safe
   */
  public void index(FetchTask fetchTask) {
    if (!indexJIT) {
      return;
    }

    try {
      if (fetchTask == null) {
        LOG.error("Failed to index, null fetchTask");
        return;
      }

      String url = fetchTask.getUrl();
      String reverseUrl = Urls.reverseUrl(url);
      WebPage page = fetchTask.getPage();

      IndexDocument doc = indexDocumentBuilder.build(reverseUrl, page);
      if (shouldIndex(doc)) {
        synchronized (indexWriters) {
          indexWriters.write(doc);
          page.putIndexTimeHistory(Instant.now());
        }
        indexedPages.incrementAndGet();
      } // if
    } catch (Throwable e) {
      LOG.error("Failed to index a page " + StringUtil.stringifyException(e));
    }
  }

  private boolean shouldIndex(IndexDocument doc) {
    if (doc == null) {
      return false;
    }

    String textContent = doc.getFieldValueAsString(DOC_FIELD_TEXT_CONTENT);
    if (textContent == null || textContent.length() < minTextLenght) {
      ignoredPages.incrementAndGet();
      LOG.warn("Invalid text content to index, url : " + doc.getUrl());
      return false;
    }

    return true;
  }

  private boolean shouldProduce(WebPage page) {
    if (page.isSeed()) {
      return false;
    }

    ParseStatus status = page.getParseStatus();

    if (status == null || !status.isSuccess() || status.getMajorCode() == ParseStatusCodes.SUCCESS_REDIRECT) {
      return false;
    }

    if (page.getContentText().length() < minTextLenght) {
      ignoredPages.incrementAndGet();
      return false;
    }

    return true;
  }
}
