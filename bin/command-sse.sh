#!/usr/bin/env bash

set -e

# 清理函数：在退出时删除 FIFO 并杀掉 curl 进程
cleanup() {
  [[ -p "$SSE_FIFO" ]] && rm -f "$SSE_FIFO"
  [[ -f /tmp/command_sse_curl_pid.txt ]] && rm -f /tmp/command_sse_curl_pid.txt
  [[ -n "$CURL_PID" ]] && kill "$CURL_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

# 自然语言命令内容
COMMAND='
Go to https://www.amazon.com/dp/B0C1H26C46
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
'

# API 接口
API_BASE="http://localhost:8182"
COMMAND_ENDPOINT="$API_BASE/api/commands/plain?mode=async"

# 发送命令
echo "Sending command to server..."
COMMAND_ID=$(curl -s -X POST \
  -H "Content-Type: text/plain" \
  --data "$COMMAND" \
  "$COMMAND_ENDPOINT")

# 检查 command ID 是否有效
if [[ -z "$COMMAND_ID" ]]; then
  echo "Error: Failed to get command ID from server."
  exit 1
fi
echo "Command ID: $COMMAND_ID"

# SSE 监听地址
SSE_URL="$API_BASE/api/commands/$COMMAND_ID/stream"
SSE_FIFO=$(mktemp -u)
mkfifo "$SSE_FIFO"

echo "Connecting to SSE stream..."
curl -N --no-buffer -H "Accept: text/event-stream" "$SSE_URL" > "$SSE_FIFO" &
CURL_PID=$!
echo "$CURL_PID" > /tmp/command_sse_curl_pid.txt

# SSE 主循环
isDone=0
while read -r line; do
  # 跳过空行或注释
  if [[ -z "$line" || "$line" == :* ]]; then
    continue
  fi

  # 提取 data 字段
  if [[ "$line" == data:* ]]; then
    data="${line#data:}"
    data="${data#"${data%%[![:space:]]*}"}"  # 去除前导空白
    echo "SSE update: $data"

    # 检查是否已完成
    if [[ "$data" =~ \"isDone\"[[:space:]]*:[[:space:]]*true ]]; then
      isDone=1
      sleep 1 # 等待一秒以确保所有数据都已接收
    fi
  fi

  if [[ $isDone -eq 1 && ( "$line" =~ \} || -z "$line" ) ]]; then
    echo "Task completed. Breaking the loop."
    break
  fi
done < "$SSE_FIFO"

sleep 1 # 确保所有数据都已处理完毕

if ps -p "$CURL_PID" >/dev/null 2>&1; then
  kill "$CURL_PID"
  wait "$CURL_PID"
fi
rm -f "$SSE_FIFO"

echo "Finished command-sse.sh script."
