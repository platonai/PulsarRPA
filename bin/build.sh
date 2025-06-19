#!/bin/bash

# Find the first parent directory that contains a VERSION file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
[[ -f "$APP_HOME/VERSION" ]] && cd "$APP_HOME" || exit

function printUsage {
  echo "Usage: build.sh [-clean|-test]"
  exit 1
}

# Maven command and options
MvnCmd="./mvnw"

# Initialize flags and additional arguments
PerformClean=false
SkipTests=true

MvnOptions=()
AdditionalMvnArgs=()

# Parse command-line arguments
for Arg in "$@"; do
  case $Arg in
    -clean)
      PerformClean=true
      ;;
    -t|-test)
      SkipTests=false
      ;;
    -h|-help|--help)
      printUsage
      ;;
    -*|--*)
      printUsage
      ;;
    *)
      AdditionalMvnArgs+=("$Arg")
      ;;
  esac
done

# Conditionally add Maven options based on flags
if $PerformClean; then
  MvnOptions+=("clean")
fi

if $SkipTests; then
  AdditionalMvnArgs+=("-DskipTests")
fi

# Function to execute Maven command in a given directory
function invokeMavenBuild {
  local Directory=$1
  shift
  local MvnOptions=("$@")

  pushd "$Directory" > /dev/null || return

  $MvnCmd "${MvnOptions[@]}"

  if [[ $? -ne 0 ]]; then
    echo "Warning: Maven command failed in $Directory"
  fi

  popd > /dev/null || return
}

# Execute Maven package in the application home directory
MvnOptions+=("install")
AdditionalMvnArgs+=("-Pall-modules")

MvnOptions+=("${AdditionalMvnArgs[@]}")
invokeMavenBuild "$AppHome" "${MvnOptions[@]}"