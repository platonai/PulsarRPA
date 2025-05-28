package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.dom.nodes.node.ext.numChars
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.rest.api.common.DomUtils
import ai.platon.pulsar.rest.api.common.PLACEHOLDER_PAGE_CONTENT
import ai.platon.pulsar.rest.api.common.PromptUtils
import ai.platon.pulsar.rest.api.common.ScrapeAPIUtils
import ai.platon.pulsar.rest.api.entities.*
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.session.PulsarSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap

@Service
class CommandService(
    val session: PulsarSession,
    val loadService: LoadService,
    val conversationService: ConversationService,
    val scrapeService: ScrapeService,
) {
    companion object {
        const val MIN_USER_MESSAGE_LENGTH = 2
        const val FLOW_POLLING_INTERVAL = 1000L
    }

    // TODO: use ehcache
    private val commandStatusCache = ConcurrentSkipListMap<String, CommandStatus>()

    // Create a dedicated dispatcher for long-running command operations
    private val commandDispatcher = Dispatchers.IO.limitedParallelism(10) // Adjust number based on your server capacity

    private val commanderScope: CoroutineScope = CoroutineScope(
        commandDispatcher + SupervisorJob() + CoroutineName("commander")
    )

    private val logger = getLogger(CommandService::class)

    fun executeSync(request: CommandRequest): CommandStatus {
        val status = createCachedCommandStatus(request)
        executeCommand(request, status)
        return status
    }

    fun submitAsync(request: CommandRequest): String {
        val status = createCachedCommandStatus(request)
        commanderScope.launch { executeCommand(request, status) }
        return status.id
    }

    fun getStatus(id: String) = commandStatusCache[id]

    fun getResult(id: String) = commandStatusCache[id]?.commandResult

    fun streamEvents(id: String): Flux<ServerSentEvent<CommandStatus>> {
        var doneCount = 0
        val handleFluxSink = { sink: FluxSink<CommandStatus> ->
            val job = commandStatusFlow(id).onEach {
                sink.next(it)

                if (it.isDone) {
                    ++doneCount
                }

                if (doneCount >= 5) {
                    // the client may not close the connection correctly
                    // check the exact behavior when the underlying flow is finished
                    sink.complete()
                }
            }.catch {
                logger.error("Error in command status flow", it)
                sink.error(it)
            }.launchIn(commanderScope)

            sink.onDispose {
                job.cancel()
            }
        }

        return Flux.create { sink -> handleFluxSink(sink) }.map {
            // ServerSentEvent.builder(it).id(it.id).event(it.event).build()
            // NOTE: [2025/5/20] JavaScript client-side code expects only JSON data, not the event ID nor event name.
            ServerSentEvent.builder(it).build()
        }
    }

    fun commandStatusFlow(id: String): Flow<CommandStatus> = flow {
        var lastModifiedTime = Instant.EPOCH
        do {
            delay(FLOW_POLLING_INTERVAL)

            val status = commandStatusCache[id] ?: CommandStatus.notFound(id)
            if (status.refreshed(lastModifiedTime)) {
                emit(status)
                lastModifiedTime = status.lastModifiedTime
            }

            if (status.isDone) {
                // emit a final event, it's OK to emit a duplicate event
                emit(status)
            }
        } while (!status.isDone)
    }

    /**
     * Executes a command based on the provided request string.
     *
     * This method first attempts to convert the request string into a PromptRequestL2 object.
     * If successful, it calls the command method with the PromptRequestL2 object.
     * If not, it returns a failed status with a status code indicating a bad request.
     *
     * @param request The request string containing a URL and other parameters.
     * @return A PromptResponseL2 object containing the result of the command execution.
     * */
    fun executeCommand(request: String): CommandStatus {
        if (request.isBlank()) {
            return CommandStatus.failed(ResourceStatus.SC_BAD_REQUEST)
        }

        val request2 = conversationService.normalizePlainCommand(request)
        val status = createCachedCommandStatus(request2)
        if (request2 == null) {
            status.failed(ResourceStatus.SC_EXPECTATION_FAILED)
            return status
        }

        return executeCommand(request2, status)
    }

    fun executeCommand(request: CommandRequest): CommandStatus {
        val status = createCachedCommandStatus(request)
        executeCommand(request, status)
        return status
    }

    /**
     * Executes a command based on the provided PromptRequestL2 object.
     *
     * This method loads the document associated with the request, processes the chat and data extraction rules,
     * and returns a PromptResponseL2 object containing the results.
     *
     * @param request The PromptRequestL2 object containing the URL and other parameters.
     * @return A PromptResponseL2 object containing the result of the command execution.
     * */
    fun executeCommand(request: CommandRequest, status: CommandStatus): CommandStatus {
        try {
            status.refresh(ResourceStatus.SC_PROCESSING)
            executeCommandStepByStep(request, status)
        } catch (e: Exception) {
            status.failed(ResourceStatus.SC_EXPECTATION_FAILED)
        } finally {
            status.done()
        }

        return status
    }

    private fun createCachedCommandStatus(request: CommandRequest? = null): CommandStatus {
        val status = CommandStatus()
        // status.request = request
        commandStatusCache[status.id] = status
        status.refresh("created")
        return status
    }

    private fun executeCommandStepByStep(request: CommandRequest, status: CommandStatus) {
        request.args = LoadOptions.mergeArgs(request.args, "-refresh")
        val (page, document) = loadService.loadDocument(request)

        if (page.isNil) {
            status.failed(ResourceStatus.SC_EXPECTATION_FAILED)
            return
        }

        status.pageStatusCode = page.protocolStatus.minorCode
        status.pageContentBytes = page.originalContentLength.toInt()
        if (!page.protocolStatus.isSuccess) {
            return
        }

        executeCommandStepByStep(page, document, request, status)
        logger.info("Finished executeCommandStepByStep | status: {} | {}", status.status, document.baseURI)

        val sql = request.xsql
        if (sql != null && ScrapeAPIUtils.isScrapeUDF(sql)) {
            status.refresh(ResourceStatus.SC_PROCESSING)
            kotlin.runCatching { executeQuery(sql, status) }.onFailure { logger.warn("Failed to execute query", it) }
        }

        status.refresh(ResourceStatus.SC_OK)
    }

    private fun executeCommandStepByStep(
        page: WebPage, document: FeaturedDocument, request: CommandRequest, status: CommandStatus
    ) {
        try {
            executeCommandStepByStep0(page, document, request, status)
        } catch (e: Exception) {
            status.failed(ResourceStatus.SC_EXPECTATION_FAILED)
        }
    }

    private fun executeCommandStepByStep0(
        page: WebPage, document: FeaturedDocument, request: CommandRequest, status: CommandStatus
    ) {
        // the 0-based screen number, 0.00 means at the top of the first screen, 1.50 means halfway through the second screen.
        val screenNumber = page.activeDOMMetadata?.screenNumber ?: 0f

        val pageSummaryPrompt = PromptUtils.normalizePageSummaryPrompt(request.pageSummaryPrompt)
        val dataExtractionRules = PromptUtils.normalizeDataExtractionRules(request.dataExtractionRules)
        var richText: String? = null
        var textContent: String? = null
        if (pageSummaryPrompt != null || dataExtractionRules != null) {
            textContent = if (request.richText == true) {
                DomUtils.selectNthScreenRichText(screenNumber, document).also { richText = it }
            } else {
                DomUtils.selectNthScreenText(screenNumber, document)
            }
            status.refresh("textContent")

            if (textContent.isBlank()) {
                if (document.body.numChars > 100) {
                    val path = document.export()
                    logger.warn(
                        "Not textContent found on screen: {} but there are chars in body: {}, exported to {}",
                        screenNumber, document.body.numChars, path.toUri()
                    )
                }
                return
            }

            if (pageSummaryPrompt != null) {
                val instruct = pageSummaryPrompt.replace(PLACEHOLDER_PAGE_CONTENT, textContent)
                performInstruct("pageSummary", instruct, status)
                logger.info("pageSummary: {}", status.commandResult?.pageSummary)
            }

            if (dataExtractionRules != null) {
                val instruct = dataExtractionRules.replace(PLACEHOLDER_PAGE_CONTENT, textContent)
                performInstruct("fields", instruct, status)
                logger.info("fields: {}", status.commandResult?.fields)
            }
        }

        val linkExtractionRules = PromptUtils.normalizeLinkExtractionRules(request.linkExtractionRules)
        if (linkExtractionRules != null) {
            val links = DomUtils.selectNthScreenLinks(document).filter { it.matches(linkExtractionRules) }
            if (links.isNotEmpty()) {
                val result = InstructResult.ok("links", links.joinToString("\n"))
                status.addInstructResult(result)
            }
            logger.info("Use regex to extract {} links: {}", links.size, linkExtractionRules)
        }
    }

    private fun performInstruct(name: String, instruct: String, status: CommandStatus) {
        val result = try {
            val content = session.chat(instruct).content
            InstructResult.ok(name, content)
        } catch (e: Exception) {
            logger.warn("Failed to perform instruct: $instruct", e)
            InstructResult.failed(name)
        }

        status.addInstructResult(result)
    }

    private fun executeQuery(sql: String, status: CommandStatus) {
        val scrapeRequest = ScrapeRequest(sql)
        try {
            val scrapeResponse = scrapeService.executeQuery(scrapeRequest)
            status.statusCode = scrapeResponse.statusCode
            status.ensureCommandResult().xsqlResultSet = scrapeResponse.resultSet
        } catch (e: Exception) {
            status.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
        }
    }
}
