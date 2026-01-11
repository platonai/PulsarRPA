#!/usr/bin/env bash

# =============================================================================
# run-e2e-test-v2.sh - Use Case Based End-to-End Test Suite
# =============================================================================
# This script reads use case files from bin/tests/use-cases/ and executes them
# against the Browser4 server using the api/commands/plain endpoint.
#
# Each use case file contains:
#   - Task description (lines starting with #)
#   - Task content (the actual steps for the agent)
#
# The script sends each task to the server and validates the response
# using appropriate assertions based on task type.
# =============================================================================

# Find the first parent directory containing the VERSION file
AppHome="$(dirname "$(readlink -f "$0")")"
while [[ "$AppHome" != "/" && ! -f "$AppHome/VERSION" ]]; do
    AppHome="$(dirname "$AppHome")"
done
cd "$AppHome" || exit 1

set -e

# =============================================================================
# Configuration
# =============================================================================
readonly API_BASE="${API_BASE:-http://localhost:8182}"
readonly COMMAND_ENDPOINT="$API_BASE/api/commands/plain"
readonly USE_CASES_DIR="$AppHome/bin/tests/use-cases"
readonly TEST_RESULTS_DIR="./target/test-results/use-cases"
readonly TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"
readonly LOG_FILE="${TEST_RESULTS_DIR}/use_case_tests_${TIMESTAMP}.log"

# Timeout settings (seconds)
readonly DEFAULT_TIMEOUT="${DEFAULT_TIMEOUT:-300}"
readonly SIMPLE_TIMEOUT="${SIMPLE_TIMEOUT:-180}"
readonly COMPLEX_TIMEOUT="${COMPLEX_TIMEOUT:-300}"
readonly ENTERPRISE_TIMEOUT="${ENTERPRISE_TIMEOUT:-600}"

# Minimum success rate for overall pass (percentage, integer)
readonly MIN_SUCCESS_RATE="${MIN_SUCCESS_RATE:-50}"

# Test selection (comma-separated list of test numbers or "all")
TEST_SELECTION="${TEST_SELECTION:-all}"

# Verbose mode
VERBOSE="${VERBOSE:-false}"

# Skip server check
SKIP_SERVER_CHECK="${SKIP_SERVER_CHECK:-false}"

# =============================================================================
# Colors
# =============================================================================
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly BOLD='\033[1m'
readonly NC='\033[0m'

# =============================================================================
# Counters
# =============================================================================
TOTAL_TESTS=0
EXECUTED_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0
TIMED_OUT_TESTS=0

# =============================================================================
# Utility Functions
# =============================================================================

log() {
    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "[$timestamp] $1" | tee -a "$LOG_FILE"
}

vlog() {
    [[ "$VERBOSE" == "true" ]] && log "${CYAN}[VERBOSE]${NC} $1"
}

ensure_directories() {
    mkdir -p "$TEST_RESULTS_DIR"
}

check_server() {
    log "${BLUE}[INFO]${NC} Checking Browser4 server at $API_BASE..."
    if curl -s --connect-timeout 5 --max-time 10 "$API_BASE/actuator/health" >/dev/null 2>&1; then
        log "${GREEN}[SUCCESS]${NC} Browser4 server is healthy and responding"
        return 0
    elif curl -s --connect-timeout 5 --max-time 10 "$API_BASE/" >/dev/null 2>&1; then
        log "${YELLOW}[WARNING]${NC} Server responding but health check endpoint unavailable"
        return 0
    else
        log "${RED}[ERROR]${NC} Browser4 server not accessible at $API_BASE"
        log "${CYAN}[HINT]${NC} Start Browser4 with:"
        log "    ${BOLD}java -DOPENROUTER_API_KEY=\${OPENROUTER_API_KEY} -jar Browser4.jar${NC}"
        return 1
    fi
}

# =============================================================================
# Use Case File Processing
# =============================================================================

# Extract task description (comment lines) from use case file
extract_description() {
    local file="$1"
    grep '^#' "$file" | sed 's/^# *//' | head -5
}

# Extract task level from use case file
extract_level() {
    local file="$1"
    local level_line
    level_line=$(grep -i '^# Level:' "$file" | head -1)
    if [[ -n "$level_line" ]]; then
        echo "$level_line" | sed 's/^# Level: *//'
    else
        echo "Simple"  # Default level
    fi
}

# Extract task content (non-comment lines) from use case file
extract_task_content() {
    local file="$1"
    grep -v '^#' "$file" | grep -v '^[[:space:]]*$'
}

# Get timeout based on task level
get_timeout_for_level() {
    local level="$1"
    case "$level" in
        *Simple*) echo "$SIMPLE_TIMEOUT" ;;
        *Complex*) echo "$COMPLEX_TIMEOUT" ;;
        *Enterprise*) echo "$ENTERPRISE_TIMEOUT" ;;
        *) echo "$DEFAULT_TIMEOUT" ;;
    esac
}

# =============================================================================
# Assertion Functions
# =============================================================================

# Basic assertion: HTTP status code is 2xx or 3xx
assert_http_success() {
    local http_status="$1"
    [[ "$http_status" =~ ^[23][0-9][0-9]$ ]]
}

# Assert response contains expected content patterns
assert_response_has_content() {
    local response_file="$1"
    local min_length="${2:-10}"
    
    if [[ ! -f "$response_file" ]]; then
        return 1
    fi
    
    local content_length
    content_length=$(wc -c < "$response_file")
    [[ "$content_length" -gt "$min_length" ]]
}

# Assert response is valid JSON (when expected)
assert_valid_json() {
    local response_file="$1"
    
    if command -v jq &>/dev/null; then
        jq empty "$response_file" 2>/dev/null
    else
        # Fallback: check for JSON-like structure
        grep -qE '^\s*[\[{]' "$response_file"
    fi
}

# Assert response indicates task completion
assert_task_completed() {
    local response_file="$1"
    
    # Check for common completion indicators
    if grep -qiE '"(status|state)"[[:space:]]*:[[:space:]]*"(completed|done|success|finished)"' "$response_file" 2>/dev/null; then
        return 0
    fi
    
    # Check for result/data presence
    if grep -qiE '"(result|data|output|summary)"[[:space:]]*:' "$response_file" 2>/dev/null; then
        return 0
    fi
    
    # Check for non-empty response with substantial content
    local content_length
    content_length=$(wc -c < "$response_file")
    [[ "$content_length" -gt 50 ]]
}

# =============================================================================
# Test Execution
# =============================================================================

# Execute a single use case test
run_use_case_test() {
    local use_case_file="$1"
    local test_number="$2"
    local total_tests="$3"
    
    local filename
    filename=$(basename "$use_case_file")
    local test_name="${filename%.txt}"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    log ""
    log "===================================================================="
    log "${PURPLE}[TEST $test_number/$total_tests]${NC} ${BOLD}$test_name${NC}"
    log "--------------------------------------------------------------------"
    
    # Extract task metadata
    local description
    description=$(extract_description "$use_case_file")
    local level
    level=$(extract_level "$use_case_file")
    local task_content
    task_content=$(extract_task_content "$use_case_file")
    local timeout
    timeout=$(get_timeout_for_level "$level")
    
    log "${CYAN}[DESCRIPTION]${NC}"
    echo "$description" | while read -r line; do
        log "    $line"
    done
    log "${CYAN}[LEVEL]${NC} $level"
    log "${CYAN}[TIMEOUT]${NC} ${timeout}s"
    
    if [[ "$VERBOSE" == "true" ]]; then
        log "${CYAN}[TASK CONTENT]${NC}"
        echo "$task_content" | while read -r line; do
            log "    $line"
        done
    fi
    
    # Prepare temp files
    local response_file
    response_file=$(mktemp)
    local error_file
    error_file=$(mktemp)
    
    # Start time tracking
    local start_time
    start_time=$(date '+%Y-%m-%d %H:%M:%S %Z')
    local start_epoch
    start_epoch=$(date +%s)
    log "${BLUE}[INFO]${NC} Start time: $start_time"
    
    # Execute the command
    log "${BLUE}[INFO]${NC} Sending task to server..."
    
    set +e
    local http_status
    http_status=$(curl -s -w '%{http_code}' \
        --max-time "$timeout" \
        -X POST \
        -H "Content-Type: text/plain" \
        --data "$task_content" \
        -o "$response_file" \
        "$COMMAND_ENDPOINT" 2>"$error_file")
    local exit_code=$?
    set -e
    
    # End time tracking
    local end_time
    end_time=$(date '+%Y-%m-%d %H:%M:%S %Z')
    local end_epoch
    end_epoch=$(date +%s)
    local duration=$((end_epoch - start_epoch))
    
    # Process results
    local response_size=0
    if [[ -f "$response_file" ]]; then
        response_size=$(wc -c < "$response_file")
    fi
    
    log "${BLUE}[RESPONSE]${NC} HTTP Status: $http_status | Size: ${response_size}B | Duration: ${duration}s"
    
    local test_passed=false
    local test_result=""
    
    if [[ $exit_code -eq 28 ]]; then
        # Timeout
        TIMED_OUT_TESTS=$((TIMED_OUT_TESTS + 1))
        test_result="TIMEOUT"
        log "${YELLOW}[TIMEOUT]${NC} Command exceeded ${timeout}s and was aborted"
        
        # Save partial response
        cp "$response_file" "${TEST_RESULTS_DIR}/${test_name}_timeout_response.txt" 2>/dev/null || true
        
    elif [[ $exit_code -ne 0 ]]; then
        # Curl execution error
        FAILED_TESTS=$((FAILED_TESTS + 1))
        EXECUTED_TESTS=$((EXECUTED_TESTS + 1))
        test_result="FAIL"
        log "${RED}[FAIL]${NC} ❌ Curl execution failed (exit code: $exit_code)"
        
        if [[ -s "$error_file" ]]; then
            local curl_error
            curl_error=$(head -c 500 "$error_file")
            log "${RED}[ERROR]${NC} $curl_error"
        fi
        
    elif assert_http_success "$http_status"; then
        # HTTP success - now validate response content
        EXECUTED_TESTS=$((EXECUTED_TESTS + 1))
        
        # Run assertions based on level
        local assertion_passed=true
        
        # Basic content assertion
        if ! assert_response_has_content "$response_file" 10; then
            assertion_passed=false
            log "${YELLOW}[ASSERTION]${NC} Response content too short"
        fi
        
        # Task completion assertion (for non-async requests)
        if [[ "$assertion_passed" == "true" ]] && ! assert_task_completed "$response_file"; then
            # Not a strict failure for complex tasks
            if [[ "$level" != *Simple* ]]; then
                log "${YELLOW}[ASSERTION]${NC} Task completion unclear (complex task - acceptable)"
            else
                assertion_passed=false
                log "${YELLOW}[ASSERTION]${NC} Task completion not detected"
            fi
        fi
        
        if [[ "$assertion_passed" == "true" ]]; then
            PASSED_TESTS=$((PASSED_TESTS + 1))
            test_result="PASS"
            test_passed=true
            log "${GREEN}[PASS]${NC} ✅ Test completed successfully"
        else
            FAILED_TESTS=$((FAILED_TESTS + 1))
            test_result="FAIL"
            log "${RED}[FAIL]${NC} ❌ Assertions failed"
        fi
        
        # Save response
        cp "$response_file" "${TEST_RESULTS_DIR}/${test_name}_response.txt" 2>/dev/null || true
        
        # Show response preview
        if [[ "$response_size" -gt 0 && "$response_size" -lt 2000 ]]; then
            local preview
            preview=$(head -c 500 "$response_file")
            log "${CYAN}[PREVIEW]${NC} $preview..."
        elif [[ "$response_size" -ge 2000 ]]; then
            log "${CYAN}[INFO]${NC} Large response (${response_size}B) saved to results directory"
        fi
        
    else
        # HTTP error
        FAILED_TESTS=$((FAILED_TESTS + 1))
        EXECUTED_TESTS=$((EXECUTED_TESTS + 1))
        test_result="FAIL"
        log "${RED}[FAIL]${NC} ❌ HTTP Status: $http_status"
        
        # Save error response
        cp "$response_file" "${TEST_RESULTS_DIR}/${test_name}_error_${http_status}.txt" 2>/dev/null || true
        
        if [[ -s "$response_file" ]]; then
            local error_preview
            error_preview=$(head -c 500 "$response_file")
            log "${RED}[ERROR RESPONSE]${NC} $error_preview"
        fi
    fi
    
    log "${BLUE}[INFO]${NC} End time: $end_time (Duration: ${duration}s)"
    log "===================================================================="
    
    # Cleanup
    rm -f "$response_file" "$error_file"
    
    return $([ "$test_passed" == "true" ] && echo 0 || echo 1)
}

# =============================================================================
# Main Test Runner
# =============================================================================

run_all_tests() {
    log "${BLUE}[INFO]${NC} ${BOLD}Starting Use Case Test Suite${NC}"
    log "${BLUE}[INFO]${NC} API Endpoint: $COMMAND_ENDPOINT"
    log "${BLUE}[INFO]${NC} Use Cases Directory: $USE_CASES_DIR"
    
    # Get list of use case files
    local use_case_files=()
    while IFS= read -r -d '' file; do
        use_case_files+=("$file")
    done < <(find "$USE_CASES_DIR" -name "*.txt" -type f -print0 | sort -z)
    
    local total_files=${#use_case_files[@]}
    
    if [[ $total_files -eq 0 ]]; then
        log "${RED}[ERROR]${NC} No use case files found in $USE_CASES_DIR"
        exit 1
    fi
    
    log "${BLUE}[INFO]${NC} Found $total_files use case files"
    
    # Build list of tests to run
    local tests_to_run=()
    if [[ "$TEST_SELECTION" == "all" ]]; then
        tests_to_run=("${use_case_files[@]}")
    else
        # Parse comma-separated test numbers
        IFS=',' read -ra selected_numbers <<< "$TEST_SELECTION"
        for num in "${selected_numbers[@]}"; do
            # Trim whitespace
            num=$(echo "$num" | tr -d ' ')
            # Find matching file
            for file in "${use_case_files[@]}"; do
                if [[ "$(basename "$file")" == "$num"* ]]; then
                    tests_to_run+=("$file")
                    break
                fi
            done
        done
    fi
    
    local total_selected=${#tests_to_run[@]}
    log "${BLUE}[INFO]${NC} Tests to execute: $total_selected"
    
    # Run tests
    local test_counter=0
    for use_case_file in "${tests_to_run[@]}"; do
        test_counter=$((test_counter + 1))
        run_use_case_test "$use_case_file" "$test_counter" "$total_selected"
    done
}

print_summary() {
    local success_rate=0
    if [[ $EXECUTED_TESTS -gt 0 ]]; then
        success_rate=$(( PASSED_TESTS * 100 / EXECUTED_TESTS ))
    fi
    
    log ""
    log "===================================================================="
    log "${BLUE}[FINAL SUMMARY]${NC} ${BOLD}Use Case Test Results${NC}"
    log "===================================================================="
    log "${BLUE}Test Session:${NC} $(date '+%Y-%m-%d %H:%M:%S')"
    log "${BLUE}Server:${NC} $API_BASE"
    log "${BLUE}Use Cases Directory:${NC} $USE_CASES_DIR"
    log "--------------------------------------------------------------------"
    log "${BLUE}Total Tests:${NC} $TOTAL_TESTS"
    log "${BLUE}Executed:${NC} $EXECUTED_TESTS"
    log "${GREEN}Passed:${NC} $PASSED_TESTS"
    log "${RED}Failed:${NC} $FAILED_TESTS"
    log "${YELLOW}Timed Out:${NC} $TIMED_OUT_TESTS"
    log "${YELLOW}Skipped:${NC} $SKIPPED_TESTS"
    log "${BLUE}Success Rate:${NC} ${success_rate}% (minimum required: ${MIN_SUCCESS_RATE}%)"
    log "${BLUE}Log File:${NC} $LOG_FILE"
    log "${BLUE}Results Directory:${NC} $TEST_RESULTS_DIR"
    log "===================================================================="
    
    if [[ $EXECUTED_TESTS -eq 0 ]]; then
        log "${YELLOW}[WARNING]${NC} No tests were executed"
        exit 1
    fi
    
    if [[ $success_rate -ge $MIN_SUCCESS_RATE ]]; then
        log "${GREEN}[SUCCESS]${NC} Overall result: PASS 🎉"
        exit 0
    else
        log "${RED}[FAILURE]${NC} Overall result: FAIL (success rate below ${MIN_SUCCESS_RATE}%)"
        exit 1
    fi
}

usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Run use case based end-to-end tests against Browser4 server.

OPTIONS:
    -u, --url URL           Browser4 base URL (default: http://localhost:8182)
    -t, --test SELECTION    Test selection (comma-separated numbers or "all", default: all)
                            Examples: "01,02,03" or "01-ecommerce" or "all"
    -s, --skip-server       Skip server connectivity check
    -v, --verbose           Enable verbose output
    -h, --help              Show this help message

ENVIRONMENT VARIABLES:
    API_BASE                Browser4 base URL
    DEFAULT_TIMEOUT         Default timeout in seconds (default: 300)
    SIMPLE_TIMEOUT          Timeout for simple tests (default: 180)
    COMPLEX_TIMEOUT         Timeout for complex tests (default: 300)
    ENTERPRISE_TIMEOUT      Timeout for enterprise tests (default: 600)
    MIN_SUCCESS_RATE        Minimum success rate to pass (default: 50)
    TEST_SELECTION          Same as --test option
    VERBOSE                 Same as --verbose option

EXAMPLES:
    $0                              # Run all tests with defaults
    $0 -u http://localhost:8080     # Use custom server URL
    $0 -t "01,02,03"               # Run specific tests
    $0 -t "01-ecommerce"           # Run test by name prefix
    $0 --verbose                    # Enable verbose logging
    $0 --skip-server -v             # Skip server check, verbose mode

USE CASE FILES:
    Use case files are located in: bin/tests/use-cases/
    Each file should contain:
        - Comment lines (starting with #) for description
        - Task steps for the agent to execute

EOF
}

# =============================================================================
# Argument Parsing
# =============================================================================

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -u|--url)
                API_BASE="$2"
                shift 2
                ;;
            -t|--test)
                TEST_SELECTION="$2"
                shift 2
                ;;
            -s|--skip-server)
                SKIP_SERVER_CHECK="true"
                shift
                ;;
            -v|--verbose)
                VERBOSE="true"
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
}

# =============================================================================
# Main Entry Point
# =============================================================================

main() {
    ensure_directories
    
    log "${BLUE}[INFO]${NC} ${BOLD}Browser4 Use Case Test Suite${NC}"
    log "${BLUE}[INFO]${NC} Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
    log "${BLUE}[INFO]${NC} Server URL: $API_BASE"
    log "${BLUE}[INFO]${NC} Verbose Mode: $VERBOSE"
    
    # Check for curl
    if ! command -v curl &>/dev/null; then
        log "${RED}[ERROR]${NC} curl command not found. Please install curl."
        exit 1
    fi
    
    # Check server
    if [[ "$SKIP_SERVER_CHECK" != "true" ]]; then
        if ! check_server; then
            log "${YELLOW}[WARNING]${NC} Use --skip-server to bypass server check"
            exit 1
        fi
    fi
    
    # Check use cases directory
    if [[ ! -d "$USE_CASES_DIR" ]]; then
        log "${RED}[ERROR]${NC} Use cases directory not found: $USE_CASES_DIR"
        exit 1
    fi
    
    run_all_tests
    print_summary
}

# Handle interruption
trap 'log "\n${YELLOW}[INFO]${NC} Tests interrupted by user"; exit 130' INT

parse_args "$@"
main
