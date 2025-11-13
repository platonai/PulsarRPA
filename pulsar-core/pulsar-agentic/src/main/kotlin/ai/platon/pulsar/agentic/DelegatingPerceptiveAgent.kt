package ai.platon.pulsar.agentic

import ai.platon.pulsar.skeleton.ai.*
import ai.platon.pulsar.skeleton.ai.support.ExtractionSchema
import java.util.*

class DelegatingPerceptiveAgent(
    val session: AgenticSession
) : PerceptiveAgent {
    private val historyAgents = mutableListOf<PerceptiveAgent>()

    private var agent: PerceptiveAgent = BrowserPerceptiveAgent(session)

    override val uuid: UUID = UUID.randomUUID()
    override val stateHistory: List<AgentState> get() = agent.stateHistory
    override val processTrace: List<ProcessTrace> get() = agent.processTrace

    fun newContext(): DelegatingPerceptiveAgent {
        agent.close()
        historyAgents.add(agent)
        agent = BrowserPerceptiveAgent(session)
        return this
    }

    override suspend fun resolve(problem: String): ActResult {
        newContext()
        return agent.resolve(problem)
    }

    // Every time call resolve, create a new BrowserPerceptiveAgent to do the job
    override suspend fun resolve(action: ActionOptions): ActResult {
        newContext()
        return agent.resolve(action)
    }

    override suspend fun observe(instruction: String): List<ObserveResult> {
        return agent.observe(instruction)
    }

    override suspend fun observe(options: ObserveOptions): List<ObserveResult> {
        return agent.observe(options)
    }

    override suspend fun act(action: String): ActResult {
        return agent.act(action)
    }

    override suspend fun act(action: ActionOptions): ActResult {
        return agent.act(action)
    }

    override suspend fun act(observe: ObserveResult): ActResult {
        return agent.act(observe)
    }

    override suspend fun extract(instruction: String): ExtractResult {
        return agent.extract(instruction)
    }

    override suspend fun extract(
        instruction: String,
        schema: ExtractionSchema
    ): ExtractResult {
        return agent.extract(instruction, schema)
    }

    override suspend fun extract(options: ExtractOptions): ExtractResult {
        return agent.extract(options)
    }

    override fun close() {
        runCatching { agent.close() }
    }
}
