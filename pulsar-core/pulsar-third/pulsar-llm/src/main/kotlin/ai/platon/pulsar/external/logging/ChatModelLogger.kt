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
                val sb = StringBuilder()
                sb.appendLine("--------------------------------------------------------------------")
                sb.append("REQUEST ID: ${pair.id}\n")
                sb.append("TIMESTAMP: ${pair.timestamp}\n")
                sb.append("USER MESSAGE:\n${pair.userMessage}\n")
                sb.append("SYSTEM MESSAGE:\n${pair.systemMessage}\n")
                sb.append("RESPONSE TIMESTAMP: ${pair.responseTimestamp}\n")
                sb.append("RESPONSE STATE: ${pair.response?.state}\n")
                sb.append("TOKEN USAGE: ${pair.response?.tokenUsage?.totalTokenCount ?: "N/A"}\n")
                sb.append("RESPONSE CONTENT:\n${pair.response?.content ?: "No response"}")
                writer.write(sb.toString())
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
