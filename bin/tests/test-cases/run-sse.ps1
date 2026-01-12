#!/usr/bin/env pwsh

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# 导入通用工具脚本
. $AppHome\bin\common\Util.ps1

Fix-Encoding-UTF8

# 设置错误处理
$ErrorActionPreference = "Stop"

# 自然语言命令内容
$COMMAND = @'
Go to https://www.amazon.com/dp/B08PP5MSVB

After browser launch:
  - clear browser cookies
  - go to https://www.amazon.com/
  - wait for 5 seconds
  - click the first product link
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
'@

# API 接口
$API_BASE = "http://localhost:8182"
$COMMAND_ENDPOINT = "$API_BASE/api/commands/plain?mode=async"

# 发送命令
Write-Host "Sending command to server..."

try {
  $response = Invoke-RestMethod -Uri $COMMAND_ENDPOINT -Method POST -Body $COMMAND -ContentType "text/plain"
  $COMMAND_ID = $response
} catch {
  Write-Error "Error: Failed to get command ID from server. $_"
  exit 1
}

# 检查 command ID 是否有效
if ([string]::IsNullOrWhiteSpace($COMMAND_ID)) {
  Write-Error "Error: Failed to get command ID from server."
  exit 1
}

Write-Host "Command ID: $COMMAND_ID"

# SSE 监听地址
$SSE_URL = "$API_BASE/api/commands/$COMMAND_ID/stream"
$RESULT_URL = "$API_BASE/api/commands/$COMMAND_ID/result"

Write-Host "Connecting to SSE stream..."

# 确保 HttpClient 类型可用（Windows PowerShell 需显式加载）
if (-not ("System.Net.Http.HttpClient" -as [type])) {
  Add-Type -AssemblyName System.Net.Http
}

# 创建 HTTP 客户端用于 SSE
$httpClient = New-Object System.Net.Http.HttpClient
$httpClient.Timeout = [System.TimeSpan]::FromMinutes(30)

# 设置 SSE 请求头
$request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Get, $SSE_URL)
$request.Headers.Add("Accept", "text/event-stream")
$request.Headers.Add("Cache-Control", "no-cache")
$request.Headers.TryAddWithoutValidation("Accept-Charset", "utf-8") | Out-Null

# 清理函数
function Cleanup {
  Write-Host "Cleaning up resources..."
  if ($httpClient) {
    $httpClient.Dispose()
  }
  if ($response) {
    $response.Dispose()
  }
  if ($stream) {
    $stream.Dispose()
  }
  if ($reader) {
    $reader.Dispose()
  }
}

# 获取最终结果的函数
function Get-FinalResult {
  param($CommandId)

  Write-Host "`n=== FETCHING FINAL RESULT ===" -ForegroundColor Green

  # Print payload safely across Windows PowerShell 5.1 and pwsh.
  function Write-TextBlock {
    param([Parameter(Mandatory=$true)][string]$Text)
    Write-Output $Text | Out-Host
  }

  try {
    $resultRequest = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Get, $RESULT_URL)
    $resultRequest.Headers.TryAddWithoutValidation("Accept", "application/json") | Out-Null
    $resultRequest.Headers.TryAddWithoutValidation("Accept-Charset", "utf-8") | Out-Null

    $resultResponse = $httpClient.SendAsync($resultRequest).Result
    if (-not $resultResponse.IsSuccessStatusCode) {
      throw "HTTP request failed with status: $($resultResponse.StatusCode)"
    }

    $result = $resultResponse.Content.ReadAsStringAsync().Result

    Write-Host "`n=== FINAL RESULT ===" -ForegroundColor Yellow
    Write-Host "Command ID: $CommandId" -ForegroundColor Cyan
    Write-Host "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') UTC" -ForegroundColor Cyan
    Write-Host "Status: COMPLETED" -ForegroundColor Green

    # Normalise to a printable string (pretty JSON when possible).
    if ($result -is [string]) {
      try {
        $jsonResult = $result | ConvertFrom-Json
        Write-Host "`nStructured Result:" -ForegroundColor Magenta
        $jsonText = $jsonResult | ConvertTo-Json -Depth 10
        Write-TextBlock -Text $jsonText
      } catch {
        Write-Host "`nRaw Result:" -ForegroundColor Magenta
        Write-TextBlock -Text $result
      }
    } else {
      Write-Host "`nResult Object:" -ForegroundColor Magenta
      $jsonText = $result | ConvertTo-Json -Depth 10
      Write-TextBlock -Text $jsonText
    }

    Write-Host "`n=== END OF RESULT ===" -ForegroundColor Yellow

  } catch {
    Write-Warning "Failed to fetch final result: $_"
    Write-Host "You can manually check the result at: $RESULT_URL"
  } finally {
    if ($resultResponse) { $resultResponse.Dispose() }
  }
}

# 注册清理函数
try {
  # 发送请求并获取响应流
  $response = $httpClient.SendAsync($request, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).Result

  if (-not $response.IsSuccessStatusCode) {
    throw "HTTP request failed with status: $($response.StatusCode)"
  }

  $stream = $response.Content.ReadAsStreamAsync().Result
  $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)

  # SSE 主循环
  $isDone = $false
  $lastUpdate = ""

  Write-Host "Reading SSE stream..."

  while (-not $reader.EndOfStream -and -not $isDone) {
    $line = $reader.ReadLine()

    # 跳过空行或注释
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith(":")) {
      continue
    }

    # 提取 data 字段
    if ($line.StartsWith("data:")) {
      $data = $line.Substring(5).Trim()

      # 避免重复打印相同的更新
      if ($data -ne $lastUpdate) {
        Write-Host "SSE update: $data" -ForegroundColor Gray
        $lastUpdate = $data
      }

      # Check for completion indicators: "done": true or "isDone": true
      if ($data -match '"done"\s*:\s*true' -or $data -match '"isDone"\s*:\s*true') {
        $isDone = $true
        Write-Host "`nTask completed! Fetching final result..." -ForegroundColor Green
        Start-Sleep -Seconds 2  # 等待服务器完全处理完成

        # 获取并打印最终结果
        Get-FinalResult -CommandId $COMMAND_ID
        break
      }
    }

    # 添加小延迟以避免过度占用 CPU
    Start-Sleep -Milliseconds 50
  }

  if (-not $isDone) {
    Write-Warning "SSE stream ended but task may not be completed."
    Write-Host "Attempting to fetch result anyway..."
    Get-FinalResult -CommandId $COMMAND_ID
  }

} catch {
  Write-Error "Error during SSE processing: $_"
  Write-Host "Attempting to fetch any available result..."
  Get-FinalResult -CommandId $COMMAND_ID
} finally {
  Cleanup
}

Write-Host "`nFinished run-sse.ps1 script." -ForegroundColor Green
