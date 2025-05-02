package ai.platon.pulsar.rest.api.common

const val SYSTEM_PROMPT = """
    
"""

const val DEFAULT_INTRODUCE = """
    I am PulsarRPA, what can I do for you?
"""

const val URL_PLACEHOLDER = "{URL_PLACEHOLDER}"

const val REQUEST_PLACEHOLDER = "{REQUEST_PLACEHOLDER}"

const val API_REQUEST_COMMAND_CONVERSION_TEMPLATE = """
Interpret the following spoken or loosely structured request and convert it into a well-formed JSON object 
that describes how to interact with a web page.

The input may contain informal, spoken, or multilingual instructions.

```text
{REQUEST_PLACEHOLDER}
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
