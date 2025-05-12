$ErrorActionPreference = "Stop"

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome=$AppHome.Parent
}
cd $AppHome

$UBERJAR = Join-Path $PWD "target\PulsarRPA.jar"
if (-not (Test-Path $UBERJAR)) {
    $SERVER_HOME = Join-Path $AppHome "pulsar-app\pulsar-master"
    Copy-Item (Join-Path $SERVER_HOME "target\PulsarRPA.jar") -Destination $UBERJAR
}

# Other Java options
$JAVA_OPTS = "$JAVA_OPTS -Dfile.encoding=UTF-8" # Use UTF-8

Write-Host "Starting PulsarRPA..."

# Uncomment the following lines to set environment variables
# $env:PROXY_ROTATION_URL = "https://your-proxy-provider.com/rotation-endpoint"
# $env:BROWSER_CONTEXT_MODE = "SEQUENTIAL"
# $env:BROWSER_CONTEXT_NUMBER = "2"
# $env:BROWSER_MAX_OPEN_TABS = "8"
# $env:BROWSER_DISPLAY_MODE = "HEADLESS"

# java -jar PulsarRPA.jar

Start-Process -NoNewWindow -Wait -FilePath "java" -ArgumentList ("$JAVA_OPTS", "-jar", "$UBERJAR")
