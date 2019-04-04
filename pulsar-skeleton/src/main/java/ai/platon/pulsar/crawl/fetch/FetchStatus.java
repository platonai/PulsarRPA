package ai.platon.pulsar.crawl.fetch;

import ai.platon.pulsar.common.Urls;

import javax.annotation.Nonnull;

/**
 * Created by vincent on 16-10-15.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
public class FetchStatus implements Comparable<FetchStatus> {

    public String hostName;
    public int urls = 0;
    public int indexUrls = 0;
    public int detailUrls = 0;
    public int searchUrls = 0;
    public int mediaUrls = 0;
    public int bbsUrls = 0;
    public int blogUrls = 0;
    public int tiebaUrls = 0;
    public int unknownUrls = 0;
    public int urlsTooLong = 0;
    public int urlsFromSeed = 0;

    public FetchStatus(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public int compareTo(@Nonnull FetchStatus fetchStatus) {
        if (hostName == null || fetchStatus.hostName == null) {
            return -1;
        }

        String reverseHost = Urls.reverseHost(hostName);
        String reverseHost2 = Urls.reverseHost(fetchStatus.hostName);

        return reverseHost.compareTo(reverseHost2);
    }
}
