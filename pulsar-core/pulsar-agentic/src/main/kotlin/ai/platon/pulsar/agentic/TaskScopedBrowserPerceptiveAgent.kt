package ai.platon.pulsar.agentic

import java.util.*

/**
 * An agent delegate that creates a new agent every time starts a new resolve loop.
 * */
class TaskScopedBrowserPerceptiveAgent(
    val session: AgenticSession
) : PerceptiveAgent {
    private val historyAgents = mutableListOf<PerceptiveAgent>()

    private var agent: PerceptiveAgent = ObserveActBrowserAgent(session)

    override val uuid: UUID = UUID.randomUUID()
    override val stateHistory: List<AgentState> get() = agent.stateHistory
    override val processTrace: List<ProcessTrace> get() = agent.processTrace

    override suspend fun run(task: String): List<AgentState> {
        newContext()
        return agent.run(task)
    }

    // Every time call resolve, create a new BrowserPerceptiveAgent to do the job
    override suspend fun run(action: ActionOptions): List<AgentState> {
        newContext()
        return agent.run(action)
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

    override suspend fun extract(instruction: String, schema: ExtractionSchema): ExtractResult {
        return agent.extract(instruction, schema)
    }

    override suspend fun extract(options: ExtractOptions): ExtractResult {
        return agent.extract(options)
    }

    override suspend fun clearHistory() {
        agent.clearHistory()
    }

    override fun close() {
        runCatching { agent.close() }
    }

    private fun newContext() {
        agent.close()
        historyAgents.add(agent)
        agent = BrowserPerceptiveAgent(session)
    }
}
