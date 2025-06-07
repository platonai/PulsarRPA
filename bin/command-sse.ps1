# bin\command-sse.ps1

$ErrorActionPreference = "Stop"

# Natural language command content
$COMMAND = @"
Go to https://www.amazon.com/dp/B0C1H26C46
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
"@

# API endpoint
$API_BASE = "http://localhost:8182"
$COMMAND_ENDPOINT = "$API_BASE/api/commands/plain?mode=async"

# Send command
Write-Host "Sending command to server..."
$COMMAND_ID = Invoke-RestMethod -Uri $COMMAND_ENDPOINT -Method Post -Body $COMMAND -ContentType "text/plain"

if ([string]::IsNullOrWhiteSpace($COMMAND_ID)) {
    Write-Host "Error: Failed to get command ID from server."
    exit 1
}
Write-Host "Command ID: $COMMAND_ID"

# SSE listen URL
$SSE_URL = "$API_BASE/api/commands/$COMMAND_ID/stream"

# Start curl process for SSE
Write-Host "Connecting to SSE stream..."
$curlProc = Start-Process -FilePath "curl" -ArgumentList @("-N", "--no-buffer", "-H", "Accept: text/event-stream", $SSE_URL) -NoNewWindow -RedirectStandardOutput "sse_output.txt" -PassThru

# Cleanup function
function Cleanup {
    if ($curlProc -and !$curlProc.HasExited) {
        $curlProc.Kill()
    }
    if (Test-Path "sse_output.txt") {
        Remove-Item "sse_output.txt"
    }
}
Register-EngineEvent PowerShell.Exiting -Action { Cleanup }

$isDone = $false
$reader = [System.IO.StreamReader]::new("sse_output.txt")
while (-not $reader.EndOfStream) {
    $line = $reader.ReadLine()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith(":")) {
        continue
    }
    if ($line.StartsWith("data:")) {
        $data = $line.Substring(5).Trim()
        Write-Host "SSE update: $data"
        if ($data -match '"isDone"\s*:\s*true') {
            $isDone = $true
            Start-Sleep -Seconds 1
        }
    }
    if ($isDone -and ($line -match "\}" -or [string]::IsNullOrWhiteSpace($line))) {
        Write-Host "Task completed. Breaking the loop."
        break
    }
}
$reader.Close()
Start-Sleep -Seconds 1

Write-Host "Finished command-sse.ps1 script."
Cleanup