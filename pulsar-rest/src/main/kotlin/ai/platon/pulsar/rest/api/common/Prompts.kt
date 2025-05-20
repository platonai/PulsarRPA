package ai.platon.pulsar.rest.api.common

const val SYSTEM_PROMPT = """
    
"""

const val DEFAULT_INTRODUCE = """
    I am PulsarRPA, what can I do for you?
"""

const val PLACEHOLDER_URL = "{PLACEHOLDER_URL}"

const val PLACEHOLDER_REQUEST = "{PLACEHOLDER_REQUEST}"

const val PLACEHOLDER_PAGE_CONTENT = "{PLACEHOLDER_PAGE_CONTENT}"

const val PLACEHOLDER_JSON_VALUE = "{PLACEHOLDER_JSON_VALUE}"

const val COMMAND_REQUEST_TEMPLATE = """
{
  "url": "{PLACEHOLDER_URL}",
  "pageSummaryPrompt": "Instructions for summarizing the page...",
  "dataExtractionRules": "Instructions for extracting specific fields...",
  "linkExtractionRules": "https://.+",
  "onPageReadyActions": [
    "scroll down",
    "click 'Sign In' button"
  ]
}
"""

const val API_REQUEST_COMMAND_CONVERSION_TEMPLATE = """
You're given a user request that describes how to interact with and extract information from a web page.
Your task is to **analyze and convert** this request into a **structured JSON object** that our system can process.

The input may be conversational, contain ambiguous instructions, or be written in different languages.

Produce a JSON object with these possible fields:

```json
$COMMAND_REQUEST_TEMPLATE
```

### 🔧 Guidelines:

* **Keep the URL placeholder** as `{PLACEHOLDER_URL}` exactly as shown.
* Only include fields that are relevant to the user's request.
* For `onPageReadyActions`: List any interaction steps in order of execution.
* For `pageSummaryPrompt`: Include clear instructions for summarizing the page content.
* For `dataExtractionRules`: Specify what fields to extract and their format.
* For `linkExtractionRules`: Define a Kotlin-compatible regex pattern that matches exactly one valid URL.
  * * Match the input's link extraction requirement.
  * * Match only one full URL (not multiple in a string).
  * * Be compatible with Kotlin's Regex class.
  * * Support both HTTP and HTTPS schemes.
  * * Optionally include path, query, and fragment.
  * * Return only the pattern string (no explanation).
  
* Convert vague requests into specific, actionable instructions.

### 📥 Input:

```text
{PLACEHOLDER_REQUEST}
```

"""

const val JSON_STRING_PLACEHOLDER = "{JSON_STRING}"

const val CONVERT_RESPONSE_TO_MARKDOWN_PROMPT_TEMPLATE = """
Convert the following JSON string into a well-structured Markdown document.

## Output requirement

The output must include:

### 🧾 Human-Readable Explanation

Present the JSON content as structured Markdown sections:

1. **Page Summary** — A brief summary of the page content.
2. **Fields** — A list of extracted key-value pairs from the page.
3. **Links** — A list of extracted links.

### 📦 JSON representation

Include the original JSON string as a fenced code block **at the end** of the Markdown document.
The JSON section name is **JSON representation**

## 🔧 JSON to Convert:

```json
{JSON_STRING}

"""
