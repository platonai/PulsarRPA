package ai.platon.pulsar.jobs.app.homepage;

import ai.platon.pulsar.common.WeakPageIndexer;
import ai.platon.pulsar.jobs.common.SelectorEntry;
import ai.platon.pulsar.jobs.core.AppContextAwareGoraReducer;
import ai.platon.pulsar.persist.WebDb;
import ai.platon.pulsar.persist.gora.generated.GWebPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ai.platon.pulsar.common.CommonCounter.rRows;
import static ai.platon.pulsar.common.config.CapabilityTypes.STAT_INDEX_HOME_URL;

/**
 * Created by vincent on 17-6-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public class HomePageUpdateReducer extends AppContextAwareGoraReducer<SelectorEntry, GWebPage, String, GWebPage> {

  private WebDb webDb;
  private int count = 0;
  private int pageSize = 10000;
  private int pageNo = 0;
  private List<CharSequence> indexUrls = new ArrayList<>(pageSize);
  private String indexHomeUrl;
  private WeakPageIndexer weakIndexer;

  @Override
  protected void setup(Context context) throws IOException {
    this.webDb = applicationContext.getBean(WebDb.class);
    this.indexHomeUrl = conf.get(STAT_INDEX_HOME_URL, "http://nebula.platonic.fun/tmp_index/");
    this.weakIndexer = new WeakPageIndexer(indexHomeUrl, webDb);
  }

  @Override
  protected void reduce(SelectorEntry key, Iterable<GWebPage> rows, Context context) {
    metricsCounters.increase(rRows);

    ++count;

    indexUrls.add(key.getUrl());
    if (indexUrls.size() >= pageSize) {
      commit();
    }
  }

  @Override
  protected void cleanup(Context context) {
    commit();

    String message = "Total " + count + " index pages, indexed in " + pageNo + " pages";
    LOG.info(message);
  }

  private void commit() {
    weakIndexer.indexAll(++pageNo, indexUrls);
    weakIndexer.commit();
    indexUrls.clear();
  }
}
