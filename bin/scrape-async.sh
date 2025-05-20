#!/usr/bin/env bash

set -e

# Default parameters
SEEDS_FILE="seeds.txt"
BATCH_SIZE=5
MAX_ATTEMPTS=30
DELAY_SECONDS=5
MAX_URLS=20
BASE_URL="http://localhost:8182/api/x/s"
STATUS_URL_TEMPLATE="http://localhost:8182/api/x/status?uuid=%s"
MAX_POLLING_TASKS=10

SQL_TEMPLATE="select
  dom_base_uri(dom) as url,
  dom_first_text(dom, '#productTitle') as title,
  str_substring_after(dom_first_href(dom, '#wayfinding-breadcrumbs_container ul li:last-child a'), 'node=') as category,
  dom_first_slim_html(dom, '#bylineInfo') as brand,
  cast(dom_all_slim_htmls(dom, '#imageBlock img') as varchar) as gallery,
  dom_first_slim_html(dom, '#landingImage, #imgTagWrapperId img, #imageBlock img:expr(width > 400)') as img,
  dom_first_text(dom, '#price tr td:contains(List Price) ~ td') as listprice,
  dom_first_text(dom, '#price tr td:matches(^Price) ~ td') as price,
  str_first_float(dom_first_text(dom, '#reviewsMedley .AverageCustomerReviews span:contains(out of)'), 0.0) as score
from load_and_select('{url} -i 20s -njr 3', 'body');
"

parse_args() {
  while getopts "f:b:a:d:m:" opt; do
    case $opt in
      f) SEEDS_FILE="$OPTARG" ;;
      b) BATCH_SIZE="$OPTARG" ;;
      a) MAX_ATTEMPTS="$OPTARG" ;;
      d) DELAY_SECONDS="$OPTARG" ;;
      m) MAX_URLS="$OPTARG" ;;
      *) echo "Invalid option"; exit 1 ;;
    esac
  done
}

extract_urls() {
  if [[ ! -f "$SEEDS_FILE" ]]; then
    echo "[ERROR] $SEEDS_FILE does not exist."
    exit 1
  fi
  mapfile -t URLS < <(grep -Eo 'https?://[^[:space:]/$.?#][^[:space:]]*' "$SEEDS_FILE" | awk '!seen[$0]++' | head -n "$MAX_URLS")
  if [[ ${#URLS[@]} -eq 0 ]]; then
    echo "[ERROR] No valid HTTP(S) links found in $SEEDS_FILE."
    exit 1
  fi
  echo "[INFO] Extracted ${#URLS[@]} valid links."
}

submit_task() {
  local url="$1"
  local sql="${SQL_TEMPLATE//\{url\}/$url}"
  curl -s -X POST -H "Content-Type: text/plain" --data "$sql" "$BASE_URL"
}

poll_tasks() {
  local uuids=("${@}")
  echo "First uuid: ${uuids[0]}, Total uuids: ${#uuids[@]}"
  echo "${uuids[1]}"
  echo "${uuids[2]}"

  declare -A TASK_MAP
  declare -a PENDING_TASKS=("${uuids[@]}")

  for uuid in "${PENDING_TASKS[@]}"; do
    TASK_MAP["$uuid"]=0
  done

  for ((attempt=0; attempt<MAX_ATTEMPTS; attempt++)); do
    sleep "$DELAY_SECONDS"
    local batch_uuids=("${PENDING_TASKS[@]:0:$MAX_POLLING_TASKS}")
    local new_pending=()
    for uuid in "${batch_uuids[@]}"; do
      echo "[INFO] Polling for task $uuid..."

      local status_url
      status_url=$(printf "$STATUS_URL_TEMPLATE" "$uuid")
      local response
      response=$(curl -s --max-time 10 "$status_url" || echo "error")
      if [[ "$response" =~ completed|success|OK ]]; then
        echo "[SUCCESS] Task $uuid completed."
      else
        TASK_MAP["$uuid"]=$((TASK_MAP["$uuid"]+1))
        echo "[PENDING] Task $uuid still in progress (Attempt ${TASK_MAP["$uuid"]})."
        new_pending+=("$uuid")
      fi
    done
    PENDING_TASKS=("${new_pending[@]}")
    if [[ ${#PENDING_TASKS[@]} -eq 0 ]]; then
      echo "[INFO] All tasks in this batch completed."
      break
    fi
    echo "[INFO] Still waiting on ${#PENDING_TASKS[@]} tasks in this batch..."
  done

  if [[ ${#PENDING_TASKS[@]} -gt 0 ]]; then
    echo "[WARNING] Timeout reached. The following tasks are still pending:"
    for uuid in "${PENDING_TASKS[@]}"; do
      echo "$uuid"
    done
  fi
}

process_batch() {
  local batch=("${@}")
  local uuids=()
  for url in "${batch[@]}"; do
    if [[ ! "$url" =~ ^https?:// ]]; then
      echo "[WARNING] Invalid URL: $url"
      continue
    fi
    local uuid
    uuid=$(submit_task "$url")
    echo "Submitted: $url -> Task UUID: $uuid"
    uuids+=("$uuid")
  done
  echo "[INFO] Started polling for ${#uuids[@]} tasks..."
  poll_tasks "${uuids[@]}"
}

main() {
  parse_args "$@"
  extract_urls
  local total=${#URLS[@]}
  for ((i=0; i<total; i+=BATCH_SIZE)); do
    local batch=("${URLS[@]:i:BATCH_SIZE}")
    local batch_num=$((i/BATCH_SIZE+1))
    echo "[INFO] Processing batch $batch_num with ${#batch[@]} URLs..."
    process_batch "${batch[@]}"
  done
}

main "$@"