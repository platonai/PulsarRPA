package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.common.config.AppConstants.FETCH_PRIORITY_DEFAULT
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes

class FetchJobForwardingResponse(
        headers: MultiMetadata,
        content: ByteArray
): ForwardingResponse(WebPage.newWebPage(headers.get(HttpHeaders.Q_URL)), getProtocolStatus(headers)!!, headers, content), HttpHeaders {

    val jobId: Int
        get() = headers.getInt(HttpHeaders.Q_JOB_ID, 0)

    val priority: Int
        get() = headers.getInt(HttpHeaders.Q_PRIORITY, FETCH_PRIORITY_DEFAULT)

    val queueId: String
        get() = headers.get(HttpHeaders.Q_QUEUE_ID)?:""

    val itemId: Int
        get() = headers.getInt(HttpHeaders.Q_ITEM_ID, 0)

    companion object {
        fun getProtocolStatus(headers: MultiMetadata): ProtocolStatus? {
            return ProtocolStatus.fromMinor(headers.getInt(HttpHeaders.Q_STATUS_CODE, ProtocolStatusCodes.NOTFOUND))
        }
    }
}
