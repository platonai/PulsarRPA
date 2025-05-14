#!/usr/bin/env pwsh
#
# scrape-async.ps1
#
# Description:
#   Asynchronously submits scraping tasks for URLs extracted from seeds.txt,
#   then polls for completion using a UUID returned by the server.
#
# Features:
#   - Extracts HTTP(S) URLs from seeds.txt (supporting embedded URLs in text)
#   - Submits each URL as a scraping job via REST API
#   - Polls results in batches of 10 tasks
#   - Handles task timeouts and success status detection
#
# Dependencies:
#   - PowerShell 7+ (pwsh)
#   - Running backend service at http://localhost:8182
#
# Usage:
#   1. Place URLs in seeds.txt (one per line)
#   2. Run the script: `pwsh .\scrape-async.ps1`
#

# Configuration
$baseUrl = "http://localhost:8182/api/scrape/submit"
$statusUrlTemplate = "http://localhost:8182/api/scrape/status?uuid={0}"
$maxPollingTasks = 10
$delaySeconds = 5
$maxAttempts = 30

# SQL template with placeholder {url}
$sqlTemplate = @'
select
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
'@

# Check if seeds.txt exists
if (-not (Test-Path "seeds.txt")) {
    Write-Output "[ERROR] seeds.txt 文件不存在。"
    exit 1
}

# Use regex to extract URL substrings from each line
$maxUrls = 200
$urls = Get-Content -Path "seeds.txt" | ForEach-Object {
    $match = [regex]::Match($_, 'https?://[^\s/$.?#].[^\s]*')
    if ($match.Success) {
        $match.Value
    }
} | Where-Object { $_ -ne $null } | Select-Object -Unique

if ($urls.Count -gt $maxUrls) {
    Write-Output "[WARNING] seeds.txt 中包含 $($urls.Count) 个链接，将只使用前 $maxUrls 个链接。"
    $urls = $urls[0..($maxUrls - 1)]
}

# Output debug info
Write-Output "[INFO] 已提取 $($urls.Count) 个有效链接："
# $urls | ForEach-Object { "$_" }

if ($urls.Count -eq 0) {
    Write-Output "[ERROR] seeds.txt 中未找到有效的 HTTP(S) 链接。"
    exit 1
}

# Store tasks: [uuid] => [url, attemptCount]
$taskMap = @{}

# Submit all tasks
foreach ($url in $urls) {
    # If url is not valid, skip
    if (-not ($url -match '^https?://')) {
        Write-Output "[WARNING] Invalid URL: $url"
        continue
    }

    $sql = $sqlTemplate.Replace('{url}', $url)

    $uuid = Invoke-RestMethod -Method POST -Uri $baseUrl -Headers @{"Content-Type" = "text/plain"} -Body $sql
    Write-Output "Submitted: $url -> Task UUID: $uuid"
    $taskMap[$uuid] = @( $url, 0 )
}

Write-Output "`n[INFO] Started polling for $($taskMap.Count) tasks...`n"

# Filter only pending tasks
$pendingTasks = New-Object System.Collections.ArrayList
[void]$pendingTasks.AddRange($taskMap.Keys)

# Start polling loop
for ($attempt = 0; $attempt -lt $maxAttempts; $attempt++) {
    Start-Sleep -Seconds $delaySeconds

    # Batch process up to $maxPollingTasks tasks
    $batch = $pendingTasks | Select-Object -First $maxPollingTasks

    foreach ($uuid in $batch) {
        $statusUrl = $statusUrlTemplate -f $uuid
        try {
            $response = Invoke-RestMethod -Method GET -Uri $statusUrl -TimeoutSec 10

            if ($response -match "completed|success|OK") {
                Write-Output "[SUCCESS] Task $uuid completed."
                [void]$pendingTasks.Remove($uuid)
            } else {
                # Increment attempt count
                $taskMap[$uuid][1] += 1
                Write-Output "[PENDING] Task $uuid still in progress (Attempt $($taskMap[$uuid][1]))."
            }
        } catch {
            Write-Output "[ERROR] Failed to poll task ${uuid}: $_"
        }
    }

    if ($pendingTasks.Count -eq 0) {
        Write-Output "`n[INFO] All tasks completed.`n"
        break
    }

    Write-Output "`n[INFO] Still waiting on $($pendingTasks.Count) tasks...`n"
}

if ($pendingTasks.Count -gt 0) {
    Write-Output "`n[WARNING] Timeout reached. The following tasks are still pending:"
    $pendingTasks | ForEach-Object { "$_" }
}
