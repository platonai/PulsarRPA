package ai.platon.pulsar.rest.api.common

const val SYSTEM_PROMPT = """
You are PulsarRPA Assistant, an AI-powered web automation and data extraction specialist.
Your role is to help users automate browser interactions and extract structured data from web pages.
"""

const val DEFAULT_INTRODUCE = """
Hi, I am PulsarRPA! 

💖 PulsarRPA: The AI-Powered, Lightning-Fast Browser Automation Solution! 💖

✨ Key Capabilities:
* 🤖 AI Integration with LLMs – Smarter automation powered by large language models
* ⚡ Ultra-Fast Automation – Coroutine-safe browser automation with spider-level crawling performance
* 🧠 Web Understanding – Deep comprehension of dynamic web content
* 📊 Data Extraction APIs – Powerful tools to extract structured data effortlessly

What can I do for you today?
"""

// Placeholder constants
const val PLACEHOLDER_URL = "{PLACEHOLDER_URL}"
const val PLACEHOLDER_REQUEST_PLAIN_COMMAND_TEMPLATE = "{PLACEHOLDER_REQUEST_PLAIN_COMMAND_TEMPLATE}"
const val PLACEHOLDER_REQUEST_JSON_COMMAND_TEMPLATE = "{PLACEHOLDER_REQUEST_JSON_COMMAND_TEMPLATE}"
const val PLACEHOLDER_PAGE_CONTENT = "{PLACEHOLDER_PAGE_CONTENT}"
const val PLACEHOLDER_JSON_VALUE = "{PLACEHOLDER_JSON_VALUE}"
const val PLACEHOLDER_JSON_STRING = "{PLACEHOLDER_JSON_STRING}"
const val PLACEHOLDER_URI_DESCRIPTION = "{PLACEHOLDER_URI_DESCRIPTION}"

const val REQUEST_PLAIN_COMMAND_TEMPLATE = """
1. Go to https://example.com
2. When browser launches:
   a. Clear browser cookies
   b. Navigate to the home page
   c. Click a random link
3. When page is ready:
   a. Scroll to the middle of the page
   b. Scroll to the top of the page
4. Summarize the page content
5. Extract specified data fields
6. Collect matching URIs/links
7. X-SQL query: select dom_first_text(dom, 'title') as title from load_and_select(@url, ':root')
"""

const val REQUEST_JSON_COMMAND_TEMPLATE = """
{
  "url": "{PLACEHOLDER_URL}",
  "onBrowserLaunchedActions": [
    "clear browser cookies",
    "navigate to the home page", 
    "click a random link"
  ],
  "onPageReadyActions": [
    "scroll to the middle of the page",
    "scroll to the top of the page"
  ],
  "pageSummaryPrompt": "Instructions for summarizing the page...",
  "dataExtractionRules": "Instructions for extracting specific fields...",
  "uriExtractionRules": "Instructions for extracting URIs, for example: links containing /dp/",
  "xsql": "select dom_first_text(dom, 'title') as title from load_and_select(@url, ':root')"
}
"""

const val API_REQUEST_PLAIN_COMMAND_CONVERSION_PROMPT = """
Convert a natural language web automation request into a structured JSON configuration for automated browser interaction and data extraction.

### 🎯 Task:
Transform the user's conversational instructions into a precise JSON object that defines browser automation workflow, page interactions, and data extraction requirements.

### 📋 JSON Structure:
Use this template with only the fields relevant to the request:

```json
{PLACEHOLDER_REQUEST_JSON_COMMAND_TEMPLATE}
```

### 🔧 Field Guidelines:

**Required Fields:**
* `url`: Always preserve as `{PLACEHOLDER_URL}` - do not modify this placeholder

**Optional Fields (include only if relevant):**
* `onBrowserLaunchedActions`: Pre-navigation setup actions (cookies, authentication, etc.)
  - Execute in the specified order before loading the target page
  - Common actions: cookie management, login, navigation setup
  
* `onPageReadyActions`: Page interaction steps after initial load
  - Execute in the specified order once the page is fully loaded
  - Common actions: clicking, scrolling, form filling, waiting
  
* `pageSummaryPrompt`: Natural language instructions for page content summarization
  - Be specific about what aspects to focus on
  - Include desired output format or structure
  
* `dataExtractionRules`: Structured data extraction specifications
  - Define exact field names and expected formats
  - Specify selectors, patterns, or identification methods
  
* `uriExtractionRules`: Natural language instructions to extract URIs from the page
  - Will be converted into regex patterns automatically
  - Example: "extract all product links containing '/dp/'"

* `xsql`: X-SQL query to run on the page's DOM
    - Use for advanced data extraction or processing
    - Should be a valid X-SQL query string

### ⚡ Conversion Rules:
* **Specificity**: Convert vague instructions into concrete, executable actions
* **Order**: Sequence actions logically (setup → navigation → interaction → extraction)
* **Clarity**: Use precise language that eliminates ambiguity
* **Relevance**: Only include fields that address the user's actual requirements
* **Actionability**: Every instruction should be directly executable by automation

### 🌐 Language Support:
Process requests in any language and output JSON with English field values.

### 📥 User Request:

```text
{PLACEHOLDER_REQUEST_PLAIN_COMMAND_TEMPLATE}
```

### 📤 Expected Output:
Return only the JSON object - no explanations or additional text.
"""

const val CONVERT_RESPONSE_TO_MARKDOWN_PROMPT = """
Convert the following JSON string into a well-structured Markdown document.

### 🎯 Output Requirements:

The output must include:

#### 🧾 Human-Readable Explanation
Present the JSON content as structured Markdown sections:

1. **Page Summary** — A brief summary of the page content
2. **Extracted Data** — Key-value pairs extracted from the page (if any)
3. **Extracted Links** — List of URIs/links found on the page (if any)

#### 📦 JSON Representation
Include the original JSON string as a fenced code block at the end of the document.

### 🔧 JSON to Convert:

```json
{PLACEHOLDER_JSON_STRING}
```
"""

const val CONVERT_URI_DESCRIPTION_TO_REGEX_PROMPT = """
Convert the following URI description into a Kotlin-compatible regex pattern that matches exactly one valid URI.

### 🎯 Objective:
Generate a precise regex pattern that captures URIs based on the provided description.

### 🔧 Requirements:
* **Single Match**: Pattern must match exactly one complete URI (not partial matches or multiple URIs in text)
* **Kotlin Compatibility**: Must work with Kotlin's `Regex` class and standard regex engine
* **Exact Matching**: Pattern should precisely match the URI format described in the input
* **Complete URI**: Match the entire URI from start to end (use anchors if needed)

### 📋 Output Format:
* Start with the exact prefix: `Regex: `
* Follow with the regex pattern only
* No explanations, comments, or additional text
* Pattern should be ready to use in `Regex("your_pattern")`

### ⚠️ Important Notes:
* Escape special regex characters appropriately for Kotlin
* Consider URI components: scheme, authority, path, query, fragment
* Ensure the pattern is neither too restrictive nor too permissive
* Test mentally against common URI variations if applicable

### 📥 Input Description:

```text
{PLACEHOLDER_URI_DESCRIPTION}
```
"""

const val COMMAND_REVISION_TEMPLATE = """
Convert a JSON automation command into clear, numbered steps in plain language.

### 🎯 Task:
Transform the structured JSON command into human-readable instructions that explain what the automation will do.

### 🔧 Guidelines:
* Start with "Visit [url]"
* Convert each action in "onBrowserLaunchedActions" to numbered sub-steps under "When browser launches:"
* Convert each action in "onPageReadyActions" to numbered sub-steps under "When page is ready:"
* Add steps for page summarization, data extraction, and link collection if specified
* Use clear, concise numbered instructions
* Maintain logical action sequence

### 📋 Example Output Format:
```
{PLACEHOLDER_REQUEST_PLAIN_COMMAND_TEMPLATE}
```

### 📥 JSON Command to Convert:

```json
{PLACEHOLDER_JSON_VALUE}
```

### 📤 Expected Output:
Return only the numbered steps - no explanations or additional text.
"""
