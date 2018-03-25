package fun.platonic.pulsar.crawl.fetch;

import fun.platonic.pulsar.common.UrlUtil;

import javax.annotation.Nonnull;

/**
 * Created by vincent on 16-10-15.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
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

        String reverseHost = UrlUtil.reverseHost(hostName);
        String reverseHost2 = UrlUtil.reverseHost(fetchStatus.hostName);

        return reverseHost.compareTo(reverseHost2);
    }
}
