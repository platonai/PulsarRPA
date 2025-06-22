#!/bin/bash
# Helper script to extract brief summaries of response content
# This is used by test-curl-commands.sh to provide concise response previews

# Function to extract a meaningful brief from a response
# Parameters:
#   $1 - response file path
#   $2 - content type (optional)
#   $3 - size in bytes (optional)

# Possible Responses

## /api/commands/plain
#{
 #  "id" : "40c390d0-5133-405f-bb92-f728e5af5ab5",
 #  "statusCode" : 200,
 #  "event" : "fields",
 #  "isDone" : true,
 #  "pageStatusCode" : 200,
 #  "pageContentBytes" : 9254,
 #  "message" : "created,textContent,pageSummary,fields",
 #  "request" : null,
 #  "commandResult" : {
 #    "pageSummary" : "It appears that the provided page content does not contain any product description, key features, or specifications. Instead, it includes standard footer information typically found on an Amazon webpage, such as:  \n\n- **Conditions of Use** (likely a link to Amazon's terms and conditions)  \n- **Privacy Policy** (a link to Amazon's privacy practices)  \n- **Copyright notice** (© 1996-2025, Amazon.com, Inc. or its affiliates)  \n\nIf you were looking for a product summary, please provide the actual product description or relevant content from the page. Otherwise, this appears to be just a generic footer section. Let me know how I can assist further!",
 #    "fields" : "```json\n{\n  \"product_name\": null,\n  \"price\": null,\n  \"ratings\": null\n}\n```",
 #    "links" : null,
 #    "xsqlResultSet" : null
 #  },
 #  "instructResults" : [ {
 #    "name" : "pageSummary",
 #    "statusCode" : 200,
 #    "result" : "It appears that the provided page content does not contain any product description, key features, or specifications. Instead, it includes standard footer information typically found on an Amazon webpage, such as:  \n\n- **Conditions of Use** (likely a link to Amazon's terms and conditions)  \n- **Privacy Policy** (a link to Amazon's privacy practices)  \n- **Copyright notice** (© 1996-2025, Amazon.com, Inc. or its affiliates)  \n\nIf you were looking for a product summary, please provide the actual product description or relevant content from the page. Otherwise, this appears to be just a generic footer section. Let me know how I can assist further!",
 #    "instruct" : null
 #  }, {
 #    "name" : "fields",
 #    "statusCode" : 200,
 #    "result" : "```json\n{\n  \"product_name\": null,\n  \"price\": null,\n  \"ratings\": null\n}\n```",
 #    "instruct" : null
 #  } ],
 #  "createTime" : "2025-06-20T16:53:29.279747371Z",
 #  "lastModifiedTime" : "2025-06-20T16:54:18.435611348Z",
 #  "finishTime" : "2025-06-20T16:54:18.435611453Z",
 #  "status" : "OK",
 #  "pageStatus" : "OK"
 #}

## /api/x/e
 #```json
 #{
 #  "id" : "5f84b913-5605-42ec-8ade-0d24d3abc39e",
 #  "statusCode" : 200,
 #  "pageStatusCode" : 200,
 #  "pageContentBytes" : 9254,
 #  "isDone" : true,
 #  "resultSet" : [ {
 #    "llm_extracted_data" : "{}",
 #    "url" : "https://www.amazon.com/dp/B0C1H26C46",
 #    "title" : "Huawei P60 Pro Dual SIM 8GB + 256GB Global Model MNA-LX9 Factory Unlocked Mobile Cellphone - Black",
 #    "img" : "https://m.media-amazon.com/images/I/617tZOVUc8L._AC_SL1200_.jpg"
 #  } ],
 #  "event" : "onLoaded",
 #  "createTime" : "2025-06-20T16:31:26.493956692Z",
 #  "lastModifiedTime" : "2025-06-20T16:32:02.500761711Z",
 #  "finishTime" : "2025-06-20T16:32:02.500727452Z",
 #  "pageStatus" : "OK",
 #  "status" : "OK"
 #}
 #```

extract_response_brief() {
  local response_file="$1"
  local content_type="${2:-text/plain}"
  local size_bytes="${3:-0}"

  # Handle empty file case first
  if [[ ! -s "$response_file" || "$size_bytes" -eq 0 ]]; then
    echo "Empty response"
    return
  fi

  local brief=""

  # Detect response type for better formatting
  if [[ "$content_type" =~ application/json || $(head -c 1 "$response_file") == "{" || $(head -c 1 "$response_file") == "[" ]]; then
    # Check if it's an array first
    local first_char=$(head -c 1 "$response_file")
    if [[ "$first_char" == "[" ]]; then
      # For arrays, count items using jq if available
      if command -v jq &>/dev/null; then
        local array_count=$(jq length 2>/dev/null < "$response_file")
        if [[ $? -eq 0 && -n "$array_count" ]]; then
          brief="Array with $array_count item(s)"
        else
          brief="JSON Array"
        fi
      else
        brief="JSON Array"
      fi
    else
      # For objects, extract keys and primary values
      if command -v jq &>/dev/null; then
        # Get the keys first
        local keys=$(jq -r 'keys | join(", ")' 2>/dev/null < "$response_file")
        if [[ $? -eq 0 && -n "$keys" ]]; then
          brief="$keys"

          # Try to get a meaningful value for context
          for field in "status" "message" "error" "name" "title" "result" "price"; do
            local value=$(jq -r --arg f "$field" '.[$f] | select(. != null and . != "" and . != false)' 2>/dev/null < "$response_file")
            if [[ $? -eq 0 && -n "$value" && "$value" != "null" ]]; then
              brief="$brief → $value"
              break
            fi
          done
        else
          # Fallback to simpler extraction
          brief=$(grep -o '{[^{}]*}' "$response_file" | head -1)
        fi
      else
        # Fallback if jq is not available
        brief=$(grep -o '{[^{}]*}' "$response_file" | head -1)
      fi
    fi
  elif [[ "$content_type" =~ text/html ]]; then
    # HTML response - extract title and heading
    brief=$(grep -o '<title>[^<]*</title>' "$response_file" 2>/dev/null |
            sed 's/<title>\(.*\)<\/title>/\1/' | head -1)

    if [[ -z "$brief" ]]; then
      brief=$(grep -o '<h1>[^<]*</h1>' "$response_file" 2>/dev/null |
              sed 's/<h1>\(.*\)<\/h1>/\1/' | head -1)
    fi

    [[ -z "$brief" ]] && brief="HTML content (no title/heading found)"
  else
    # Plain text or other format - get first meaningful line
    brief=$(grep -v '^\s*$' "$response_file" | head -1 | tr -d '\n\r')
  fi

  # Format the brief - handle empty response and truncation
  if [[ -z "$brief" && "$size_bytes" -gt 0 ]]; then
    echo "Response: $size_bytes bytes (cannot extract preview)"
  elif [[ ${#brief} -gt 120 ]]; then
    # Use exact truncation to match test expectations
    echo "${brief:0:117}..."
  else
    echo "$brief"
  fi
}

# Command-line use case (if script is called directly)
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <response_file> [content_type] [size_bytes]"
    exit 1
  fi

  response_file="$1"
  content_type="${2:-text/plain}"
  size_bytes="${3:-0}"

  if [[ ! -f "$response_file" ]]; then
    echo "Error: File not found - $response_file"
    exit 1
  fi

  extract_response_brief "$response_file" "$content_type" "$size_bytes"
fi
