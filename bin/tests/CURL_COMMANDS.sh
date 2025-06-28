#!/bin/bash

# =============================================================================
# CURL COMMANDS FROM README.MD - UPDATE THIS SECTION AS NEEDED
# =============================================================================

# System Health Checks (Quick tests first)
CURL_DESC_HEALTH_CHECK="Health Check Endpoint"
CURL_CMD_HEALTH_CHECK='curl -X GET "http://localhost:8182/actuator/health"'

CURL_DESC_QUERY_PARAMS="Query Parameters Test"
CURL_CMD_QUERY_PARAMS='curl -X GET "http://localhost:8182/actuator/health?details=true"'

CURL_DESC_WEBUI="WebUI Command Interface"
CURL_CMD_WEBUI='
curl -X GET "http://localhost:8182/command.html"
'

CURL_DESC_CUSTOM_HEADERS="Custom Headers Test"
read -r -d '' CURL_CMD_CUSTOM_HEADERS << 'EOF'
curl -X GET "http://localhost:8182/actuator/health" -H "Accept: application/json" -H "User-Agent: PulsarRPA-Test-Suite/1.0"
EOF

# Simple Data Extraction Tests
CURL_DESC_SIMPLE_LOAD="Simple Page Load Test"
read -r -d '' CURL_CMD_SIMPLE_LOAD << 'EOF'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_base_uri(dom) as url,
dom_first_text(dom, 'title') as page_title
from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
EOF

CURL_DESC_HTML_PARSE="HTML Parsing Test"
read -r -d '' CURL_CMD_HTML_PARSE << 'EOF'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, 'h1') as heading,
dom_all_texts(dom, 'p') as paragraphs
from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
EOF

CURL_DESC_COMPLEX_XSQL="Complex X-SQL Query"
read -r -d '' CURL_CMD_COMPLEX_XSQL << 'EOF'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, 'title') as page_title,
dom_first_text(dom, 'h1,h2') as main_heading,
dom_base_uri(dom) as base_url
from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
EOF

CURL_DESC_FORM_DATA="Form Data Test"
read -r -d '' CURL_CMD_FORM_DATA << 'EOF'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select dom_first_text(dom, 'title') as title from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
EOF

CURL_DESC_XSQL_LLM="X-SQL API - LLM Data Extraction"
read -r -d '' CURL_CMD_XSQL_LLM << 'EOF'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
dom_base_uri(dom) as url,
dom_first_text(dom, '#productTitle') as title,
dom_first_slim_html(dom, 'img:expr(width > 400)') as img
from load_and_select('https://www.amazon.com/dp/B0C1H26C46', 'body');
"
EOF

CURL_DESC_ASYNC_MODE="Async Command Mode Test"
read -r -d '' CURL_CMD_ASYNC_MODE << 'EOF'
curl -X POST "http://localhost:8182/api/commands/plain?mode=async" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

Extract the page title and all text content.
"
EOF












CURL_DESC_JSON_API="JSON Command API - Amazon Product"
read -r -d '' CURL_CMD_JSON_API << 'EOF'
curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '{
"url": "https://www.amazon.com/dp/B0C1H26C46",
"onBrowserLaunchedActions": [
  "clear browser cookies",
  "navigate to the home page",
  "click a random link"
],
"onPageReadyActions": ["click #title", "scroll to the middle"],
"pageSummaryPrompt": "Provide a brief introduction of this product.",
"dataExtractionRules": "product name, price, and ratings",
"uriExtractionRules": "all links containing /dp/ on the page"
}'
EOF

# Advanced API Tests (Longer running)
CURL_DESC_PLAIN_API="Plain Text Command API - Amazon Product"
read -r -d '' CURL_CMD_PLAIN_API << 'EOF'
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

After browser launch: clear browser cookies.
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
"
EOF



CURL_DESC_JSON_API_XSQL="JSON Command API with X-SQL - Amazon Product"
read -r -d '' CURL_CMD_JSON_API << 'EOF'
curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '{
"url": "https://www.amazon.com/dp/B0C1H26C46",
"onBrowserLaunchedActions": [
  "clear browser cookies",
  "navigate to the home page",
  "click a random link"
],
"onPageReadyActions": ["click #title", "scroll to the middle"],
"pageSummaryPrompt": "Provide a brief introduction of this product.",
"dataExtractionRules": "product name, price, and ratings",
"uriExtractionRules": "all links containing /dp/ on the page",
"xql": "select dom_first_text(dom, 'title') as title from load_and_select(@url, 'body');"
}'
EOF

# Advanced API Tests (Longer running)
CURL_DESC_PLAIN_API_XSQL="Plain Text Command API with X-SQL - Amazon Product"
read -r -d '' CURL_CMD_PLAIN_API << 'EOF'
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0C1H26C46

After browser launch: clear browser cookies.
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.

X-SQL:
```sql
select
llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
dom_base_uri(dom) as url,
dom_first_text(dom, '#productTitle') as title,
dom_first_slim_html(dom, 'img:expr(width > 400)') as img
from load_and_select(@url, 'body');
```
"
EOF






# Advanced API Tests (Longer running)
CURL_DESC_PLAIN_API_XSQL_2="Plain Text Command API with X-SQL 2 - Amazon Product"
read -r -d '' CURL_CMD_PLAIN_API << 'EOF'
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
1. Go to https://www.amazon.com/dp/B0C1H26C46
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
"
EOF



CURL_DESC_JSON_API_XSQL_2="JSON Command API with X-SQL 2 - Amazon Product"
read -r -d '' CURL_CMD_JSON_API << 'EOF'
curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '{
  "url": "https://www.amazon.com/dp/B0C1H26C46",
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
}'
EOF








# 系统测试优先的命令数组 (System tests first, then functional tests)
declare -a CURL_COMMANDS=(
"$CURL_DESC_HEALTH_CHECK|$CURL_CMD_HEALTH_CHECK"
"$CURL_DESC_QUERY_PARAMS|$CURL_CMD_QUERY_PARAMS"
"$CURL_DESC_WEBUI|$CURL_CMD_WEBUI"
"$CURL_DESC_CUSTOM_HEADERS|$CURL_CMD_CUSTOM_HEADERS"
"$CURL_DESC_SIMPLE_LOAD|$CURL_CMD_SIMPLE_LOAD"
"$CURL_DESC_HTML_PARSE|$CURL_CMD_HTML_PARSE"
"$CURL_DESC_COMPLEX_XSQL|$CURL_CMD_COMPLEX_XSQL"
"$CURL_DESC_FORM_DATA|$CURL_CMD_FORM_DATA"
"$CURL_DESC_XSQL_LLM|$CURL_CMD_XSQL_LLM"
"$CURL_DESC_ASYNC_MODE|$CURL_CMD_ASYNC_MODE"
"$CURL_DESC_PLAIN_API|$CURL_CMD_PLAIN_API"
"$CURL_DESC_JSON_API|$CURL_CMD_JSON_API"
"$CURL_DESC_JSON_API_XSQL|$CURL_CMD_JSON_API_XSQL"
"$CURL_DESC_PLAIN_API_XSQL|$CURL_CMD_PLAIN_API_XSQL"
"$CURL_DESC_PLAIN_API_XSQL_2|$CURL_CMD_PLAIN_API_XSQL_2"
"$CURL_DESC_JSON_API_XSQL_2|$CURL_CMD_JSON_API_XSQL_2"
)
