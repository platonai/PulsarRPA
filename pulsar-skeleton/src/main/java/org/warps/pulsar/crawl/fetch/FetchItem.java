package org.warps.pulsar.crawl.fetch;

import org.warps.pulsar.crawl.protocol.ForwardingResponse;

public class FetchItem {
    private FetchTask task;
    private ForwardingResponse response;

    public FetchItem(FetchTask task, ForwardingResponse response) {
        this.task = task;
        this.response = response;
    }

    public FetchTask getTask() {
        return task;
    }

    public ForwardingResponse getResponse() {
        return response;
    }
}
