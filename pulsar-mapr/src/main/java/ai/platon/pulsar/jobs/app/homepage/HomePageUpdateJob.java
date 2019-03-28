package ai.platon.pulsar.jobs.app.homepage;

import ai.platon.pulsar.common.config.Params;
import ai.platon.pulsar.common.options.CommonOptions;
import ai.platon.pulsar.jobs.core.AppContextAwareJob;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.gora.generated.GWebPage;
import ai.platon.pulsar.persist.metadata.Mark;
import org.apache.gora.filter.FilterOp;
import org.apache.gora.filter.MapFieldValueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static ai.platon.pulsar.common.config.CapabilityTypes.*;
import static ai.platon.pulsar.common.config.PulsarConstants.YES_STRING;

/**
 * Created by vincent on 17-6-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public abstract class HomePageUpdateJob extends AppContextAwareJob {

  public static final Logger LOG = LoggerFactory.getLogger(HomePageUpdateJob.class);

  protected static final Set<GWebPage.Field> FIELDS = new HashSet<>();

  static {
    Collections.addAll(FIELDS, GWebPage.Field.values());
    FIELDS.remove(GWebPage.Field.CONTENT);
    FIELDS.remove(GWebPage.Field.HEADERS);
    FIELDS.remove(GWebPage.Field.PAGE_TEXT);
    FIELDS.remove(GWebPage.Field.CONTENT_TEXT);
    FIELDS.remove(GWebPage.Field.LINKS);
    FIELDS.remove(GWebPage.Field.INLINKS);
    FIELDS.remove(GWebPage.Field.PAGE_MODEL);
  }

  @Override
  protected void setup(Params params) throws Exception {
    super.setup(params);
    setIndexHomeUrl();
  }

  public abstract void setIndexHomeUrl();

  public MapFieldValueFilter<String, GWebPage> getQueryFilter() {
    MapFieldValueFilter<String, GWebPage> filter = new MapFieldValueFilter<>();

    filter.setFieldName(GWebPage.Field.MARKERS.toString());
    filter.setFilterOp(FilterOp.NOT_EQUALS);
    filter.setFilterIfMissing(false);
    filter.setMapKey(WebPage.wrapKey(Mark.INACTIVE));
    filter.getOperands().add(WebPage.u8(YES_STRING));

    return filter;
  }

  @Override
  public int run(String[] args) throws Exception {
    CommonOptions options = new CommonOptions(args);
    options.parseOrExit();
    conf.setIfNotEmpty(STORAGE_CRAWL_ID, options.getCrawlId());
    run();
    return 0;
  }
}
