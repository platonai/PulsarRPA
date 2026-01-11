#!/bin/bash

# Simple Data Extraction Tests (override via BASE_URL / PRODUCT_URL)
BASE_URL="${BASE_URL:-http://localhost:8182}"
PRODUCT_URL="${PRODUCT_URL:-https://www.amazon.com/dp/B08PP5MSVB}"

CURL_DESC_SIMPLE_LOAD="Simple Page Load Test"
read -r -d '' CURL_CMD_SIMPLE_LOAD << EOF
curl -X POST "${BASE_URL}/api/x/e" -H "Content-Type: text/plain" -d "
select
  dom_base_uri(dom) as url,
  dom_first_text(dom, 'title') as page_title
from load_and_select('${PRODUCT_URL}', 'body');
"
EOF

CURL_DESC_HTML_PARSE="HTML Parsing Test"
read -r -d '' CURL_CMD_HTML_PARSE << EOF
curl -X POST "${BASE_URL}/api/x/e" -H "Content-Type: text/plain" -d "
  select
  dom_first_text(dom, 'h1') as heading,
  dom_all_texts(dom, 'p') as paragraphs
from load_and_select('${PRODUCT_URL}', 'body');
"
EOF

CURL_DESC_COMPLEX_XSQL="Complex X-SQL Query"
read -r -d '' CURL_CMD_COMPLEX_XSQL << EOF
curl -X POST "${BASE_URL}/api/x/e" -H "Content-Type: text/plain" -d "
select
  dom_first_text(dom, 'title') as page_title,
  dom_first_text(dom, 'h1,h2') as main_heading,
  dom_base_uri(dom) as base_url
from load_and_select('${PRODUCT_URL}', 'body');
"
EOF

CURL_DESC_FORM_DATA="Form Data Test"
read -r -d '' CURL_CMD_FORM_DATA << EOF
curl -X POST "${BASE_URL}/api/x/e" -H "Content-Type: text/plain" -d "
select dom_first_text(dom, 'title') as title from load_and_select('${PRODUCT_URL}', 'body');
"
EOF

# 系统测试优先的命令数组 (System tests first, then functional tests)
# shellcheck disable=SC2034
declare -a CURL_COMMANDS=(
  "$CURL_DESC_SIMPLE_LOAD|$CURL_CMD_SIMPLE_LOAD"
  "$CURL_DESC_HTML_PARSE|$CURL_CMD_HTML_PARSE"
  "$CURL_DESC_COMPLEX_XSQL|$CURL_CMD_COMPLEX_XSQL"
  "$CURL_DESC_FORM_DATA|$CURL_CMD_FORM_DATA"
)
