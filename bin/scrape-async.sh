#!/usr/bin/env bash

# scrape-async.sh
#
# Description:
#   Asynchronously submits scraping tasks for URLs extracted from a seeds file (default: seeds.txt),
#   then polls for completion using a UUID returned by the server. Processes URLs in batches (default: 20 per batch),
#   submitting and polling each batch until all are complete before moving to the next batch.
#
# Usage:
#   ./scrape-async.sh [-f seeds.txt] [-b 20] [-a 30] [-d 5] [-m 200]
#
#   -f: Seeds file (default: seeds.txt)
#   -b: Batch size (default: 20)
#   -a: Max attempts per batch (default: 30)
#   -d: Delay seconds between polls (default: 5)
#   -m: Max URLs to process (default: 200)
#

set -e

# Default parameters
SEEDS_FILE="seeds.txt"
BATCH_SIZE=20
MAX_ATTEMPTS=30
DELAY_SECONDS=5
MAX_URLS=200

# Parse options
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

if [[ ! -f "$SEEDS_FILE" ]]; then
  echo "[ERROR] $SEEDS_FILE 文件不存在。"
  exit 1
fi

# Extract URLs using grep and regex, unique, up to MAX_URLS
mapfile -t URLS < <(grep -Eo 'https?://[^[:space:]/$.?#][^[:space:]]*' "$SEEDS_FILE" | awk '!seen[$0]++' | head -n "$MAX_URLS")

if [[ ${#URLS[@]} -eq 0 ]]; then
  echo "[ERROR] $SEEDS_FILE 中未找到有效的 HTTP(S) 链接。"
  exit 1
fi

echo "[INFO] 已提取 ${#URLS[@]} 个有效链接："

# Function to process a batch of URLs
process_batch() {
  local batch=("${@}")
  declare -A TASK_MAP
  declare -a PENDING_TASKS

  # Submit all tasks in the batch
  for url in "${batch[@]}"; do
    if [[ ! "$url" =~ ^https?:// ]]; then
      echo "[WARNING] Invalid URL: $url"
      continue
    fi
    sql="${SQL_TEMPLATE//\{url\}/$url}"
    uuid=$(curl -s -X POST -H "Content-Type: text/plain" --data "$sql" "$BASE_URL")
    echo "Submitted: $url -> Task UUID: $uuid"
    TASK_MAP["$uuid"]="$url:0"
    PENDING_TASKS+=("$uuid")
  done

  echo -e "\n[INFO] Started polling for ${#PENDING_TASKS[@]} tasks...\n"

  for ((attempt=0; attempt<MAX_ATTEMPTS; attempt++)); do
    sleep "$DELAY_SECONDS"
    local batch_uuids=("${PENDING_TASKS[@]:0:$MAX_POLLING_TASKS}")
    for uuid in "${batch_uuids[@]}"; do
      status_url=$(printf "$STATUS_URL_TEMPLATE" "$uuid")
      response=$(curl -s --max-time 10 "$status_url" || echo "error")
      if [[ "$response" =~ completed|success|OK ]]; then
        echo "[SUCCESS] Task $uuid completed."
        # Remove from pending
        PENDING_TASKS=("${PENDING_TASKS[@]/$uuid}")
      else
        # Increment attempt count (not strictly needed in Bash, but for parity)
        IFS=: read url count <<< "${TASK_MAP[$uuid]}"
        count=$((count+1))
        TASK_MAP["$uuid"]="$url:$count"
        echo "[PENDING] Task $uuid still in progress (Attempt $count)."
      fi
    done
    if [[ ${#PENDING_TASKS[@]} -eq 0 ]]; then
      echo -e "\n[INFO] All tasks in this batch completed.\n"
      break
    fi
    echo -e "\n[INFO] Still waiting on ${#PENDING_TASKS[@]} tasks in this batch...\n"
  done

  if [[ ${#PENDING_TASKS[@]} -gt 0 ]]; then
    echo -e "\n[WARNING] Timeout reached. The following tasks in this batch are still pending:"
    for uuid in "${PENDING_TASKS[@]}"; do
      echo "$uuid"
    done
  fi
}

# Main batching loop
total=${#URLS[@]}
for ((i=0; i<total; i+=BATCH_SIZE)); do
  batch=("${URLS[@]:i:BATCH_SIZE}")
  batch_num=$((i/BATCH_SIZE+1))
  echo -e "\n[INFO] Processing batch $batch_num with ${#batch[@]} URLs..."
  process_batch "${batch[@]}"
done 