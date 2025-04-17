package ai.platon.pulsar.rest.api.service

import ai.platon.pulsar.rest.api.entities.PromptRequest
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.crawl.common.GlobalCacheFactory
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.springframework.stereotype.Service

@Service
class PromptService(
    val session: PulsarSession,
    val globalCacheFactory: GlobalCacheFactory,
) {
    fun chat(prompt: String): String {
        return session.chat(prompt).content
    }

    fun chat(request: PromptRequest): String {
        val page = session.load(request.url, "-refresh")
        val document = session.parse(page)
        return if (page.protocolStatus.isSuccess) {
            session.chat(request.prompt, document.text).content
        } else {
            // Throw?
            page.protocolStatus.toString()
        }
    }

    fun extract(request: PromptRequest): String {
        val page = session.load(request.url, "-refresh")
        val document = session.parse(page)

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
