param(
    [string]$RemoteUser = "vincent",
    [string]$RemoteHost = "platonai.cn",
    [string]$RemotePath = "~/platonai.cn/repo/ai/platon/pulsar/"
)

# Find the first parent directory containing the VERSION file
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($null -ne $AppHome -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = $AppHome.Parent
}

if ($null -eq $AppHome) {
    Write-Error "Could not find VERSION file in any parent directory"
    exit 1
}

Set-Location $AppHome

# If pulsar-app/pulsar-master/target/PulsarRPA.jar exists, copy it to remote
$PulsarRPAPath = "$AppHome/pulsar-app/pulsar-master/target/PulsarRPA.jar"
if (Test-Path $PulsarRPAPath) {
    $DestinationPath = "${RemoteUser}@${RemoteHost}:${RemotePath}"

    Write-Host "Copying $PulsarRPAPath to $DestinationPath..."

    try {
        # Check if SSH connection works
        $testResult = Invoke-Expression "ssh ${RemoteUser}@${RemoteHost} 'echo Connection test'"
        if ($LASTEXITCODE -ne 0) {
            throw "SSH connection test failed"
        }

        # Ensure destination directory exists
        $mkdirResult = Invoke-Expression "ssh ${RemoteUser}@${RemoteHost} 'mkdir -p $RemotePath'"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to create remote directory"
        }

        # Copy the file
        $copyResult = Invoke-Expression "scp $PulsarRPAPath $DestinationPath"
        if ($LASTEXITCODE -ne 0) {
            throw "SCP command failed with exit code $LASTEXITCODE"
        }

        Write-Host "File copied successfully to $DestinationPath" -ForegroundColor Green
    }
    catch {
        Write-Error "Error: $_"
        exit 1
    }
} else {
    Write-Warning "$PulsarRPAPath does not exist"
    exit 1
}
