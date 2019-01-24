package fun.platonic.pulsar.common;

import com.google.common.collect.Lists;
import fun.platonic.pulsar.persist.HypeLink;
import fun.platonic.pulsar.persist.WebDb;
import fun.platonic.pulsar.persist.WebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by vincent on 17-6-18.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * <p>
 * A WebPage based indexer to pages, the index does not support ACID
 */
public class WeakPageIndexer {
    private final static int MAX_LINKS = 20000;
    private Logger LOG = LoggerFactory.getLogger(WeakPageIndexer.class);
    private WebDb webDb;
    private String homeUrl;

    public WeakPageIndexer(CharSequence homeUrl, WebDb webDb) {
        this.webDb = webDb;
        this.homeUrl = homeUrl.toString();
    }

    public WebPage home() {
        return getHome();
    }

    public WebPage get(int pageNo) {
        return getIndex(pageNo);
    }

    public void index(CharSequence url) {
        indexAll(1, Lists.newArrayList(url));
    }

    public void indexAll(Iterable<CharSequence> urls) {
        indexAll(1, urls);
    }

    public void indexAll(int pageNo, Iterable<CharSequence> urls) {
        updateAll(pageNo, urls, false);
    }

    public Set<CharSequence> getAll(int pageNo) {
        return get(pageNo).getVividLinks().keySet();
    }

    /**
     * Return a copy of all urls in page N, and clear it's urls
     */
    synchronized public Set<CharSequence> takeN(int pageNo, int n) {
        WebPage page = get(pageNo);

        Set<CharSequence> urls = new HashSet<>();
        Iterator<Map.Entry<CharSequence, CharSequence>> it = page.getVividLinks().entrySet().iterator();
        while (n-- > 0 && it.hasNext()) {
            urls.add(it.next().getKey());
            it.remove();
        }

        if (!urls.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Taken {} urls from page {}", urls.size(), page.getUrl());
            }
        }

        webDb.put(page.getUrl(), page);
        webDb.flush();

        return urls;
    }

    synchronized public Set<CharSequence> takeAll(int pageNo) {
        return takeN(pageNo, Integer.MAX_VALUE);
    }

    public void remove(String url) {
        remove(1, url);
    }

    public void removeAll(Iterable<CharSequence> urls) {
        removeAll(1, urls);
    }

    public void remove(int pageNo, CharSequence url) {
        updateAll(pageNo, Lists.newArrayList(url), true);
    }

    public void removeAll(int pageNo, Iterable<CharSequence> urls) {
        updateAll(pageNo, urls, true);
    }

    public void commit() {
        webDb.flush();
    }

    private void update(int pageNo, HypeLink newHypeLinks, boolean remove) {
        updateAll(pageNo, Lists.newArrayList(newHypeLinks.getUrl()), remove);
    }

    synchronized private void updateAll(int pageNo, Iterable<CharSequence> urls, boolean remove) {
        if (!urls.iterator().hasNext()) {
            return;
        }

        WebPage indexPage = getIndex(pageNo);

        Map<CharSequence, CharSequence> vividLinks = indexPage.getVividLinks();
        if (remove) {
            urls.forEach(vividLinks::remove);
        } else {
            urls.forEach(l -> vividLinks.put(l, ""));
        }

        String message = "Total " + vividLinks.size() + " indexed links";
        indexPage.setTextCascaded(message);

        if (LOG.isDebugEnabled()) {
            LOG.debug(message + ", indexed in " + indexPage.getUrl());
        }

        // webDb.put(indexPage.getUrl(), indexPage, true);
        webDb.put(indexPage.getUrl(), indexPage);
        webDb.flush();
    }

    synchronized private WebPage getHome() {
        WebPage home = webDb.getOrNil(homeUrl);
        if (home.isNil()) {
            home = WebPage.newInternalPage(homeUrl, "Web Page Index Home");
            LOG.debug("Creating weak index home: " + homeUrl);
        }

        webDb.put(homeUrl, home);
        webDb.flush();

        return home;
    }

    private WebPage getIndex(int pageNo) {
        return getIndex(pageNo, "Web Page Index " + pageNo);
    }

    synchronized private WebPage getIndex(int pageNo, String pageTitle) {
        String url = homeUrl + "/" + pageNo;

        WebPage indexPage = webDb.getOrNil(url);
        if (indexPage.isNil()) {
            WebPage home = getHome();
            home.getVividLinks().put(url, "");
            webDb.put(homeUrl, home);

            indexPage = WebPage.newInternalPage(url, pageTitle);
            webDb.put(url, indexPage);
            webDb.flush();

            // log.debug("Created weak index: " + url);
        }

        return indexPage;
    }
}
