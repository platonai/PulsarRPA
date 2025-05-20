#!/usr/bin/env pwsh

# Default parameters
$SEEDS_FILE = "seeds.txt"
$BATCH_SIZE = 5
$MAX_ATTEMPTS = 30
$DELAY_SECONDS = 5
$MAX_URLS = 20
$BASE_URL = "http://localhost:8182/api/x/s"
$STATUS_URL_TEMPLATE = "http://localhost:8182/api/x/status?uuid={0}"
$MAX_POLLING_TASKS = 10

$SQL_TEMPLATE = @"
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
"@

function Parse-Args {
    param (
        [string]$f,
        [int]$b,
        [int]$a,
        [int]$d,
        [int]$m
    )

    if ($f) { $script:SEEDS_FILE = $f }
    if ($b) { $script:BATCH_SIZE = $b }
    if ($a) { $script:MAX_ATTEMPTS = $a }
    if ($d) { $script:DELAY_SECONDS = $d }
    if ($m) { $script:MAX_URLS = $m }
}

function Extract-Urls {
    if (-not (Test-Path $SEEDS_FILE)) {
        Write-Error "[ERROR] $SEEDS_FILE does not exist."
        exit 1
    }

    $URLS = @(Select-String -Path $SEEDS_FILE -Pattern 'https?://[^\s/$.?#][^\s]*' |
              ForEach-Object { $_.Matches.Value } |
              Sort-Object -Unique |
              Select-Object -First $MAX_URLS)

    if ($URLS.Count -eq 0) {
        Write-Error "[ERROR] No valid HTTP(S) links found in $SEEDS_FILE."
        exit 1
    }

    Write-Host "[INFO] Extracted $($URLS.Count) valid links."
    return $URLS
}

function Submit-Task {
    param ([string]$url)

    $sql = $SQL_TEMPLATE -replace '\{url\}', $url
    $response = Invoke-RestMethod -Uri $BASE_URL -Method Post -ContentType "text/plain" -Body $sql
    return $response
}

function Poll-Tasks {
    param ([array]$uuids)

    Write-Host "First uuid: $($uuids[0]), Total uuids: $($uuids.Count)"
    if ($uuids.Count -gt 1) { Write-Host "$($uuids[1])" }
    if ($uuids.Count -gt 2) { Write-Host "$($uuids[2])" }

    $TASK_MAP = @{}
    $PENDING_TASKS = $uuids.Clone()

    foreach ($uuid in $PENDING_TASKS) {
        $TASK_MAP[$uuid] = 0
    }

    for ($attempt = 0; $attempt -lt $MAX_ATTEMPTS; $attempt++) {
        Start-Sleep -Seconds $DELAY_SECONDS
        $batch_uuids = $PENDING_TASKS[0..([Math]::Min($MAX_POLLING_TASKS-1, $PENDING_TASKS.Count-1))]
        $new_pending = @()

        foreach ($uuid in $batch_uuids) {
            Write-Host "[INFO] Polling for task $uuid..."

            $status_url = $STATUS_URL_TEMPLATE -f $uuid
            try {
                $response = Invoke-RestMethod -Uri $status_url -TimeoutSec 10
                if ($response -match "completed|success|OK") {
                    Write-Host "[SUCCESS] Task $uuid completed."
                } else {
                    $TASK_MAP[$uuid]++
                    Write-Host "[PENDING] Task $uuid still in progress (Attempt $($TASK_MAP[$uuid]))."
                    $new_pending += $uuid
                }
            } catch {
                $TASK_MAP[$uuid]++
                Write-Host "[PENDING] Task $uuid still in progress (Attempt $($TASK_MAP[$uuid]))."
                $new_pending += $uuid
            }
        }

        $PENDING_TASKS = $new_pending
        if ($PENDING_TASKS.Count -eq 0) {
            Write-Host "[INFO] All tasks in this batch completed."
            break
        }

        Write-Host "[INFO] Still waiting on $($PENDING_TASKS.Count) tasks in this batch..."
    }

    if ($PENDING_TASKS.Count -gt 0) {
        Write-Host "[WARNING] Timeout reached. The following tasks are still pending:"
        foreach ($uuid in $PENDING_TASKS) {
            Write-Host $uuid
        }
    }
}

function Process-Batch {
    param ([array]$batch)

    $uuids = @()
    foreach ($url in $batch) {
        if ($url -notmatch '^https?://') {
            Write-Host "[WARNING] Invalid URL: $url"
            continue
        }

        $uuid = Submit-Task -url $url
        Write-Host "Submitted: $url -> Task UUID: $uuid"
        $uuids += $uuid
    }

    Write-Host "[INFO] Started polling for $($uuids.Count) tasks..."
    Poll-Tasks -uuids $uuids
}

function Main {
    param (
        [string]$f,
        [int]$b,
        [int]$a,
        [int]$d,
        [int]$m
    )

    Parse-Args -f $f -b $b -a $a -d $d -m $m
    $URLS = Extract-Urls
    $total = $URLS.Count

    for ($i = 0; $i -lt $total; $i += $BATCH_SIZE) {
        $end = [Math]::Min($i + $BATCH_SIZE - 1, $total - 1)
        $batch = $URLS[$i..$end]
        $batch_num = [Math]::Floor($i / $BATCH_SIZE) + 1

        Write-Host "[INFO] Processing batch $batch_num with $($batch.Count) URLs..."
        Process-Batch -batch $batch
    }
}

# Parse command line parameters
$params = @{}
for ($i = 0; $i -lt $args.Count; $i++) {
    if ($args[$i] -match '^-([fbadm])$' -and $i+1 -lt $args.Count) {
        $param = $matches[1]
        $value = $args[$i+1]
        $params[$param] = $value
        $i++
    }
}

# Call the main function with parsed parameters
Main -f $params['f'] -b $params['b'] -a $params['a'] -d $params['d'] -m $params['m']