#!/bin/bash
#
# Test script for extract_response_brief.sh
# This script generates various test responses and verifies the extraction functionality
#

set -e  # Exit on error

# Determine script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_DIR="$PROJECT_ROOT/script-tests/test-responses"
SOURCE_SCRIPT="$PROJECT_ROOT/tests/extract_response_brief.sh"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Create test directory
mkdir -p "$TEST_DIR"

# Source the extract_response_brief function
if [[ -f "$SOURCE_SCRIPT" ]]; then
  source "$SOURCE_SCRIPT"
else
  echo -e "${RED}Error: extract_response_brief.sh not found at $SOURCE_SCRIPT${NC}"
  exit 1
fi

# Test counter
TEST_COUNT=0
PASS_COUNT=0
FAIL_COUNT=0

# Test function
run_test() {
  local test_name="$1"
  local file_content="$2"
  local content_type="$3"
  local expected_brief="$4"
  local file_path="$TEST_DIR/test_${TEST_COUNT}.txt"

  TEST_COUNT=$((TEST_COUNT + 1))

  echo -e "\n${BLUE}[TEST $TEST_COUNT]${NC} $test_name"
  echo -e "${CYAN}Content Type:${NC} $content_type"

  # Create test file
  echo -e "$file_content" > "$file_path"
  local file_size=$(wc -c < "$file_path")

  # Run extraction
  local actual_brief=$(extract_response_brief "$file_path" "$content_type" "$file_size")

  echo -e "${CYAN}Expected Brief:${NC} $expected_brief"
  echo -e "${CYAN}Actual Brief:${NC} $actual_brief"

  if [[ "$actual_brief" == "$expected_brief" ]]; then
    echo -e "${GREEN}✅ PASS${NC}"
    PASS_COUNT=$((PASS_COUNT + 1))
  else
    echo -e "${RED}❌ FAIL${NC}"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
}

# -----------------------------------------------------------
# TEST CASES
# -----------------------------------------------------------

# Test 1: Simple JSON object
run_test "Simple JSON Object" \
'{"status":"UP","components":{"db":{"status":"UP"}}}' \
"application/json" \
"status, components → UP"

# Test 2: Complex JSON object
run_test "Complex JSON Object" \
'{
  "data": {
    "product": {
      "name": "PulsarRPA Pro Edition",
      "price": "$299.99",
      "version": "3.0.12",
      "features": ["Web Scraping", "RPA", "Browser Automation"]
    },
    "ratings": {
      "average": 4.8,
      "count": 253
    }
  },
  "status": "success"
}' \
"application/json" \
"data, status → success"

# Test 3: JSON Array
run_test "JSON Array" \
'[
  {"id": 1, "name": "Item 1"},
  {"id": 2, "name": "Item 2"},
  {"id": 3, "name": "Item 3"}
]' \
"application/json" \
"Array with 3 item(s)"

# Test 4: Simple HTML
run_test "Simple HTML" \
'<!DOCTYPE html>
<html>
<head>
  <title>Test Page Title</title>
</head>
<body>
  <h1>Main Heading</h1>
  <p>This is a test paragraph.</p>
</body>
</html>' \
"text/html" \
"Test Page Title"

# Test 5: HTML without title but with h1
run_test "HTML without title" \
'<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
</head>
<body>
  <h1>Main Heading Only</h1>
  <p>This is a test paragraph.</p>
</body>
</html>' \
"text/html" \
"Main Heading Only"

# Test 6: HTML without title or h1
run_test "HTML without title or h1" \
'<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
</head>
<body>
  <p>This is just a test paragraph.</p>
</body>
</html>' \
"text/html" \
"HTML content (no title/heading found)"

# Test 7: Plain text
run_test "Plain Text" \
'This is the first line of plain text.
This is the second line that should not appear in the brief.' \
"text/plain" \
"This is the first line of plain text."

# Test 8: Empty response
run_test "Empty Response" \
"" \
"text/plain" \
"Empty response"

# Test 9: Very long text that should be truncated
long_text=$(printf '%.0s-' {1..200})
run_test "Long Text Truncation" \
"Very long text that exceeds the maximum length: $long_text" \
"text/plain" \
"Very long text that exceeds the maximum length: --------------------------------------------------------------..."

# Test 10: Error response JSON
run_test "Error Response JSON" \
'{"error":"Not Found","message":"The requested resource was not found","status":404,"path":"/api/missing"}' \
"application/json" \
"error, message, status, path → Not Found"

# Test 11: XML response (treated as text)
run_test "XML Response" \
'<?xml version="1.0" encoding="UTF-8"?>
<response>
  <status>success</status>
  <data>
    <item id="1">First item</item>
    <item id="2">Second item</item>
  </data>
</response>' \
"application/xml" \
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

# Test 12: CSV data (treated as text)
run_test "CSV Data" \
'id,name,category,price
1,Product A,Electronics,199.99
2,Product B,Books,29.99
3,Product C,Home,59.99' \
"text/csv" \
"id,name,category,price"

# Test 13: JSON with null values
run_test "JSON with Null Values" \
'{"name":null,"description":null,"price":"$19.99","available":true}' \
"application/json" \
"name, description, price, available → \$19.99"

# -----------------------------------------------------------
# Print Summary
# -----------------------------------------------------------

echo -e "\n${BLUE}================================================${NC}"
echo -e "${BLUE}             TEST SUMMARY                       ${NC}"
echo -e "${BLUE}================================================${NC}"
echo -e "Total Tests: $TEST_COUNT"
echo -e "${GREEN}Passed:     $PASS_COUNT${NC}"
echo -e "${RED}Failed:     $FAIL_COUNT${NC}"
echo -e "${BLUE}================================================${NC}"

# Clean up test files
rm -rf "$TEST_DIR"

if [ $FAIL_COUNT -eq 0 ]; then
  echo -e "${GREEN}All tests passed!${NC}"
  exit 0
else
  echo -e "${RED}Some tests failed. Please check the output above.${NC}"
  exit 1
fi
