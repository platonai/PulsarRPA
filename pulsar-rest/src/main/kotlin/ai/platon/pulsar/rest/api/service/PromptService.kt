package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.rest.api.common.DEFAULT_INTRODUCE
import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.skeleton.common.options.LoadOptions
import ai.platon.pulsar.skeleton.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.springframework.stereotype.Service

@Service
class PromptService(
    val session: PulsarSession,
    val globalCacheFactory: GlobalCacheFactory,
    val loadService: LoadService,
) {
    fun chat(prompt: String): String {
        return session.chat(prompt).content
    }

    fun chat(request: PromptRequest): String {
        request.args = LoadOptions.mergeArgs(request.args, "-refresh")
        val (page, document) = loadService.loadDocument(request)

        val prompt = request.prompt
        if (prompt.isNullOrBlank()) {
            return DEFAULT_INTRODUCE
        }

        return if (page.protocolStatus.isSuccess) {
            session.chat(prompt, document.text).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }

    fun extract(request: PromptRequest): String {
        val prompt = request.prompt
        if (prompt.isNullOrBlank()) {
            return DEFAULT_INTRODUCE
        }

        request.args = LoadOptions.mergeArgs(request.args, "-refresh")
        val (page, document) = loadService.loadDocument(request)

        val prompt2 = """
            Extract the following information from the web page:
            $prompt
            
            ${document.text}


            """.trimIndent()

        return if (page.protocolStatus.isSuccess) {
            session.chat(prompt2).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }
}
