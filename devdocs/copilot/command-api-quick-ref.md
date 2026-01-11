# Command API Quick Reference Card

> Ultra-compact reference for AI agents. For detailed guide see [command-api-guide.md](./command-api-guide.md)

## Endpoints

| Method | Endpoint | Input | Output |
|--------|----------|-------|--------|
| POST | `/api/commands` | JSON CommandRequest | CommandStatus |
| POST | `/api/commands/plain` | Plain text | CommandStatus |
| GET | `/api/commands/{id}/status` | - | CommandStatus |
| GET | `/api/commands/{id}/result` | - | CommandResult |

## CommandRequest JSON Template (Copy-Paste Ready)

```json
{
  "url": "https://example.com/page",
  "args": "-refresh -requireSize 50000",
  "onBrowserLaunchedActions": [
    "clear browser cookies"
  ],
  "onPageReadyActions": [
    "scroll down",
    "click #button"
  ],
  "pageSummaryPrompt": "Summarize the page content...",
  "dataExtractionRules": "field1, field2, field3",
  "uriExtractionRules": "links containing /pattern/",
  "async": false
}
```

## Minimal Required Fields

```json
{
  "url": "https://example.com"
}
```

## Field Reference

| Field | Type | Description |
|-------|------|-------------|
| `url` | String | **Required**. Target page URL |
| `args` | String | Load options (e.g., `-refresh -expires 1d`) |
| `onBrowserLaunchedActions` | List | Actions before navigation |
| `onPageReadyActions` | List | Actions after page loads |
| `pageSummaryPrompt` | String | LLM prompt for summarization |
| `dataExtractionRules` | String | Fields to extract |
| `uriExtractionRules` | String | URI pattern to find |
| `xsql` | String | X-SQL query |
| `async` | Boolean | Async execution (default: false) |

## Plain Text Format

```text
Visit [URL]
[Summarize the page.]
[Extract: field1, field2, field3.]
[Find all links containing /pattern/.]
[After page load: action1, action2.]
```

## Common Actions

### Browser Launch Actions
- `clear browser cookies`
- `navigate to [url]`
- `click [selector]`

### Page Ready Actions
- `scroll down` / `scroll to bottom`
- `click [selector]`
- `fill [selector] with [value]`
- `wait for [selector]`
- `hover over [selector]`

## Response Structure

```json
{
  "id": "uuid",
  "statusCode": 200,
  "status": "OK",
  "isDone": true,
  "pageStatusCode": 200,
  "commandResult": {
    "pageSummary": "...",
    "fields": {"key": "value"},
    "links": ["url1", "url2"]
  }
}
```

## Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created/Pending |
| 202 | Processing |
| 400 | Bad Request |
| 404 | Not Found |
| 417 | Failed |

## Common Patterns

### Product Extraction
```json
{
  "url": "https://www.amazon.com/dp/B08PP5MSVB",
  "dataExtractionRules": "product name, price, rating, review count",
  "uriExtractionRules": "links containing /dp/"
}
```

### Interactive Page
```json
{
  "url": "https://example.com/search",
  "args": "-refresh",
  "onPageReadyActions": [
    "fill #search with 'query'",
    "click #submit",
    "wait for .results"
  ],
  "dataExtractionRules": "result titles and URLs"
}
```

### Force Refresh
```json
{
  "url": "https://example.com",
  "args": "-refresh"
}
```

### Async with Polling
```json
{
  "url": "https://example.com",
  "async": true
}
```
Then poll: `GET /api/commands/{id}/status`

## cURL Examples

### JSON Command
```bash
curl -X POST "http://localhost:8182/api/commands" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com", "dataExtractionRules": "title, description"}'
```

### Plain Text Command
```bash
curl -X POST "http://localhost:8182/api/commands/plain" \
  -H "Content-Type: text/plain" \
  -d 'Visit https://example.com. Extract: title, description.'
```

### Async Command
```bash
curl -X POST "http://localhost:8182/api/commands?async=true" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com"}'
```

## Decision Tree

```
Has URL?
├─ Yes → Structured command
│   ├─ Need page actions? → Add onPageReadyActions
│   ├─ Need data? → Add dataExtractionRules
│   ├─ Need summary? → Add pageSummaryPrompt
│   ├─ Need links? → Add uriExtractionRules
│   └─ Long running? → Set async: true
└─ No → Agent command (autonomous execution)
```

## Constraints Checklist

- [ ] URL is valid HTTP/HTTPS
- [ ] Actions use specific CSS selectors
- [ ] Field names are clear and descriptive
- [ ] Only necessary fields included
- [ ] async=true for long operations

## Related Docs

- [Full Guide](./command-api-guide.md)
- [LoadOptions](./load-options-quick-ref.md)
- [PulsarSettings](./pulsar-settings-quick-ref.md)
- [REST API Examples](../../docs/rest-api-examples.md)

---

**Version**: 2026-01-11
