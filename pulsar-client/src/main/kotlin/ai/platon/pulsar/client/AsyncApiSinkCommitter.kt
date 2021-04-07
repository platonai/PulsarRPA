package ai.platon.pulsar.client

import ai.platon.pulsar.common.urls.Urls
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentSkipListSet

class AsyncApiSinkCommitter: SinkCommitter() {
    private val log = LoggerFactory.getLogger(AsyncApiSinkCommitter::class.java)
    private val client = HttpClient.newHttpClient()
    private val badSinkDescriptor = ConcurrentSkipListSet<String>()

    override fun commit(requestBody: String, sinkDescriptor: String, thenAction: (response: SinkResponse) -> Unit): Int {
        if (!Urls.isValidUrl(sinkDescriptor)) {
            if (sinkDescriptor !in badSinkDescriptor) {
                log.warn("Bad sink descriptor | {}", sinkDescriptor)
                badSinkDescriptor.add(sinkDescriptor)
            }
            return -1
        }

        val request = HttpRequest.newBuilder(URI.create(sinkDescriptor))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApplyAsync {
            thenAction(SinkResponse(it.statusCode(), it.headers().map(), it.body()))
        }

        return 0
    }
}
