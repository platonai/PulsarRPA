package fun.platonic.pulsar.crawl.fetch;

import fun.platonic.pulsar.crawl.protocol.ForwardingResponse;

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
