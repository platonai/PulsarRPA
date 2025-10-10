package ai.platon.pulsar.skeleton.ai

import dev.langchain4j.model.chat.ChatModel

class InferenceEngine(val chatModel: ChatModel) {
    fun extract(instruction: String, domElements: List<String>) {
//        const extractCallMessages: ChatMessage[] = [
//            buildExtractSystemPrompt(isUsingAnthropic, userProvidedInstructions),
//            buildExtractUserPrompt(instruction, domElements, isUsingAnthropic),
//        ];

        val extractionResponse = chatModel.chat(instruction)

//        const metadataCallMessages: ChatMessage[] = [
//            buildMetadataSystemPrompt(),
//            buildMetadataPrompt(instruction, extractedData, chunksSeen, chunksTotal),
//        ];

//        return {
//            ...extractedData,
//            metadata: {
//                completed: metadataResponseCompleted,
//                progress: metadataResponseProgress,
//        },
//            prompt_tokens: totalPromptTokens,
//            completion_tokens: totalCompletionTokens,
//            inference_time_ms: totalInferenceTimeMs,
//        };

    }

    fun observe(instruction: String, domElements: List<String>) {

    }
}
