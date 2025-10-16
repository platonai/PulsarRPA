package ai.platon.pulsar.skeleton.ai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

// Kotlin equivalent of prompt.ts utilities

data class ChatMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: Any // String or List<ChatMessageImageContent | ChatMessageTextContent>
)

data class ChatMessageImageContent(
    val type: String,
    val image_url: ImageUrl? = null,
    val text: String? = null,
    val source: Source? = null,
) {
    data class ImageUrl(val url: String)
    data class Source(
        val type: String,
        val media_type: String,
        val data: String,
    )
}

data class ChatMessageTextContent(
    val type: String,
    val text: String,
)

fun buildUserInstructionsString(userProvidedInstructions: String?): String {
    if (userProvidedInstructions.isNullOrBlank()) return ""

    return """

# Custom Instructions Provided by the User

Please keep the user's instructions in mind when performing actions. If the user's instructions are not relevant to the current task, ignore them.

User Instructions:
$userProvidedInstructions
""".trimEnd()
}

// extract
fun buildExtractSystemPrompt(
    isUsingPrintExtractedDataTool: Boolean = false,
    userProvidedInstructions: String? = null,
): ChatMessage {
    val baseContent = """You are extracting content on behalf of a user.
  If a user asks you to extract a 'list' of information, or 'all' information,
  YOU MUST EXTRACT ALL OF THE INFORMATION THAT THE USER REQUESTS.

  You will be given:
1. An instruction
2. """

    val contentDetail = "A list of DOM elements to extract from."

    val instructions = """
Print the exact text from the DOM elements with all symbols, characters, and endlines as is.
Print null or an empty string if no new information is found.
  """.trim()

    val toolInstructions = if (isUsingPrintExtractedDataTool) {
        """
ONLY print the content using the print_extracted_data tool provided.
ONLY print the content using the print_extracted_data tool provided.
  """.trim()
    } else ""

    val additionalInstructions =
        "If a user is attempting to extract links or URLs, you MUST respond with ONLY the IDs of the link elements. \n" +
        "Do not attempt to extract links directly from the text unless absolutely necessary. "

    val userInstructions = buildUserInstructionsString(userProvidedInstructions)

    val contentRaw = buildString {
        append(baseContent)
        append(contentDetail)
        append("\n\n")
        append(instructions)
        if (toolInstructions.isNotBlank()) {
            append("\n")
            append(toolInstructions)
        }
        if (additionalInstructions.isNotBlank()) {
            append("\n\n")
            append(additionalInstructions)
        }
        if (userInstructions.isNotBlank()) {
            append("\n\n")
            append(userInstructions)
        }
    }

    val content = Regex("\\s+").replace(contentRaw, " ")

    return ChatMessage(
        role = "system",
        content = content,
    )
}

fun buildExtractUserPrompt(
    instruction: String,
    domElements: String,
    isUsingPrintExtractedDataTool: Boolean = false,
): ChatMessage {
    val sb = StringBuilder()
        .append("Instruction: ")
        .append(instruction)
        .append('\n')
        .append("DOM: ")
        .append(domElements)

    if (isUsingPrintExtractedDataTool) {
        sb.append('\n')
            .append("ONLY print the content using the print_extracted_data tool provided.")
            .append('\n')
            .append("ONLY print the content using the print_extracted_data tool provided.")
    }

    return ChatMessage(
        role = "user",
        content = sb.toString(),
    )
}

private val metadataSystemPrompt: String = """
You are an AI assistant tasked with evaluating the progress and completion status of an extraction task.
Analyze the extraction response and determine if the task is completed or if more information is needed.
Strictly abide by the following criteria:
1. Once the instruction has been satisfied by the current extraction response, ALWAYS set completion status to true and stop processing, regardless of remaining chunks.
2. Only set completion status to false if BOTH of these conditions are true:
   - The instruction has not been satisfied yet
   - There are still chunks left to process (chunksTotal > chunksSeen)
""".trimIndent()

fun buildMetadataSystemPrompt(): ChatMessage {
    return ChatMessage(
        role = "system",
        content = metadataSystemPrompt,
    )
}

fun buildMetadataPrompt(
    instruction: String,
    extractionResponse: Any,
    chunksSeen: Int,
    chunksTotal: Int,
): ChatMessage {
    val mapper = jacksonObjectMapper()
    val extractedJson = runCatching {
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(extractionResponse)
    }.getOrElse { _ ->
        // Fallback to toString if serialization fails
        extractionResponse.toString()
    }

    val content = """
Instruction: $instruction
Extracted content: $extractedJson
chunksSeen: $chunksSeen
chunksTotal: $chunksTotal
""".trim()

    return ChatMessage(
        role = "user",
        content = content,
    )
}

// observe
fun buildObserveSystemPrompt(
    userProvidedInstructions: String? = null,
): ChatMessage {
    val observeSystemPrompt = """
You are helping the user automate the browser by finding elements based on what the user wants to observe in the page.

You will be given:
1. a instruction of elements to observe
2. a hierarchical accessibility tree showing the semantic structure of the page. The tree is a hybrid of the DOM and the accessibility tree.

Return an array of elements that match the instruction if they exist, otherwise return an empty array.
""".trimIndent()

    val content = Regex("\\s+").replace(observeSystemPrompt, " ")

    val extra = buildUserInstructionsString(userProvidedInstructions)
    val merged = listOf(content, extra).filter { it.isNotBlank() }.joinToString("\n\n")

    return ChatMessage(
        role = "system",
        content = merged,
    )
}

fun buildObserveUserMessage(
    instruction: String,
    domElements: String,
): ChatMessage {
    val content = """
instruction: $instruction
Accessibility Tree:
$domElements
""".trim()

    return ChatMessage(
        role = "user",
        content = content,
    )
}

/**
 * Builds the instruction for the observeAct method to find the most relevant element for an action
 */
fun buildActObservePrompt(
    action: String,
    supportedActions: List<String>,
    variables: Map<String, String>? = null,
): String {
    // Base instruction
    var instruction = """
Find the most relevant element to perform an action on given the following action: $action.
  Provide an action for this element such as ${supportedActions.joinToString(", ")}, or any other playwright locator method.
  Remember that to users, buttons and links look the same in most cases.
  If the action is completely unrelated to a potential action to be taken on the page, return an empty array.
  ONLY return one action. If multiple actions are relevant, return the most relevant one.
  If the user is asking to scroll to a position on the page, e.g., 'halfway' or 0.75, etc, you must return the argument
  formatted as the correct percentage, e.g., '50%' or '75%', etc.
  If the user is asking to scroll to the next chunk/previous chunk, choose the nextChunk/prevChunk method. No arguments are required here.
  If the action implies a key press, e.g., 'press enter', 'press a', 'press space', etc., always choose the press method
  with the appropriate key as argument — e.g. 'a', 'Enter', 'Space'. Do not choose a click action on an on-screen keyboard.
  Capitalize the first character like 'Enter', 'Tab', 'Escape' only for special keys.
  If the action implies choosing an option from a dropdown, AND the corresponding element is a 'select' element,
  choose the selectOptionFromDropdown method. The argument should be the text of the option to select.
  If the action implies choosing an option from a dropdown, and the corresponding element is NOT a 'select' element, choose the click method.
""".trimIndent()

    // Add variable names (not values) to the instruction if any
    if (variables != null && variables.isNotEmpty()) {
        val variableNames = variables.keys.joinToString(", ") { "%$it%" }
        val variablesPrompt = """
The following variables are available to use in the action: $variableNames.
    Fill the argument variables with the variable name.
""".trimIndent()
        instruction += " $variablesPrompt"
    }

    return instruction
}

fun buildOperatorSystemPrompt(goal: String): ChatMessage {
    val content = """
You are a general-purpose agent whose job is to accomplish the user's goal across multiple model calls by running actions on the page.

You will be given a goal and a list of steps that have been taken so far. Your job is to determine if either the user's
goal has been completed or if there are still steps that need to be taken.

# Your current goal
$goal

# CRITICAL: You MUST use the provided tools to take actions. Do not just describe what you want to do - actually call the appropriate tools.

# Available tools and when to use them:
- `act`: Use this to interact with the page (click, type, navigate, etc.)
- `extract`: Use this to get information from the page
- `goto`: Use this to navigate to a specific URL
- `wait`: Use this to wait for a period of time
- `navback`: Use this to go back to the previous page
- `refresh`: Use this to refresh the current page
- `close`: Use this ONLY when the task is complete or cannot be achieved
- External tools: Use any additional tools (like search tools) as needed for your goal

# Important guidelines
1. ALWAYS use tools - never just provide text responses about what you plan to do
2. Break down complex actions into individual atomic steps
3. For `act` commands, use only one action at a time, such as:
   - Single click on a specific element
   - Type into a single input field
   - Select a single option
4. Avoid combining multiple actions in one instruction
5. If multiple actions are needed, they should be separate steps
6. Only use `close` when the task is genuinely complete or impossible to achieve
""".trimIndent()

    return ChatMessage(
        role = "system",
        content = content,
    )
}

