$BIN = Split-Path -Parent $MyInvocation.MyCommand.Definition
$APP_HOME = (Resolve-Path "$BIN\..").Path

$CLEAN = $false
$SKIP_TEST = $true

$MVNW="$APP_HOME\mvnw"
$MVN_OPTS = @()

if ($CLEAN) {
  $MVN_OPTS += "clean"
}

if ($SKIP_TEST) {
  $MVN_OPTS += "-DskipTests=true"
}
$MVN_OPTS += "-Pall-modules"

Push-Location $APP_HOME -ErrorAction Stop
& $MVNW $MVN_OPTS
Pop-Location
