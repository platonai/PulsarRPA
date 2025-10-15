package ai.platon.pulsar.external.impl

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.external.*
import ai.platon.pulsar.external.logging.ChatModelLogger
import dev.langchain4j.data.message.*
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.output.FinishReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.jsoup.nodes.Element
import java.io.IOException
import java.io.InterruptedIOException
import java.time.Duration

open class CachedBrowserChatModel(
    val langchainModel: dev.langchain4j.model.chat.ChatModel,
    private val conf: ImmutableConfig
) : BrowserChatModel {
    private val logger = getLogger(CachedBrowserChatModel::class)

    private val llmResponseCacheTTL = conf.getLong("llm.response.cache.ttl", 600L) // Default to 10 minutes if not set

    private val cacheManager: CacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache(
            "modelResponses",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java,
                ModelResponse::class.java,
                ResourcePoolsBuilder.heap(1000) // Maximum entries in cache
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(llmResponseCacheTTL))) // TTL: 60 minutes
        )
        .build(true)

    private val responseCache: Cache<String, ModelResponse> =
        cacheManager.getCache("modelResponses", String::class.java, ModelResponse::class.java)

    override val settings = ChatModelSettings(conf)

    override suspend fun call(userMessage: String) = callUmSm(userMessage, "")

    override suspend fun call(document: FeaturedDocument, prompt: String) = call(document.document, prompt)

    override suspend fun call(ele: Element, prompt: String) = callUmSm(ele.text(), prompt)

    override suspend fun callSmUm(
        systemMessage: String, userMessage: String,
        imageUrl: String?, b64Image: String?, mediaType: String?
    ): ModelResponse {
        return callSmUmWithCache(systemMessage, userMessage, imageUrl, b64Image, mediaType)
    }

    override suspend fun callUmSm(
        userMessage: String, systemMessage: String,
        imageUrl: String?, b64Image: String?, mediaType: String?
    ): ModelResponse {
        return callUmSmWithCache(userMessage, systemMessage, imageUrl, b64Image, mediaType)
    }

    /**
     * This is the main API to interact with the chat model.
     *
     * @param chatRequest a [ChatRequest], containing all the inputs to the LLM
     * @return a [ChatResponse], containing all the outputs from the LLM
     */
    override suspend fun send(chatRequest: ChatRequest): ChatResponse {
        return withContext(Dispatchers.IO) {
            langchainModel.chat(chatRequest)
        }
    }

    override suspend fun send(vararg messages: ChatMessage): ChatResponse {
        return withContext(Dispatchers.IO) {
            langchainModel.chat(*messages)
        }
    }

    override suspend fun send(messages: List<ChatMessage>): ChatResponse {
        return withContext(Dispatchers.IO) {
            langchainModel.chat(messages)
        }
    }

    private suspend fun callUmSmWithCache(
        userMessage: String, systemMessage: String,
        imageUrl: String? = null,
        b64Image: String? = null, mediaType: String? = null,
    ): ModelResponse {
        return callSmUmWithCache(systemMessage, userMessage, imageUrl, b64Image, mediaType)
    }

    private suspend fun callSmUmWithCache(
        systemMessage: String, userMessage: String,
        imageUrl: String? = null,
        b64Image: String? = null, mediaType: String? = null,
    ): ModelResponse {
        if (userMessage.isBlank()) {
            logger.warn("No user message, return empty response")
            return ModelResponse("", ResponseState.OTHER)
        }

        val trimmedUserMessage = userMessage.take(settings.maximumLength).trim()

        // Build cache key; include b64Image/mediaType hash if provided
        val b64ImageProvided = !b64Image.isNullOrBlank() && !mediaType.isNullOrBlank()
        val imageUrlProvided = !imageUrl.isNullOrBlank()
        val imageProvided = b64ImageProvided || imageUrlProvided
        val attachmentKeyPart = if (b64ImageProvided) {
            val dataUrl = "data:$mediaType;base64,$b64Image"
            ":" + DigestUtils.md5Hex(dataUrl)
        } else if (imageUrlProvided) {
            DigestUtils.md5Hex(imageUrl)
        } else ""

        val cacheKey = DigestUtils.md5Hex("$trimmedUserMessage|$systemMessage$attachmentKeyPart")

        // Check if the response is already cached
        responseCache.get(cacheKey)?.let {
            logger.debug("Returning cached response for key: $cacheKey")
            return it
        }

        // 记录请求
        val requestId = ChatModelLogger.logRequest(trimmedUserMessage, systemMessage)

        // Build user message, optionally with b64Image content parts
        val um: UserMessage = if (imageProvided) {
            // Build a data URL for the image to be compatible with OpenAI-style vision inputs
            val dataUrl = imageUrl ?: "data:$mediaType;base64,$b64Image"

            val contents = mutableListOf<Content>()

            contents.add(TextContent.from(trimmedUserMessage))
            contents.add(ImageContent.from(dataUrl))

            UserMessage.userMessage(contents)
        } else {
            UserMessage.userMessage(trimmedUserMessage)
        }

        if (logger.isInfoEnabled) {
            val log = StringUtils.abbreviate(trimmedUserMessage, 100).replace("\n", " ")
            logger.info("▶ Chat - [len: {}] {}", trimmedUserMessage.length, log)
        }

        val response: ChatResponse = try {
            if (systemMessage.isBlank()) {
                // langchainModel.chat(um)
                sendChatMessageInIOThread(um)
            } else {
                val sm = SystemMessage.systemMessage(systemMessage)
                // langchainModel.chat(sm, um)
                sendChatMessageInIOThread(sm, um)
            }
        } catch (e: IOException) {
            logger.info("IOException | {}", e.message)
            return ModelResponse("", ResponseState.OTHER).also {
                ChatModelLogger.logResponse(requestId, it)
            }
        } catch (e: RuntimeException) {
            if (e.cause is InterruptedIOException) {
                logger.info("InterruptedIOException | {}", e.message)
                return ModelResponse("", ResponseState.OTHER).also {
                    ChatModelLogger.logResponse(requestId, it)
                }
            } else {
                logger.warn("RuntimeException | {} | {}", langchainModel.javaClass.simpleName, e.message)
                throw e
            }
        } catch (e: Exception) {
            logger.warn("[Unexpected] Exception | {} | {}", langchainModel.javaClass.simpleName, e.message)
            return ModelResponse("", ResponseState.OTHER).also {
                ChatModelLogger.logResponse(requestId, it)
            }
        }

        val u = response.tokenUsage()
        val tokenUsage = TokenUsage(u.inputTokenCount(), u.outputTokenCount(), u.totalTokenCount())
        val r = response.finishReason()
        val state = when (r) {
            FinishReason.STOP -> ResponseState.STOP
            FinishReason.LENGTH -> ResponseState.LENGTH
            FinishReason.TOOL_EXECUTION -> ResponseState.TOOL_EXECUTION
            FinishReason.CONTENT_FILTER -> ResponseState.CONTENT_FILTER
            else -> ResponseState.OTHER
        }

        val modelResponse = ModelResponse(response.aiMessage().text().trim(), state, tokenUsage)
        if (logger.isInfoEnabled) {
            val log = StringUtils.abbreviate(modelResponse.content, 100).replace("\n", " ")
            logger.info(
                "◀ Chat - token: {} | [len: {}] {}",
                modelResponse.tokenUsage.totalTokenCount,
                modelResponse.content.length,
                log
            )
        }

        // 记录响应
        ChatModelLogger.logResponse(requestId, modelResponse)

        // Cache the response
        responseCache.put(cacheKey, modelResponse)
        logger.debug("Cached response for key: $cacheKey")

        return modelResponse
    }

    private suspend fun sendChatMessageInIOThread(vararg messages: ChatMessage): ChatResponse {
        return withContext(Dispatchers.IO) {
            langchainModel.chat(*messages)
        }
    }
}
