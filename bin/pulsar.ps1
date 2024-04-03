#!bin/powershell

# Run pulsar-app/pulsar-master/target/pulsar-master-1.12.3-SNAPSHOT.jar

$scriptPath = $MyInvocation.MyCommand.Path
$scriptDir = Split-Path $scriptPath
$APP_HOME = Split-Path $scriptDir

$scriptDir = $scriptDir.Replace("\", "/")
$scriptDir = $scriptDir.Replace("bin", "pulsar-app/pulsar-master/target")

# read file VERSION to get the version
$VERSION = Get-Content "$APP_HOME/VERSION"
$JAR="pulsar-master-$VERSION.jar"
$CLASS="ai.platon.pulsar.app.master.PulsarMasterKt"

Set-Location $scriptDir

# If mvn is not available, exit
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "mvn is not available, please install maven first"
    exit
}

$outputFile = "classpath.txt"
mvn dependency:build-classpath -Dmdep.outputFile="$outputFile"

$MODULE_CLASSPATH = Get-Content -Path $outputFile -Raw
$CLASSPATH = "${env:CLASSPATH};${MODULE_CLASSPATH}"
$CLASSPATH = "${CLASSPATH};${APP_HOME}\${M}\target\${JAR1}"

Set-Location $APP_HOME

$env:CLASSPATH = $CLASSPATH
java -jar $JAR $CLASS $args
