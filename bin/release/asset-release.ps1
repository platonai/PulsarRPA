param(
    [string]$RemoteUser = "vincent",
    [string]$RemoteHost = "platonai.cn",
    [string]$RemotePath = "~/platonic.fun/repo/ai/platon/pulsar/"
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

$Version=(Get-Content "$AppHome/VERSION" -TotalCount 1) -replace "-SNAPSHOT", ""

# If pulsar-app/pulsar-master/target/PulsarRPA.jar exists, copy it to remote
$PulsarRPAPath = "$AppHome/pulsar-app/pulsar-master/target/PulsarRPA.jar"
if (Test-Path $PulsarRPAPath) {
    $DestinationPath = "${RemoteUser}@${RemoteHost}:${RemotePath}PulsarRPA-${Version}.jar"

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

        # Create a symbolic link to the latest version
        $linkResult = Invoke-Expression "ssh ${RemoteUser}@${RemoteHost} 'ln -sf PulsarRPA-${Version}.jar PulsarRPA.jar'"
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to create symbolic link"
        }
        Write-Host "Symbolic link created successfully" -ForegroundColor Green

        # List the remote directory contents
        # e.g. ssh vincent@platonai.cn ls -l ~/platonic.fun/repo/ai/platon/pulsar/
        $listResult = Invoke-Expression "ssh ${RemoteUser}@${RemoteHost} 'ls -l $RemotePath'"
    }
    catch {
        Write-Error "Error: $_"
        exit 1
    }
} else {
    Write-Warning "$PulsarRPAPath does not exist"
    exit 1
}
