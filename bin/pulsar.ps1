#!bin/powershell

# Run pulsar-app/pulsar-master/target/pulsar-master-1.12.3-SNAPSHOT.jar
$MAVEN_HOME = "D:\Program Files\maven\apache-maven-3.8.8"
# $JAVA_HOME = "D:\Program Files\Java\jdk-11.0.12"

$MVN="$MAVEN_HOME/bin/mvn.cmd"
$JAVA="java"

$scriptPath = $MyInvocation.MyCommand.Path
$scriptDir = Split-Path $scriptPath
$APP_HOME = Split-Path $scriptDir
$MODULE = "pulsar-app/pulsar-master"

$scriptDir = $scriptDir.Replace("\", "/")
$JAR_DIR = $scriptDir.Replace("bin", "$MODULE/target")

# read file VERSION to get the version
$VERSION = Get-Content "$APP_HOME/VERSION"
$JAR="pulsar-master-$VERSION.jar"
$CLASS="ai.platon.pulsar.app.master.PulsarMasterKt"

# If mvn is not available, exit
if (-not (Get-Command $MVN -ErrorAction SilentlyContinue)) {
    Write-Host "mvn is not available, please install maven first"
    exit
}

# System temp dir
$TEMP_DIR = [System.IO.Path]::GetTempPath()
$outputFile = "$TEMP_DIR/pulsar-master-classpath.txt"

Set-Location "$APP_HOME/pulsar-app/pulsar-master"

& $MVN dependency:build-classpath -D"mdep.outputFile=$outputFile"

$MODULE_CLASSPATH = Get-Content -Path $outputFile -Raw
$CLASSPATH = "${env:CLASSPATH};${MODULE_CLASSPATH}"
$CLASSPATH = "${CLASSPATH};${APP_HOME}\$MODULE\target\$JAR"

# echo $CLASSPATH

Set-Location $APP_HOME

$env:CLASSPATH = $CLASSPATH
& $JAVA $CLASS $args
