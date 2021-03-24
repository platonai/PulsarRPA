package ai.platon.pulsar.client

class SinkResponse(
        val statusCode: Int,
        val headers: Map<String, List<String>>,
        val body: String
)

abstract class SinkCommitter {
    open fun commit(requestBody: String, sinkDescriptor: String): Int = commit(requestBody, sinkDescriptor, {})
    abstract fun commit(requestBody: String, sinkDescriptor: String, thenAction: (response: SinkResponse) -> Unit): Int
}
