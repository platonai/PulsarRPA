package ai.platon.pulsar.skeleton.crawl.llm

import ai.platon.pulsar.external.ModelResponse
import ai.platon.pulsar.skeleton.ai.tta.TextToAction
import ai.platon.pulsar.skeleton.context.PulsarContexts

open class TTATestBase {

    companion object {
        val session = PulsarContexts.createSession()
        var lastResponse: ModelResponse? = null

        val textToAction = TextToAction(session)
    }
}