package ai.platon.pulsar.rest.api.service

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

        return if (page.protocolStatus.isSuccess) {
            session.chat(request.prompt, document.text).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }

    fun extract(request: PromptRequest): String {
        request.args = LoadOptions.mergeArgs(request.args, "-refresh")
        val (page, document) = loadService.loadDocument(request)

        val prompt = """
            Extract the following information from the web page:
            ${request.prompt}
            """.trimIndent() + "\n\n" + document.text
        return if (page.protocolStatus.isSuccess) {
            session.chat(prompt).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }
}
