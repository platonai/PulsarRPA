#!/usr/bin/env pwsh
#
# scrape-async.ps1
#
# Description:
#   Asynchronously submits scraping tasks for URLs extracted from a seeds file (default: seeds.txt),
#   then polls for completion using a UUID returned by the server. Processes URLs in batches (default: 20 per batch),
#   submitting and polling each batch until all are complete before moving to the next batch.
#
# Features:
#   - Accepts input parameters for seeds file, batch size, polling delay, max attempts, and max URLs
#   - Extracts HTTP(S) URLs from the specified seeds file (supports embedded URLs in text)
#   - Submits each URL as a scraping job via REST API
#   - Polls results in batches of 10 tasks per polling cycle
#   - Handles task timeouts and success status detection
#   - Processes URLs in user-defined batches, only moving to the next batch when the current one completes
#
# Dependencies:
#   - PowerShell 7+ (pwsh)
#   - Running backend service at http://localhost:8182
#
# Usage:
#   1. Place URLs in a seeds file (default: seeds.txt, one per line or embedded in text)
#   2. Run the script with optional parameters:
#        pwsh .\scrape-async.ps1 [-SeedsFile "myseeds.txt"] [-BatchSize 20] [-MaxAttempts 30] [-DelaySeconds 5] [-MaxUrls 200]
#
#   Example:
#        pwsh .\scrape-async.ps1 -SeedsFile "urls.txt" -BatchSize 10 -MaxAttempts 40 -DelaySeconds 3 -MaxUrls 100
#

param(
    [string]$SeedsFile = "seeds.txt",
    [int]$BatchSize = 20,
    [int]$MaxAttempts = 30,
    [int]$DelaySeconds = 5,
    [int]$MaxUrls = 200
)

# Configuration
$baseUrl = "http://localhost:8182/api/x/s"
$statusUrlTemplate = "http://localhost:8182/api/x/status?uuid={0}"
$maxPollingTasks = 10

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

# Search for seeds.txt in the current directory and get the full path
$seedsFile = Get-ChildItem -Path . -Filter $SeedsFile -Recurse -ErrorAction SilentlyContinue
if (-not $seedsFile) {
    Write-Output "[ERROR] $SeedsFile 文件不存在。"
    exit 1
}

# Use regex to extract URL substrings from each line
$urls = Get-Content -Path $seedsFile.FullName | ForEach-Object {
    $match = [regex]::Match($_, 'https?://[^\s/$.?#].[^\s]*')
    if ($match.Success) {
        $match.Value
    }
} | Where-Object { $_ -ne $null } | Select-Object -Unique

if ($urls.Count -gt $MaxUrls) {
    Write-Output "[WARNING] $SeedsFile 中包含 $($urls.Count) 个链接，将只使用前 $MaxUrls 个链接。"
    $urls = $urls[0..($MaxUrls - 1)]
}

# Output debug info
Write-Output "[INFO] 已提取 $($urls.Count) 个有效链接："
# $urls | ForEach-Object { "$_" }

if ($urls.Count -eq 0) {
    Write-Output "[ERROR] $SeedsFile 中未找到有效的 HTTP(S) 链接。"
    exit 1
}

# Function to process a batch of URLs
function Process-Batch {
    param(
        [array]$batchUrls
    )
    $taskMap = @{}
    foreach ($url in $batchUrls) {
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
    $pendingTasks = New-Object System.Collections.ArrayList
    [void]$pendingTasks.AddRange($taskMap.Keys)
    for ($attempt = 0; $attempt -lt $MaxAttempts; $attempt++) {
        Start-Sleep -Seconds $DelaySeconds
        $batch = $pendingTasks | Select-Object -First $maxPollingTasks
        foreach ($uuid in $batch) {
            $statusUrl = $statusUrlTemplate -f $uuid
            try {
                $response = Invoke-RestMethod -Method GET -Uri $statusUrl -TimeoutSec 10
                if ($response -match "completed|success|OK") {
                    Write-Output "[SUCCESS] Task $uuid completed."
                    [void]$pendingTasks.Remove($uuid)
                } else {
                    $taskMap[$uuid][1] += 1
                    Write-Output "[PENDING] Task $uuid still in progress (Attempt $($taskMap[$uuid][1]))."
                }
            } catch {
                Write-Output "[ERROR] Failed to poll task ${uuid}: $_"
            }
        }
        if ($pendingTasks.Count -eq 0) {
            Write-Output "`n[INFO] All tasks in this batch completed.`n"
            break
        }
        Write-Output "`n[INFO] Still waiting on $($pendingTasks.Count) tasks in this batch...`n"
    }
    if ($pendingTasks.Count -gt 0) {
        Write-Output "`n[WARNING] Timeout reached. The following tasks in this batch are still pending:"
        $pendingTasks | ForEach-Object { "$_" }
    }
}

# Main batching loop
for ($i = 0; $i -lt $urls.Count; $i += $BatchSize) {
    $batchUrls = $urls[$i..([Math]::Min($i + $BatchSize - 1, $urls.Count - 1))]
    Write-Output "`n[INFO] Processing batch $([Math]::Floor($i / $BatchSize) + 1) with $($batchUrls.Count) URLs..."
    Process-Batch -batchUrls $batchUrls
}
