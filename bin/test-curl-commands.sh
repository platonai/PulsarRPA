#!/bin/bash

# Test script for curl commands from README.md
# All curl commands are explicitly listed below for easy maintenance
# Author: Auto-generated for platonai
# Date: 2025-06-11 17:29:59

# set -e  # Exit on error

# =============================================================================
# CURL COMMANDS FROM README.MD - UPDATE THIS SECTION AS NEEDED
# =============================================================================

# =========================
# CURL 命令与描述变量定义
# =========================

# System Health Checks (Quick tests first)
CURL_DESC_HEALTH_CHECK="Health Check Endpoint"
CURL_CMD_HEALTH_CHECK='curl -X GET "http://localhost:8182/api/actuator/health"'

CURL_DESC_APP_INFO="Application Info Endpoint"
CURL_CMD_APP_INFO='curl -X GET "http://localhost:8182/api/actuator/info"'

CURL_DESC_QUERY_PARAMS="Query Parameters Test"
CURL_CMD_QUERY_PARAMS='curl -X GET "http://localhost:8182/api/actuator/health?details=true"'

CURL_DESC_METRICS="Metrics Endpoint Test"
CURL_CMD_METRICS='
curl -X GET "http://localhost:8182/api/actuator/metrics"
'

CURL_DESC_WEBUI="WebUI Command Interface"
CURL_CMD_WEBUI='
curl -X GET "http://localhost:8182/api/command.html"
'

CURL_DESC_CUSTOM_HEADERS="Custom Headers Test"
read -r -d '' CURL_CMD_CUSTOM_HEADERS << 'EOF'
curl -X GET "http://localhost:8182/api/actuator/health" -H "Accept: application/json" -H "User-Agent: PulsarRPA-Test-Suite/1.0"
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
"pageSummaryPrompt": "Provide a brief introduction of this product.",
"dataExtractionRules": "product name, price, and ratings",
"linkExtractionRules": "all links containing /dp/ on the page",
"onPageReadyActions": ["click #title", "scroll to the middle"]
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
"$CURL_DESC_APP_INFO|$CURL_CMD_APP_INFO"
"$CURL_DESC_QUERY_PARAMS|$CURL_CMD_QUERY_PARAMS"
"$CURL_DESC_METRICS|$CURL_CMD_METRICS"
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
# CONFIGURATION AND SETUP
# =============================================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Test configuration
PULSAR_BASE_URL="http://localhost:8182"
TEST_RESULTS_DIR="./target/test-results"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
LOG_FILE="${TEST_RESULTS_DIR}/curl_tests_${TIMESTAMP}.log"

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Test options
TIMEOUT_SECONDS=120
FAST_MODE=false
SKIP_SERVER_CHECK=false
VERBOSE_MODE=false

# Create test results directory
mkdir -p "$TEST_RESULTS_DIR"

# =============================================================================
# UTILITY FUNCTIONS
# =============================================================================

# Logging function with timestamp
log() {
local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
echo -e "[$timestamp] $1" | tee -a "$LOG_FILE"
}

# Verbose logging
vlog() {
if [[ "$VERBOSE_MODE" == "true" ]]; then
log "${CYAN}[VERBOSE]${NC} $1"
fi
}

# Progress indicator
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

# Function to check if PulsarRPA is running
check_server() {
log "${BLUE}[INFO]${NC} Checking PulsarRPA server at $PULSAR_BASE_URL..."

    # Try health check first
    if curl -s --connect-timeout 5 --max-time 10 "$PULSAR_BASE_URL/actuator/health" > /dev/null 2>&1; then
        log "${GREEN}[SUCCESS]${NC} PulsarRPA server is healthy and responding"
        return 0
    fi

    # Try basic connectivity
    if curl -s --connect-timeout 5 --max-time 10 "$PULSAR_BASE_URL/" > /dev/null 2>&1; then
        log "${YELLOW}[WARNING]${NC} Server responding but health check endpoint unavailable"
        return 0
    fi

    log "${RED}[ERROR]${NC} PulsarRPA server not accessible at $PULSAR_BASE_URL"
    log "${CYAN}[HINT]${NC} Start PulsarRPA with:"
    log "    ${BOLD}java -DDEEPSEEK_API_KEY=\${DEEPSEEK_API_KEY} -jar PulsarRPA.jar${NC}"
    return 1
}

# Function to substitute URL placeholders
substitute_urls() {
local command="$1"
echo "$command" | sed "s|http://localhost:8182|$PULSAR_BASE_URL|g"
}

# Function to run a single curl test
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
        local short_cmd=$(echo "$curl_command" | head -c 80 | tr '\n' ' ')
        log "${CYAN}[COMMAND]${NC} $short_cmd..."
    fi

    # Create temporary files
    local response_file=$(mktemp)
    local headers_file=$(mktemp)
    local error_file=$(mktemp)

    # Substitute URLs in command
    local final_command=$(substitute_urls "$curl_command")

    # Add curl monitoring options
    local full_command="$final_command --max-time $TIMEOUT_SECONDS -w '%{http_code}\\n%{time_total}\\n%{size_download}\\n%{url_effective}' -D '$headers_file' -o '$response_file' -s"

    vlog "Executing: $(echo "$full_command" | head -c 150)..."

    # Execute the command
    local start_time=$(date +%s)
    if eval "$full_command" > "${response_file}.meta" 2>"$error_file"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))

        # Parse response metadata
        local http_status=$(sed -n '1p' "${response_file}.meta" 2>/dev/null || echo "000")
        local time_total=$(sed -n '2p' "${response_file}.meta" 2>/dev/null || echo "0.000")
        local size_download=$(sed -n '3p' "${response_file}.meta" 2>/dev/null || echo "0")
        local url_effective=$(sed -n '4p' "${response_file}.meta" 2>/dev/null || echo "N/A")

        log "${BLUE}[RESPONSE]${NC} Status: $http_status | Time: ${time_total}s | Size: ${size_download}B | Duration: ${duration}s"

        # Check success (2xx or 3xx status codes)
        if [[ "$http_status" =~ ^[23][0-9][0-9]$ ]]; then
            log "${GREEN}[PASS]${NC} ✅ Test completed successfully"
            PASSED_TESTS=$((PASSED_TESTS + 1))

            # Save successful response
            cp "$response_file" "${TEST_RESULTS_DIR}/test_${test_number}_success.json" 2>/dev/null || true

            # Show response preview for reasonably sized responses
            if [[ "$size_download" -gt 0 && "$size_download" -lt 3000 ]]; then
                local preview=$(head -c 250 "$response_file" 2>/dev/null | tr -d '\n\r' | sed 's/[[:space:]]\+/ /g')
                if [[ -n "$preview" && "$preview" != " " ]]; then
                    log "${CYAN}[PREVIEW]${NC} $preview..."
                fi
            elif [[ "$size_download" -gt 3000 ]]; then
                log "${CYAN}[INFO]${NC} Large response (${size_download}B) saved to results directory"
            fi

        else
            log "${RED}[FAIL]${NC} ❌ HTTP Status: $http_status"
            FAILED_TESTS=$((FAILED_TESTS + 1))

            # Save error response
            cp "$response_file" "${TEST_RESULTS_DIR}/test_${test_number}_error_${http_status}.txt" 2>/dev/null || true

            # Show error details
            if [[ -s "$response_file" ]]; then
                local error_preview=$(head -c 200 "$response_file" 2>/dev/null | tr -d '\n\r')
                log "${RED}[ERROR RESPONSE]${NC} $error_preview"
            fi

            if [[ -s "$error_file" ]]; then
                local curl_error=$(head -c 200 "$error_file" 2>/dev/null | tr -d '\n\r')
                log "${RED}[CURL ERROR]${NC} $curl_error"
            fi
        fi

    else
        log "${RED}[FAIL]${NC} ❌ Command execution failed"
        FAILED_TESTS=$((FAILED_TESTS + 1))

        # Save execution error details
        {
            echo "Command: $final_command"
            echo "Error output:"
            cat "$error_file" 2>/dev/null || echo "No error output available"
        } > "${TEST_RESULTS_DIR}/test_${test_number}_exec_error.txt"

        if [[ -s "$error_file" ]]; then
            local exec_error=$(head -c 200 "$error_file" 2>/dev/null | tr -d '\n\r')
            log "${RED}[EXECUTION ERROR]${NC} $exec_error"
        fi
    fi

    # Cleanup temporary files
    rm -f "$response_file" "$headers_file" "$error_file" "${response_file}.meta"

    # Show progress (but not on last test)
    if [[ $test_number -lt ${#CURL_COMMANDS[@]} ]]; then
        show_progress $TOTAL_TESTS ${#CURL_COMMANDS[@]}
    fi
}

# Function to run all tests
run_all_tests() {
log "${BLUE}[INFO]${NC} ${BOLD}Starting test execution...${NC}"
log "${BLUE}[INFO]${NC} Total commands to test: ${#CURL_COMMANDS[@]}"

    local test_counter=0

    for command_entry in "${CURL_COMMANDS[@]}"; do
        test_counter=$((test_counter + 1))

        # Skip commented out commands
        if [[ "$command_entry" =~ ^[[:space:]]*# ]]; then
            log "${YELLOW}[SKIP]${NC} Skipping commented command $test_counter"
            SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
            continue
        fi

        # Parse command entry (format: "description|curl_command")
        local description=$(echo "$command_entry" | cut -d'|' -f1)
        local curl_command=$(echo "$command_entry" | cut -d'|' -f2-)

        # Run the test
        run_curl_test "$description" "$curl_command" "$test_counter"

        # Small delay between tests unless in fast mode
        if [[ "$FAST_MODE" == "false" ]]; then
            sleep 1
        fi
    done

    echo  # New line after progress bar
}

# Function to print test summary
print_summary() {
log ""
log "=============================================="
log "${BLUE}[FINAL SUMMARY]${NC} ${BOLD}Test Results${NC}"
log "=============================================="
log "${BLUE}Test Session:${NC} $(date '+%Y-%m-%d %H:%M:%S UTC')"
log "${BLUE}User:${NC} platonai"
log "${BLUE}Server:${NC} $PULSAR_BASE_URL"
log "${BLUE}Total Commands:${NC} ${#CURL_COMMANDS[@]}"
log "${BLUE}Tests Executed:${NC} $TOTAL_TESTS"
log "${GREEN}Passed:${NC} $PASSED_TESTS"
log "${RED}Failed:${NC} $FAILED_TESTS"
log "${YELLOW}Skipped:${NC} $SKIPPED_TESTS"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        log "${BLUE}Success Rate:${NC} $success_rate%"
    fi

    log "${BLUE}Log File:${NC} $LOG_FILE"
    log "${BLUE}Results Directory:${NC} $TEST_RESULTS_DIR"
    log "=============================================="

    # Final status
    if [[ $TOTAL_TESTS -eq 0 ]]; then
        log "${YELLOW}[INFO]${NC} No tests were executed"
        exit 0
    elif [[ $FAILED_TESTS -eq 0 ]]; then
        log "${GREEN}[SUCCESS]${NC} All tests passed! 🎉"
        exit 0
    else
        log "${YELLOW}[PARTIAL SUCCESS]${NC} Some tests failed. Check logs for details."
        exit 1
    fi
}

# Function to show usage
usage() {
cat << EOF
Usage: $0 [OPTIONS]

Test curl commands from README.md against PulsarRPA server.

OPTIONS:
-u, --url URL         PulsarRPA base URL (default: http://localhost:8182)
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
Edit the CURL_DESC_* and CURL_CMD_* variables to add/modify tests.
Commands use Here-String format for better readability.
EOF
}

# =============================================================================
# MAIN EXECUTION
# =============================================================================

# Parse command line arguments
while [[ $# -gt 0 ]]; do
case $1 in
-u|--url)
PULSAR_BASE_URL="$2"
shift 2
;;
-f|--fast)
FAST_MODE=true
shift
;;
-s|--skip-server)
SKIP_SERVER_CHECK=true
shift
;;
-t|--timeout)
TIMEOUT_SECONDS="$2"
shift 2
;;
-v|--verbose)
VERBOSE_MODE=true
shift
;;
-h|--help)
usage
exit 0
;;
*)
echo "Unknown option: $1"
usage
exit 1
;;
esac
done

# Main execution function
main() {
log "${BLUE}[INFO]${NC} ${BOLD}PulsarRPA Curl Command Test Suite${NC}"
log "${BLUE}[INFO]${NC} User: platonai"
log "${BLUE}[INFO]${NC} Timestamp: 2025-06-11 17:29:59"
log "${BLUE}[INFO]${NC} Server URL: $PULSAR_BASE_URL"
log "${BLUE}[INFO]${NC} Timeout: ${TIMEOUT_SECONDS}s"
log "${BLUE}[INFO]${NC} Fast Mode: $FAST_MODE"
log "${BLUE}[INFO]${NC} Verbose Mode: $VERBOSE_MODE"

    # Check prerequisites
    if ! command -v curl &> /dev/null; then
        log "${RED}[ERROR]${NC} curl command not found. Please install curl."
        exit 1
    fi

    # Check server (unless skipped)
    if [[ "$SKIP_SERVER_CHECK" != "true" ]]; then
        if ! check_server; then
            log "${YELLOW}[WARNING]${NC} Use --skip-server to bypass server check"
            exit 1
        fi
    fi

    # Run all tests
    run_all_tests

    # Print summary
    print_summary
}

# Handle interruption
trap 'log "\n${YELLOW}[INFO]${NC} Tests interrupted by user"; exit 130' INT

# Execute main function
main "$@"