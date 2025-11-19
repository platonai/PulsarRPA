package ai.platon.pulsar.agentic.ai.todo

import ai.platon.pulsar.agentic.AgentConfig
import ai.platon.pulsar.agentic.ai.agent.detail.StructuredAgentLogger
import ai.platon.pulsar.agentic.common.AgentFileSystem
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.skeleton.ai.ObserveElement
import ai.platon.pulsar.skeleton.ai.ToolCall
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/**
 * Manage writing and updating todolist.md for an agent session.
 * This is a thin wrapper over FileSystem with minimal policy from AgentConfig.
 */
class ToDoManager(
    private val fs: AgentFileSystem,
    private val config: AgentConfig,
    private val uuid: UUID,
    private val slogger: StructuredAgentLogger? = null,
) {
    /** Initialize todolist.md with header and sections if it's empty. */
    suspend fun primeIfEmpty(instruction: String, url: String?) {
        val content = fs.getTodoContents()
        if (content.isNotBlank()) return
        val shortId = uuid.toString().take(8)
        val now = Instant.now().toString()
        val header = buildString {
            appendLine("# TODO for session $shortId")
            appendLine("Instruction: ${Strings.compactInline(instruction, 200)}")
            appendLine("Started at: $now")
            appendLine("Current URL: ${url ?: "(unknown)"}")
            appendLine("Progress: (0/∞)")
            appendLine()
            appendLine("## Plan")
            if (config.todoPlanWithLLM) {
                appendLine("- [ ] Step 1: TBD  #action:navigateTo")
                appendLine("- [ ] Step 2: TBD  #action:click")
            } else {
                appendLine("(Plan TBD — 将在执行过程中逐步完善，并通过标签进行勾选)")
            }
            appendLine()
            appendLine("## Progress Log")
            appendLine()
            appendLine("## Notes")
        }
        runCatching { fs.writeString("todolist.md", header) }
            .onFailure { e -> slogger?.logError("todo.prime.write.fail", e, uuid.toString()) }
    }

    /** Append one OK progress line and enforce max lines cap. */
    suspend fun appendProgress(
        step: Int,
        toolCall: ToolCall?,
        observe: ObserveElement?,
        url: String,
        summary: String?,
    ): Boolean {
        val time = LocalDateTime.now().toLocalTime().toString().take(8)
        val method = toolCall?.method ?: "(unknown)"
        val selector = selectorSnippet(observe)
        val summaryText = Strings.compactInline(summary ?: "", 120)
        val line = "- [OK] $time $method \"${selector}\" @ ${url} | ${summaryText}\n"

        val existing = fs.getTodoContents()
        // Count any occurrence of the OK marker (don't rely on a previous newline)
        val okLines = Regex("""- \[OK\]""").findAll(existing).count()
        if (okLines >= config.todoMaxProgressLines) return false

        val updated = if (existing.endsWith("\n")) existing + line else existing + "\n" + line
        return runCatching {
            fs.writeString("todolist.md", updated)
            true
        }.onFailure { e ->
            slogger?.logError("todo.progress.write.fail", e, uuid.toString())
        }.getOrElse { false }
    }

    /** Increment the Progress counter in the header if present. */
    suspend fun updateProgressCounter() {
        val content = fs.getTodoContents()
        val regex = Regex("""Progress:\s*\((\d+)/(∞|\d+)\)""")
        val m = regex.find(content) ?: return
        val cur = m.groupValues[1].toIntOrNull() ?: return
        val den = m.groupValues[2]
        val newFrag = "Progress: (${cur + 1}/${den})"
        val updated = content.replaceRange(m.range, newFrag)
        runCatching { fs.writeString("todolist.md", updated) }
            .onFailure { e -> slogger?.logError("todo.progress.counter.fail", e, uuid.toString()) }
    }

    /** Try to check off one plan item whose line contains any of tags. */
    suspend fun markPlanItemDoneByTags(tags: Set<String>) {
        if (tags.isEmpty()) return
        val content = fs.getTodoContents()
        val lines = content.split("\n").toMutableList()
        var modified = false
        for (i in lines.indices) {
            val line = lines[i]
            val lineLower = line.lowercase()
            // match unchecked plan items and do a case-insensitive tag search
            if (line.trimStart().startsWith("- [ ]") && tags.any { tag -> lineLower.contains(tag.lowercase()) }) {
                lines[i] = line.replaceFirst("- [ ]", "- [x]")
                modified = true
                break
            }
        }
        if (modified) {
            runCatching { fs.writeString("todolist.md", lines.joinToString("\n")) }
                .onFailure { e -> slogger?.logError("todo.plan.check.fail", e, uuid.toString()) }
        }
    }

    fun buildTags(toolCall: ToolCall?, url: String?): Set<String> {
        if (toolCall == null) return emptySet()
        val tags = mutableSetOf<String>()
        val method = toolCall.method.trim().lowercase()
        if (method.isNotBlank()) tags.add("#action:$method")
        // Try to extract host robustly. If the URL lacks a scheme, attempt to parse with http:// added.
        val host = runCatching {
            val uri = try {
                java.net.URI(url ?: "")
            } catch (e: Exception) {
                java.net.URI("http://${url ?: ""}")
            }
            uri.host
        }.getOrNull()?.lowercase()
        if (!host.isNullOrBlank()) tags.add("#domain:$host")
        return tags
    }

    suspend fun onTaskCompletion(instruction: String) {
        val ts = LocalDateTime.now().toLocalTime().toString().take(8)
        val line = "- [OK] $ts task.complete | $instruction\n"
        val current = fs.getTodoContents()
        val updated = if (current.endsWith("\n")) current + line else current + "\n" + line
        runCatching { fs.writeString("todolist.md", updated) }
            .onFailure { e -> slogger?.logError("todo.complete.write.fail", e, uuid.toString()) }
        updateProgressCounter()
    }

    private fun selectorSnippet(observe: ObserveElement?): String {
        if (observe == null) return ""
        val s = observe.cssSelector
            ?: observe.locator
            ?: observe.backendNodeId?.let { "backend:$it" }
            ?: ""
        return s.take(80)
    }
}
