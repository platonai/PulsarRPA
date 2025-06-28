#!/bin/bash

# Test script for curl commands from README.md
# All curl commands are explicitly listed below for easy maintenance
# Author: Auto-generated for platonai
# Date: 2025-06-11 17:29:59

# set -e  # Exit on error

readonly DEFAULT_BASE_URL="http://localhost:8182"
readonly TEST_RESULTS_DIR="./target/test-results"
readonly TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"
readonly LOG_FILE="${TEST_RESULTS_DIR}/curl_tests_${TIMESTAMP}.log"

# Default options
PULSAR_BASE_URL="$DEFAULT_BASE_URL"
TIMEOUT_SECONDS=120
FAST_MODE=false
SKIP_SERVER_CHECK=false
VERBOSE_MODE=true
USER_NAME="platonai"

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
"$CURL_DESC_ASYNC_MODE|$CURL_CMD_ASYNC_MODE"
"$CURL_DESC_PLAIN_API|$CURL_CMD_PLAIN_API"
"$CURL_DESC_JSON_API|$CURL_CMD_JSON_API"
"$CURL_DESC_XSQL_LLM|$CURL_CMD_XSQL_LLM"
)

# =============================================================================
# SECTION: GLOBAL CONFIGURATION AND INITIALIZATION
# =============================================================================

# Colors
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly BOLD='\033[1m'
readonly NC='\033[0m'

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Ensure results directory exists
mkdir -p "$TEST_RESULTS_DIR"

# =============================================================================
# SECTION: UTILITY FUNCTIONS
# =============================================================================

log() {
  local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
  echo -e "[$timestamp] $1" | tee -a "$LOG_FILE"
}

vlog() {
  [[ "$VERBOSE_MODE" == "true" ]] && log "${CYAN}[VERBOSE]${NC} $1"
}

show_progress() {
  local current=$1
  local total=$2
  local percent=$(( current * 100 / total ))
  local filled=$(( percent / 2 ))
  local empty=$(( 50 - filled ))
  printf "\r${BLUE}[PROGRESS]${NC} ["
  printf "%*s" $filled | tr ' ' '='
  printf "%*s" $empty | tr ' ' '-'
  printf "] %d%% (%d/%d)" $percent $current $total
}

substitute_urls() {
  # Substitute localhost URL with configured base URL
  local command="$1"
  echo "$command" | sed "s|http://localhost:8182|$PULSAR_BASE_URL|g"
}

check_server() {
  log "${BLUE}[INFO]${NC} Checking PulsarRPA server at $PULSAR_BASE_URL..."
  if curl -s --connect-timeout 5 --max-time 10 "$PULSAR_BASE_URL/actuator/health" >/dev/null 2>&1; then
    log "${GREEN}[SUCCESS]${NC} PulsarRPA server is healthy and responding"
    return 0
  elif curl -s --connect-timeout 5 --max-time 10 "$PULSAR_BASE_URL/" >/dev/null 2>&1; then
    log "${YELLOW}[WARNING]${NC} Server responding but health check endpoint unavailable"
    return 0
  else
    log "${RED}[ERROR]${NC} PulsarRPA server not accessible at $PULSAR_BASE_URL"
    log "${CYAN}[HINT]${NC} Start PulsarRPA with:"
    log "    ${BOLD}java -DDEEPSEEK_API_KEY=\${DEEPSEEK_API_KEY} -jar PulsarRPA.jar${NC}"
    return 1
  fi
}

# =============================================================================
# SECTION: TEST EXECUTION FUNCTIONS
# =============================================================================

# Execute a curl command with appropriate parameters and return results
execute_curl_command() {
  local curl_command="$1"
  local timeout="$2"
  local response_file="$3"
  local error_file="$4"

  local final_command=$(substitute_urls "$curl_command")
  local full_command="$final_command --max-time $timeout -w '%{http_code}\\n%{time_total}\\n%{size_download}\\n%{url_effective}\\n%{content_type}' -o $response_file -s"

  vlog "Executing: \n$(echo "$full_command" | head -c 2000)"

  eval "$full_command" > "${response_file}.meta" 2>"$error_file"
  return $?
}

# Process successful responses
process_success_response() {
  local response_file="$1"
  local test_number="$2"
  local content_type="$3"
  local size_download="$4"
  local size_download_mb=$(( size_download / 1024 / 1024 ))

  local target="${TEST_RESULTS_DIR}/test_${test_number}_response.json"
  cp "$response_file" "$target" 2>/dev/null || true

  # Get and show response brief for successful tests
  if [[ "$size_download" -gt 0 ]]; then
      # Fallback if extract_response_brief.sh is not available
      if [[ "$size_download_mb" -lt 1 ]]; then
#        local preview=$(head -c 250 "$response_file" 2>/dev/null | tr -d '\n\r' | sed 's/[[:space:]]\+/ /g')
        local preview=$(head -c 500 "$response_file" 2>/dev/null)
        [[ -n "$preview" && "$preview" != " " ]] && log "${CYAN}[PREVIEW]${NC} $preview..."
      else
        log "${CYAN}[INFO]${NC} Large response (${size_download}B) saved to results directory"
      fi
  fi
}

# Process error responses with HTTP status codes
process_error_response() {
  local response_file="$1"
  local error_file="$2"
  local test_number="$3"
  local http_status="$4"
  local content_type="$5"
  local size_download="$6"

  cp "$response_file" "${TEST_RESULTS_DIR}/test_${test_number}_error_${http_status}.txt" 2>/dev/null || true

  if [[ -s "$response_file" ]]; then
    local error_preview=$(head -c 2000 "$response_file" 2>/dev/null | tr -d '\n\r')
    log "${RED}[ERROR RESPONSE]${NC} $error_preview"
  fi

  if [[ -s "$error_file" ]]; then
    local curl_error=$(head -c 2000 "$error_file" 2>/dev/null | tr -d '\n\r')
    log "${RED}[CURL ERROR]${NC} $curl_error"
  fi
}

# Process command execution errors
process_execution_error() {
  local error_file="$1"
  local test_number="$2"
  local final_command="$3"

  {
    echo "Command: $final_command"
    echo "Error output:"
    cat "$error_file" 2>/dev/null || echo "No error output available"
  } > "${TEST_RESULTS_DIR}/test_${test_number}_exec_error.txt"

  if [[ -s "$error_file" ]]; then
    local exec_error=$(head -c 2000 "$error_file" 2>/dev/null | tr -d '\n\r')
    log "${RED}[EXECUTION ERROR]${NC} $exec_error"
  fi
}

# Main function to run a curl test
run_curl_test() {
  local test_name="$1"
  local curl_command="$2"
  local test_number=$3

  TOTAL_TESTS=$((TOTAL_TESTS + 1))
  log ""
  log "${PURPLE}[TEST $test_number/${#CURL_COMMANDS[@]}]${NC} ${BOLD}$test_name${NC}"

  # Show command preview
  if [[ "$VERBOSE_MODE" == "true" ]]; then
    log "${CYAN}[COMMAND]${NC}"
    echo "$curl_command"
  else
    local short_cmd=$(echo "$curl_command" | head -c 200 | tr '\n' ' ')
    log "${CYAN}[COMMAND]${NC} $short_cmd..."
  fi

  # Temp files
  local response_file
  local error_file
  response_file=$(mktemp)
  error_file=$(mktemp)

  # Execute the command
  local start_time=$(date +%s)
  if execute_curl_command "$curl_command" "$TIMEOUT_SECONDS" "$response_file" "$error_file"; then
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    local http_status=$(sed -n '1p' "${response_file}.meta" 2>/dev/null || echo "000")
    local time_total=$(sed -n '2p' "${response_file}.meta" 2>/dev/null || echo "0.000")
    local size_download=$(sed -n '3p' "${response_file}.meta" 2>/dev/null || echo "0")
    local url_effective=$(sed -n '4p' "${response_file}.meta" 2>/dev/null || echo "N/A")
    local content_type=$(sed -n '5p' "${response_file}.meta" 2>/dev/null || echo "text/plain")

    log "${BLUE}[RESPONSE]${NC} Status: $http_status | Time: ${time_total}s | Size: ${size_download}B | Duration: ${duration}s"

    # Check success
    if [[ "$http_status" =~ ^[23][0-9][0-9]$ ]]; then
      log "${GREEN}[PASS]${NC} ✅ Test completed successfully"
      PASSED_TESTS=$((PASSED_TESTS + 1))
      process_success_response "$response_file" "$test_number" "$content_type" "$size_download"
    else
      log "${RED}[FAIL]${NC} ❌ HTTP Status: $http_status"
      FAILED_TESTS=$((FAILED_TESTS + 1))
      process_error_response "$response_file" "$error_file" "$test_number" "$http_status" "$content_type" "$size_download"
    fi
  else
    log "${RED}[FAIL]${NC} ❌ Command execution failed"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    process_execution_error "$error_file" "$test_number" "$(substitute_urls "$curl_command")"
  fi

  rm -f "$response_file" "$error_file" "${response_file}.meta"
  [[ $test_number -lt ${#CURL_COMMANDS[@]} ]] && show_progress $TOTAL_TESTS ${#CURL_COMMANDS[@]}
}

run_all_tests() {
  log "${BLUE}[INFO]${NC} ${BOLD}Starting test execution...${NC}"
  log "${BLUE}[INFO]${NC} Total commands to test: ${#CURL_COMMANDS[@]}"
  local test_counter=0
  for command_entry in "${CURL_COMMANDS[@]}"; do
    test_counter=$((test_counter + 1))
    [[ "$command_entry" =~ ^[[:space:]]*# ]] && { log "${YELLOW}[SKIP]${NC} Skipping commented command $test_counter"; SKIPPED_TESTS=$((SKIPPED_TESTS + 1)); continue; }
    local description=$(echo "$command_entry" | cut -d'|' -f1)
    local curl_command=$(echo "$command_entry" | cut -d'|' -f2-)
    run_curl_test "$description" "$curl_command" "$test_counter"
    echo -n " "
    [[ "$FAST_MODE" == "false" ]] && sleep 1
  done
  echo
}

print_summary() {
  local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))

  log ""
  log "=============================================="
  log "${BLUE}[FINAL SUMMARY]${NC} ${BOLD}Test Results${NC}"
  log "=============================================="
  log "${BLUE}Test Session:${NC} $(date '+%Y-%m-%d %H:%M:%S UTC')"
  log "${BLUE}User:${NC} $USER_NAME"
  log "${BLUE}Server:${NC} $PULSAR_BASE_URL"
  log "${BLUE}Total Commands:${NC} ${#CURL_COMMANDS[@]}"
  log "${BLUE}Tests Executed:${NC} $TOTAL_TESTS"
  log "${GREEN}Passed:${NC} $PASSED_TESTS"
  log "${RED}Failed:${NC} $FAILED_TESTS"
  log "${YELLOW}Skipped:${NC} $SKIPPED_TESTS"
  if [[ $TOTAL_TESTS -gt 0 ]]; then
    log "${BLUE}Success Rate:${NC} $success_rate%"
  fi
  log "${BLUE}Log File:${NC} $LOG_FILE"
  log "${BLUE}Results Directory:${NC} $TEST_RESULTS_DIR"
  log "=============================================="
  if [[ $TOTAL_TESTS -eq 0 ]]; then
    log "${YELLOW}[INFO]${NC} No tests were executed"
    exit 0
  elif [[ $FAILED_TESTS -eq 0 ]]; then
    log "${GREEN}[SUCCESS]${NC} All tests passed! 🎉"
    exit 0
  else
    log "${YELLOW}[PARTIAL SUCCESS]${NC} Some tests failed. Check logs for details."

    if [[ $success_rate -lt 80 ]]; then
      log "${RED}[FAILURE]${NC} Success rate below 80%. Exiting with failure."
      exit 1
    else
      log "${YELLOW}[PARTIAL SUCCESS]${NC} Some tests failed. Check logs for details."
      exit 0
    fi
  fi
}

usage() {
  cat << EOF
Usage: $0 [OPTIONS]

Test curl commands from README.md against PulsarRPA server.

OPTIONS:
-u, --url URL         PulsarRPA base URL (default: $DEFAULT_BASE_URL)
-f, --fast            Fast mode - minimal delays between tests
-s, --skip-server     Skip server connectivity check
-t, --timeout SEC     Request timeout in seconds (default: 120)
-v, --verbose         Enable verbose output
-h, --help            Show this help message

EXAMPLES:
$0                              # Run all tests with defaults
$0 -u http://localhost:8080     # Use custom server URL
$0 --fast --timeout 60          # Fast mode with 60s timeout
$0 --skip-server --verbose      # Skip server check with verbose output

REQUIREMENTS:
- curl command available
- PulsarRPA server running (unless --skip-server)

UPDATING COMMANDS:
Edit the CURL_COMMANDS array to add/modify tests.
EOF
}

# =============================================================================
# SECTION: MAIN EXECUTION LOGIC
# =============================================================================

parse_args() {
  while [[ $# -gt 0 ]]; do
    case $1 in
      -u|--url) PULSAR_BASE_URL="$2"; shift 2 ;;
      -f|--fast) FAST_MODE=true; shift ;;
      -s|--skip-server) SKIP_SERVER_CHECK=true; shift ;;
      -t|--timeout) TIMEOUT_SECONDS="$2"; shift 2 ;;
      -v|--verbose) VERBOSE_MODE=true; shift ;;
      -h|--help) usage; exit 0 ;;
      *) echo "Unknown option: $1"; usage; exit 1 ;;
    esac
  done
}

main() {
  log "${BLUE}[INFO]${NC} ${BOLD}PulsarRPA Curl Command Test Suite${NC}"
  log "${BLUE}[INFO]${NC} User: $USER_NAME"
  log "${BLUE}[INFO]${NC} Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
  log "${BLUE}[INFO]${NC} Server URL: $PULSAR_BASE_URL"
  log "${BLUE}[INFO]${NC} Timeout: ${TIMEOUT_SECONDS}s"
  log "${BLUE}[INFO]${NC} Fast Mode: $FAST_MODE"
  log "${BLUE}[INFO]${NC} Verbose Mode: $VERBOSE_MODE"
  if ! command -v curl &> /dev/null; then
    log "${RED}[ERROR]${NC} curl command not found. Please install curl."
    exit 1
  fi
  if [[ "$SKIP_SERVER_CHECK" != "true" ]]; then
    if ! check_server; then
      log "${YELLOW}[WARNING]${NC} Use --skip-server to bypass server check"
      exit 1
    fi
  fi
  run_all_tests
  print_summary
}

trap 'log "\n${YELLOW}[INFO]${NC} Tests interrupted by user"; exit 130' INT

parse_args "$@"
main

