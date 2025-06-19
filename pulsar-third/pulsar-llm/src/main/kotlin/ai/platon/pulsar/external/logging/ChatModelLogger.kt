package ai.platon.pulsar.external.logging

import ai.platon.pulsar.common.MessageWriter
import ai.platon.pulsar.common.concurrent.ConcurrentExpiringLRUCache
import ai.platon.pulsar.external.ModelResponse
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

class ChatModelLogger {
    companion object {
        private val logger = LoggerFactory.getLogger(ChatModelLogger::class.java)
        private val counter = AtomicInteger(0)
        private val requestResponseMap = ConcurrentExpiringLRUCache<Int, RequestResponsePair>(ttl = Duration.ofMinutes(10))

        private var logDirectory = "logs/chat-model"
        private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        private val writer: MessageWriter

        fun configure(logDirectory: String = "logs/chat-model") {
            this.logDirectory = logDirectory
        }

        init {
            File(logDirectory).mkdirs()
            val dateStr = LocalDateTime.now().format(dateFormat)
            writer = MessageWriter(Paths.get(logDirectory).resolve("chat_$dateStr.log"))
        }

        /**
         * 记录请求
         * @return 请求ID
         */
        fun logRequest(userMessage: String, systemMessage: String): Int {
            val requestId = counter.incrementAndGet()
            val timestamp = LocalDateTime.now()
            val request = RequestResponsePair(requestId, timestamp, userMessage, systemMessage)
            requestResponseMap.putDatum(requestId, request)

            return requestId
        }

        /**
         * 记录响应
         */
        fun logResponse(requestId: Int, response: ModelResponse) {
            val pair = requestResponseMap.getDatum(requestId) ?: return
            pair.response = response
            pair.responseTimestamp = LocalDateTime.now()

            writeToFile(pair)
        }

        private fun writeToFile(pair: RequestResponsePair) {
            try {
                val pairString = buildString {
                    appendLine("--------------------------------------------------------------------")
                    append("REQUEST ID: ${pair.id}\n")
                    append("TIMESTAMP: ${pair.timestamp}\n")
                    append("USER MESSAGE:\n${pair.userMessage}\n")
                    append("SYSTEM MESSAGE:\n${pair.systemMessage}\n")
                    append("RESPONSE TIMESTAMP: ${pair.responseTimestamp}\n")
                    append("RESPONSE STATE: ${pair.response?.state}\n")
                    append("TOKEN USAGE: ${pair.response?.tokenUsage?.totalTokenCount ?: "N/A"}\n")
                    append("RESPONSE CONTENT:\n${pair.response?.content ?: "No response"}")
                }
                writer.write(pairString)
            } catch (e: Exception) {
                logger.error("Failed to write chat log to file", e)
            }
        }
    }

    /**
     * 请求响应对象，用于存储一次对话的请求和响应
     */
    data class RequestResponsePair(
        val id: Int,
        val timestamp: LocalDateTime,
        val userMessage: String,
        val systemMessage: String,
        var response: ModelResponse? = null,
        var responseTimestamp: LocalDateTime? = null
    )
}
