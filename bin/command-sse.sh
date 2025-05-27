#!/usr/bin/env bash

set -e

# Amazon product URL to analyze
COMMAND='
Go to https://www.amazon.com/dp/B0C1H26C46
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
'

# API Endpoints
API_BASE="http://localhost:8182"
COMMAND_ENDPOINT="$API_BASE/api/commands/plain?mode=async"

# Send command to server and get command ID
echo "Sending command to server..."
COMMAND_ID=$(curl -s -X POST \
  -H "Content-Type: text/plain" \
  --data "$COMMAND" \
  "$COMMAND_ENDPOINT")

echo "Command ID: $COMMAND_ID"

# Connect to SSE stream using curl
echo "Connecting to SSE stream..."
SSE_URL="$API_BASE/api/commands/$COMMAND_ID/stream"

# Process the SSE stream with curl
# The -N flag disables buffering and --no-buffer ensures immediate output
# Process the SSE stream with curl
isDone=false
SSE_FIFO=$(mktemp -u)
mkfifo "$SSE_FIFO"

curl -N --no-buffer -H "Accept: text/event-stream" "$SSE_URL" > "$SSE_FIFO" &
CURL_PID=$!
echo "$CURL_PID" > /tmp/command_sse_curl_pid.txt

# Wait for the curl process to start
while read -r line < "$SSE_FIFO"; do
  if $isDone; then
    echo "done: $line"
  fi

  # If isDone is true and we've reached this point, break the loop
  if $isDone && [[ "$line" =~ \} || -z "$line" ]]; then
    echo "Task completed. Breaking the loop."
    break
  fi

  # Skip empty lines and lines starting with ":" (comments in SSE)
  if [[ -z "$line" || "$line" == :* ]]; then
    continue
  fi

  # Extract the data from SSE format (lines starting with "data:")
  if [[ "$line" == data:* ]]; then
    echo $line

    # Check if response contains "isDone.*true"
    if [[ "$data" =~ isDone.*true ]]; then
      isDone=true
    fi
  fi
done

# Clean up the FIFO
rm -f "$SSE_FIFO"
echo "Finished command-sse.sh script."
