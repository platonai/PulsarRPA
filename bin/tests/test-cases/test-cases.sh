#!/bin/bash

# =============================================================================
# CURL COMMANDS FROM README.MD - UPDATE THIS SECTION AS NEEDED
# This file is sourced by run-e2e-tests.sh and should only declare commands.
# =============================================================================

# Defaults (override via environment)
BASE_URL="${BASE_URL:-http://localhost:8182}"
PRODUCT_URL="${PRODUCT_URL:-https://www.amazon.com/dp/B08PP5MSVB}"

# System Health Checks (Quick tests first)
CURL_DESC_HEALTH_CHECK="Health Check Endpoint"
CURL_CMD_HEALTH_CHECK="curl -X GET \"${BASE_URL}/actuator/health\""

CURL_DESC_QUERY_PARAMS="Query Parameters Test"
CURL_CMD_QUERY_PARAMS="curl -X GET \"${BASE_URL}/actuator/health?details=true\""

CURL_DESC_WEBUI="WebUI Command Interface"
read -r -d '' CURL_CMD_WEBUI << EOF
curl -X GET "${BASE_URL}/command.html"
EOF

CURL_DESC_CUSTOM_HEADERS="Custom Headers Test"
read -r -d '' CURL_CMD_CUSTOM_HEADERS << EOF
curl -X GET "${BASE_URL}/actuator/health" -H "Accept: application/json" -H "User-Agent: Browser4-Test-Suite/1.0"
EOF

# Advanced API Tests (Longer running)
CURL_DESC_PLAIN_API="Plain Text Command API - Amazon Product"
read -r -d '' CURL_CMD_PLAIN_API << EOF
curl -X POST "${BASE_URL}/api/commands/plain" -H "Content-Type: text/plain" -d "
Go to ${PRODUCT_URL}

After browser launch: clear browser cookies.
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
"
EOF

CURL_DESC_JSON_API="JSON Command API - Amazon Product"
read -r -d '' CURL_CMD_JSON_API << EOF
curl -X POST "${BASE_URL}/api/commands" -H "Content-Type: application/json" -d '{
  "url": "${PRODUCT_URL}",
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

CURL_DESC_ASYNC_MODE="Async Command Mode Test"
read -r -d '' CURL_CMD_ASYNC_MODE << EOF
curl -X POST "${BASE_URL}/api/commands/plain?mode=async" -H "Content-Type: text/plain" -d "
Go to ${PRODUCT_URL}

Extract the page title and all text content.
"
EOF

# shellcheck disable=SC2034
declare -a CURL_COMMANDS=(
  "$CURL_DESC_HEALTH_CHECK|$CURL_CMD_HEALTH_CHECK"
  "$CURL_DESC_QUERY_PARAMS|$CURL_CMD_QUERY_PARAMS"
  "$CURL_DESC_WEBUI|$CURL_CMD_WEBUI"
  "$CURL_DESC_CUSTOM_HEADERS|$CURL_CMD_CUSTOM_HEADERS"
  "$CURL_DESC_ASYNC_MODE|$CURL_CMD_ASYNC_MODE"
  "$CURL_DESC_PLAIN_API|$CURL_CMD_PLAIN_API"
  "$CURL_DESC_JSON_API|$CURL_CMD_JSON_API"
)
