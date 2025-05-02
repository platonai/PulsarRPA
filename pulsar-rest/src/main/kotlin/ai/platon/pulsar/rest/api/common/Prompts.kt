package ai.platon.pulsar.rest.api.common

const val SYSTEM_PROMPT = """
    
"""

const val DEFAULT_INTRODUCE = """
    I am PulsarRPA, what can I do for you?
"""

const val PLACEHOLDER_URL = "{PLACEHOLDER_URL}"

const val PLACEHOLDER_REQUEST = "{PLACEHOLDER_REQUEST}"

const val PLACEHOLDER_PAGE_CONTENT = "{PLACEHOLDER_PAGE_CONTENT}"

const val API_REQUEST_COMMAND_CONVERSION_TEMPLATE = """
Interpret the following spoken or loosely structured request and convert it into a well-formed JSON object 
that describes how to interact with a web page.

The input may contain informal, spoken, or multilingual instructions.

```text
{PLACEHOLDER_REQUEST}
```

Use the following JSON format:

```json
{
  "url": "https://keep-me-unchanged.com",           // Keep the URL unchanged, the programmer will handle it
  "pageSummaryPrompt": "Summarize or analyze...",   // (Optional) Natural-language prompt about the page content
  "dataExtractionRules": "Extract fields like...",  // (Optional) What structured information to extract from the page
  "linkExtractionRules": "Extract links like...",   // (Optional) What links to extract from the page
  "onPageReadyActions": [                           // (Optional) Actions to perform after page load in the browser
    "scroll down",
    "click 'Sign In' button"
  ]
}
```

Notes:

- Handle vague or partial commands gracefully.
- Normalize non-English input where possible.
- If some fields are not mentioned, leave them out or set them to null.

"""

const val JSON_STRING_PLACEHOLDER = "{JSON_STRING}"

const val CONVERT_RESPONSE_TO_MARKDOWN_PROMPT_TEMPLATE = """
Convert the following JSON string into a well-structured Markdown document.

## 🔧 JSON to Convert:

```json
{JSON_STRING}

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

"""
