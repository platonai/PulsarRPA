# Command API - AI-Friendly Reference Guide

## Overview

The Command API provides endpoints for AI systems to execute web automation tasks through Browser4. It supports two input modes:

1. **Structured JSON Commands** - Strongly typed requests with explicit fields
2. **Plain Text Commands** - Natural language instructions converted to structured commands

This guide helps AI agents understand how to generate high-quality, strongly-constrained commands that Browser4 can accurately interpret and execute.

**Primary Entry Point**: `ai.platon.pulsar.rest.api.controller.CommandController`

---

## API Endpoints

### Base URL
```
http://localhost:8182/api/commands
```

### Available Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/commands` | Execute structured JSON command |
| POST | `/api/commands/plain` | Execute plain text command |
| GET | `/api/commands/{id}/status` | Get command execution status |
| GET | `/api/commands/{id}/result` | Get command execution result |
| GET | `/api/commands/{id}/stream` | Stream command status via SSE |

---

## Execution Modes

### Synchronous Execution (Default)
```bash
# JSON command
curl -X POST "http://localhost:8182/api/commands" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com"}'

# Plain text command
curl -X POST "http://localhost:8182/api/commands/plain" \
  -H "Content-Type: text/plain" \
  -d 'Visit https://example.com and summarize the page.'
```

### Asynchronous Execution
```bash
# JSON command with async flag
curl -X POST "http://localhost:8182/api/commands" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com", "async": true}'

# Plain text command with async parameter
curl -X POST "http://localhost:8182/api/commands/plain?async=true" \
  -H "Content-Type: text/plain" \
  -d 'Visit https://example.com and summarize the page.'
```

---

## Data Structures

### CommandRequest

The primary input structure for JSON-based commands.

**Location**: `ai.platon.pulsar.rest.api.entities.CommandRequest`

```kotlin
data class CommandRequest(
    var url: String,                           // Required: Target page URL
    var args: String? = null,                  // Optional: Load options (CLI style)
    var onBrowserLaunchedActions: List<String>? = null,  // Actions when browser launches
    var onPageReadyActions: List<String>? = null,        // Actions when page is ready
    var pageSummaryPrompt: String? = null,     // Prompt for page summarization
    var dataExtractionRules: String? = null,   // Data extraction specifications
    var uriExtractionRules: String? = null,    // URI extraction rules (regex/description)
    var xsql: String? = null,                  // X-SQL query for advanced extraction
    var richText: Boolean? = null,             // Whether to retain rich text formatting
    var async: Boolean? = null,                // Async execution flag
    var id: String? = null                     // Optional command ID
)
```

#### Field Details

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `url` | String | **Yes** | Target page URL. Must be a valid HTTP/HTTPS URL. |
| `args` | String | No | Load options in CLI style (e.g., `-expires 1d -refresh`). See [LoadOptions Guide](./load-options-guide.md). |
| `onBrowserLaunchedActions` | List<String> | No | Actions executed when browser launches, before navigating to URL. |
| `onPageReadyActions` | List<String> | No | Actions executed after page content is fully loaded. |
| `pageSummaryPrompt` | String | No | Natural language prompt for LLM to summarize page content. |
| `dataExtractionRules` | String | No | Specifications for extracting structured fields from page. |
| `uriExtractionRules` | String | No | Pattern or description for extracting URIs from page. |
| `xsql` | String | No | X-SQL query for advanced DOM-based data extraction. |
| `richText` | Boolean | No | If true, retains formatting in extracted text content. |
| `async` | Boolean | No | If true, executes command asynchronously and returns ID. |
| `id` | String | No | Custom identifier for the command (auto-generated if not provided). |

---

### CommandStatus

Tracks the execution state of a command.

**Location**: `ai.platon.pulsar.rest.api.entities.CommandStatus`

```kotlin
data class CommandStatus(
    val id: String,                            // Unique command identifier
    var statusCode: Int,                       // HTTP-like status code
    var event: String,                         // Last event description
    var isDone: Boolean,                       // Whether execution is complete
    var pageStatusCode: Int,                   // Page fetch status code
    var pageContentBytes: Int,                 // Page content size in bytes
    var message: String?,                      // Additional information
    var request: CommandRequest?,              // Original request (if stored)
    var commandResult: CommandResult?,         // Execution result
    var instructResults: MutableList<InstructResult>  // Individual instruction results
)
```

#### Status Codes

| Code | Name | Description |
|------|------|-------------|
| 200 | OK | Command completed successfully |
| 201 | CREATED | Command created, not yet executed |
| 202 | PROCESSING | Command is currently executing |
| 400 | BAD_REQUEST | Invalid input (e.g., blank command) |
| 404 | NOT_FOUND | Command ID not found |
| 417 | EXPECTATION_FAILED | Command execution failed |

---

### CommandResult

Contains the output data from command execution.

**Location**: `ai.platon.pulsar.rest.api.entities.CommandResult`

```kotlin
data class CommandResult(
    var summary: String?,                      // Overall summary (for agent commands)
    var pageSummary: String?,                  // LLM-generated page summary
    var fields: Map<String, String>?,          // Extracted data fields
    var links: List<String>?,                  // Extracted URIs/links
    var xsqlResultSet: List<Map<String, Any?>>?  // X-SQL query results
)
```

---

## Command Types

### 1. URL-Based Commands (Structured)

Commands with a valid URL are processed through the standard command execution flow:

```
CommandService.executeCommand(request, status, eventHandlers)
```

#### Flow:
1. Parse and validate CommandRequest
2. Load page with specified options
3. Execute page actions (if specified)
4. Execute LLM prompts for summarization/extraction
5. Extract URIs using specified rules
6. Execute X-SQL queries (if specified)
7. Return CommandStatus with results

### 2. Agent Commands (Open-Ended)

When a plain text command cannot be converted to a structured CommandRequest (no URL or complex instructions), it's executed by the companion agent:

```
CommandService.executeAgentCommand(plainCommand)
```

#### Flow:
1. Receive plain text instruction
2. Pass to `session.companionAgent.run(plainCommand)`
3. Agent autonomously decides actions (navigate, interact, extract)
4. Return final agent state as CommandStatus

---

## Plain Text Command Conversion

Plain text commands are normalized to JSON via LLM:

```
ConversationService.normalizePlainCommand(plainCommand) -> CommandRequest?
```

### Conversion Process:
1. Extract URLs from plain text
2. If no URL found, route to agent command
3. Use LLM to convert plain text to JSON structure
4. Parse JSON into CommandRequest
5. Execute as structured command

### Supported Plain Text Patterns:

```text
Visit https://www.amazon.com/dp/B08PP5MSVB
Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
After page load: click #title, then scroll to the middle.
```

Converts to:

```json
{
  "url": "https://www.amazon.com/dp/B08PP5MSVB",
  "pageSummaryPrompt": "Summarize the product.",
  "dataExtractionRules": "product name, price, ratings",
  "uriExtractionRules": "all links containing /dp/",
  "onPageReadyActions": ["click #title", "scroll to the middle"]
}
```

---

## Action Specifications

### onBrowserLaunchedActions

Actions executed before navigating to the target URL. Use for browser setup tasks.

**Common Actions:**
- `"clear browser cookies"` - Clear all cookies
- `"navigate to [url]"` - Navigate to a different URL first
- `"click [selector]"` - Click an element
- `"wait [duration]"` - Wait for specified time

**Example:**
```json
{
  "onBrowserLaunchedActions": [
    "clear browser cookies",
    "navigate to the home page",
    "click a random link"
  ]
}
```

### onPageReadyActions

Actions executed after the page is fully loaded. Use for page interaction.

**Common Actions:**
- `"scroll down"` / `"scroll to bottom"` - Scroll page
- `"scroll to [element]"` - Scroll to specific element
- `"click [selector]"` - Click an element
- `"fill [selector] with [value]"` - Fill form field
- `"wait for [selector]"` - Wait for element to appear
- `"hover over [selector]"` - Hover over element

**Example:**
```json
{
  "onPageReadyActions": [
    "scroll down",
    "click #load-more",
    "wait for .results"
  ]
}
```

---

## Data Extraction Specifications

### pageSummaryPrompt

Natural language instructions for page summarization.

**Guidelines:**
- Be specific about focus areas
- Specify output format if needed
- Keep prompts concise but clear

**Example:**
```json
{
  "pageSummaryPrompt": "Provide a brief product overview including key features, target audience, and competitive advantages."
}
```

### dataExtractionRules

Specifications for extracting structured fields.

**Guidelines:**
- List exact field names to extract
- Specify expected format for each field
- Can include extraction hints

**Example:**
```json
{
  "dataExtractionRules": "product name, price (with currency), rating (out of 5), number of reviews"
}
```

### uriExtractionRules

Rules for extracting URIs from the page.

**Formats:**
1. **Natural Language**: `"all links containing /dp/"` - Converted to regex
2. **Regex Pattern**: `"Regex: .*/dp/[A-Z0-9]+.*"` - Used directly

**Example:**
```json
{
  "uriExtractionRules": "links containing /dp/ or /product/"
}
```

### xsql

X-SQL query for DOM-based extraction.

**Example:**
```json
{
  "xsql": "select dom_first_text(dom, '#productTitle') as title, dom_first_text(dom, '#priceblock_ourprice') as price from load_and_select(@url, 'body')"
}
```

---

## Constraints for AI-Generated Commands

When generating commands for Browser4, AI agents should follow these constraints:

### URL Constraints
1. **Valid Format**: URLs must be standard HTTP/HTTPS URLs
2. **Accessible**: URLs should be publicly accessible (unless auth provided)
3. **Specific**: Prefer direct page URLs over navigation sequences

### Action Constraints
1. **Specificity**: Use CSS selectors or clear element descriptions
2. **Order**: Sequence actions logically
3. **Timing**: Include waits if dynamic content expected
4. **Idempotency**: Actions should be repeatable without side effects

### Extraction Constraints
1. **Field Names**: Use clear, descriptive field names
2. **Format Hints**: Specify expected data formats
3. **Fallbacks**: Consider missing data scenarios
4. **Scope**: Limit extraction to visible/accessible content

### Performance Constraints
1. **Minimal Actions**: Only include necessary actions
2. **Efficient Selectors**: Use specific CSS selectors
3. **Reasonable Timeouts**: Don't exceed page load limits
4. **Resource Awareness**: Consider page complexity

---

## Complete Examples

### Example 1: Product Data Extraction

**Plain Text:**
```text
Visit https://www.amazon.com/dp/B08PP5MSVB
Summarize the product.
Extract: product name, price, ratings, review count.
Find all similar product links.
```

**JSON:**
```json
{
  "url": "https://www.amazon.com/dp/B08PP5MSVB",
  "pageSummaryPrompt": "Summarize this product including key features and specifications.",
  "dataExtractionRules": "product name, price (with currency symbol), star rating (out of 5), total review count",
  "uriExtractionRules": "links containing /dp/ that are similar or related products"
}
```

**cURL:**
```bash
curl -X POST "http://localhost:8182/api/commands" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.amazon.com/dp/B08PP5MSVB",
    "pageSummaryPrompt": "Summarize this product including key features and specifications.",
    "dataExtractionRules": "product name, price (with currency symbol), star rating (out of 5), total review count",
    "uriExtractionRules": "links containing /dp/"
  }'
```

### Example 2: Interactive Page with Actions

**JSON:**
```json
{
  "url": "https://www.example.com/search",
  "args": "-refresh -requireSize 50000",
  "onBrowserLaunchedActions": [
    "clear browser cookies"
  ],
  "onPageReadyActions": [
    "fill #search-input with 'browser automation'",
    "click #search-button",
    "wait for .search-results",
    "scroll down",
    "scroll down"
  ],
  "dataExtractionRules": "result titles, result descriptions, result URLs",
  "async": true
}
```

### Example 3: Agent Command (Open-Ended)

**Plain Text (no URL):**
```text
Search for the latest news about AI browser automation.
Visit the top 3 results.
Summarize the key points from each article.
Create a combined summary of trends.
```

This will be executed as an agent command since no specific URL is provided.

---

## Response Format

### Synchronous Response

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "statusCode": 200,
  "status": "OK",
  "event": "pageSummary,fields,links",
  "isDone": true,
  "pageStatusCode": 200,
  "pageStatus": "OK",
  "pageContentBytes": 125000,
  "message": "created,textContent,pageSummary,fields,links",
  "commandResult": {
    "pageSummary": "This is an Amazon product page for...",
    "fields": {
      "product name": "Example Product",
      "price": "$29.99",
      "ratings": "4.5"
    },
    "links": [
      "https://www.amazon.com/dp/B08ABC123",
      "https://www.amazon.com/dp/B08DEF456"
    ]
  },
  "createTime": "2024-01-05T10:30:00Z",
  "lastModifiedTime": "2024-01-05T10:30:15Z",
  "finishTime": "2024-01-05T10:30:15Z"
}
```

### Asynchronous Response

Initial response (status ID):
```
"550e8400-e29b-41d4-a716-446655440000"
```

Poll for status:
```bash
curl "http://localhost:8182/api/commands/550e8400-e29b-41d4-a716-446655440000/status"
```

---

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| 400 BAD_REQUEST | Blank or invalid command | Provide valid URL or command text |
| 404 NOT_FOUND | Command ID not found | Verify command ID is correct |
| 417 EXPECTATION_FAILED | Page load or extraction failed | Check URL accessibility, adjust options |

### Validation

The system validates:
1. URL format (must be standard HTTP/HTTPS)
2. Command not blank
3. Page successfully loaded before extraction

---

## Related Documentation

- [LoadOptions Guide](./load-options-guide.md) - URL-level configuration options
- [LoadOptions Quick Reference](./load-options-quick-ref.md) - Quick option reference
- [PageEventHandlers](./page-event-handlers.md) - Event handling during page lifecycle
- [PulsarSettings Guide](./pulsar-settings-guide.md) - Global browser configuration
- [REST API Examples](../../docs/rest-api-examples.md) - Additional API examples
- [X-SQL Documentation](../../docs/x-sql.md) - X-SQL query reference

---

## Best Practices for AI Agents

### Generating Structured Commands

1. **Prefer JSON over Plain Text** when possible for deterministic behavior
2. **Include args** for page load optimization (e.g., `-refresh`, `-requireSize`)
3. **Specify actions explicitly** with CSS selectors
4. **Use structured extraction rules** with clear field names
5. **Set async=true** for long-running commands

### Generating Plain Text Commands

1. **Include URL first** to enable structured conversion
2. **Use clear action verbs**: Visit, Click, Scroll, Extract, Summarize
3. **Separate concerns**: Navigation → Actions → Extraction
4. **Be specific** about expected data fields
5. **Use "After page load:"** prefix for onPageReadyActions

### Handling Results

1. **Check isDone** before processing results
2. **Verify statusCode** is 200 for success
3. **Handle partial results** (some fields may be null)
4. **Retry with adjustments** if extraction fails

---

**Version**: 2026-01-11  
**Maintainer**: Browser4 Team  
**Status**: Living Document - Update as Command API evolves
