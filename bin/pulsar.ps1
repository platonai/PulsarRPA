$ErrorActionPreference = "Stop"

$BIN = Split-Path -Parent $MyInvocation.MyCommand.Definition
$APP_HOME = (Resolve-Path "$BIN/..").Path

$UBERJAR = Join-Path $PWD "target\PulsarRPA.jar"
if (-not (Test-Path $UBERJAR)) {
    $SERVER_HOME = Join-Path $APP_HOME "pulsar-app\pulsar-master"
    Copy-Item (Join-Path $SERVER_HOME "target\PulsarRPA.jar") -Destination $UBERJAR
}

# Other Java options
$JAVA_OPTS = "$JAVA_OPTS -Dfile.encoding=UTF-8" # Use UTF-8

Write-Host "Using these JAVA_OPTS: $JAVA_OPTS"

Start-Process -NoNewWindow -Wait -FilePath "java" -ArgumentList ("$JAVA_OPTS", "-jar", "$UBERJAR")
