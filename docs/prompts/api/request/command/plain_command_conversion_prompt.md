You're given a user request that describes how to interact with and extract information from a web page.
Your task is to **analyze and convert** this request into a **structured JSON object** that our system can process.

The input may be conversational, contain ambiguous instructions, or be written in different languages.

Produce a JSON object with these possible fields:

```json-text
{PLACEHOLDER_REQUEST_JSON_COMMAND_TEMPLATE}
```

### 🔧 Guidelines:

* **Keep the URL placeholder** as `{PLACEHOLDER_URL}` exactly as shown.
* Only include fields that are relevant to the user's request.
* For `onBrowserLaunchedActions`: List any pre-page load steps in order of execution.
* For `onPageReadyActions`: List any interaction steps in order of execution.
* For `pageSummaryPrompt`: Include clear instructions for summarizing the page content.
* For `dataExtractionRules`: Specify what fields to extract and their format.
* For `uriExtractionRules`: Define a Kotlin-compatible regex pattern that matches exactly one valid URI.
    * * Match the input's uri extraction requirement.
    * * Match only one full URI (not multiple in a string).
    * * Be compatible with Kotlin's Regex class.
    * * Support both HTTP and HTTPS schemes.
    * * Optionally include path, query, and fragment.
    * * Return value must start with a prefix "Regex: " and then provide the regex pattern, for example: `Regex: https?://[\\w.-]+(?:/[\\w.-]*)*`
    * * Return only the pattern string and the "Regex: " prefix (no explanation).

* Convert vague requests into specific, actionable instructions.

### 📥 Input:

```text
{PLACEHOLDER_REQUEST_PLAIN_COMMAND_TEMPLATE}
```
