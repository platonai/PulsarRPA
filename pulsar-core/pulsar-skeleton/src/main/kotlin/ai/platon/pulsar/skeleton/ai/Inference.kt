package ai.platon.pulsar.skeleton.ai

import ai.platon.pulsar.browser.driver.chrome.dom.DomService
import ai.platon.pulsar.browser.driver.chrome.dom.model.PageTarget
import ai.platon.pulsar.browser.driver.chrome.dom.model.SnapshotOptions
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import dev.langchain4j.model.chat.ChatModel

class InferenceEngine(
    private val driver: WebDriver,
    private val chatModel: ChatModel
) {
    private val domService: DomService = (driver as AbstractWebDriver).domService!!

    fun extract(instruction: String) {
        val domTree = domService.getAllTrees(PageTarget(), SnapshotOptions())
    }

    fun extract(instruction: String, domElements: List<String>) {
//        const extractCallMessages: ChatMessage[] = [
//            buildExtractSystemPrompt(isUsingAnthropic, userProvidedInstructions),
//            buildExtractUserPrompt(instruction, domElements, isUsingAnthropic),
//        ];

        // val extractionResponse = chatModel.chat(extractCallMessages)

//        const metadataCallMessages: ChatMessage[] = [
//            buildMetadataSystemPrompt(),
//            buildMetadataPrompt(instruction, extractedData, chunksSeen, chunksTotal),
//        ];

        // val extractionResponse = chatModel.chat(metadataCallMessages)

//        return {
//            ...extractedData,
//            metadata: {
//                completed: metadataResponseCompleted,
//                progress: metadataResponseProgress,
//            },
//            prompt_tokens: totalPromptTokens,
//            completion_tokens: totalCompletionTokens,
//            inference_time_ms: totalInferenceTimeMs,
//        };

    }

    fun observe(instruction: String, domElements: List<String>) {

    }
}
