package ai.platon.pulsar.crawl.fetch;

import ai.platon.pulsar.common.HttpHeaders;
import ai.platon.pulsar.persist.ProtocolStatus;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import ai.platon.pulsar.crawl.protocol.ForwardingResponse;
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes;
import org.apache.commons.lang3.math.NumberUtils;

import static ai.platon.pulsar.common.config.PulsarConstants.FETCH_PRIORITY_DEFAULT;

/**
 *
 * */
public class FetchJobForwardingResponse extends ForwardingResponse implements HttpHeaders {

    public FetchJobForwardingResponse(MultiMetadata headers, byte[] content) {
        super(headers.get(Q_URL), content, ProtocolStatus.fromMinor(NumberUtils.toInt(headers.get(Q_STATUS_CODE), ProtocolStatusCodes.NOTFOUND)), headers);
    }

    public int getJobId() {
        return NumberUtils.toInt(getHeaders().get(Q_JOB_ID), 0);
    }

    public int getPriority() {
        return NumberUtils.toInt(getHeaders().get(Q_PRIORITY), FETCH_PRIORITY_DEFAULT);
    }

    public String getQueueId() {
        return getHeaders().get(Q_QUEUE_ID);
    }

    public int getItemId() {
        return NumberUtils.toInt(getHeaders().get(Q_ITEM_ID), 0);
    }
}
