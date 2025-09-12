#!/bin/bash
VERSION="3.0.8"
NEXT_VERSION="3.1.0"

test_cases=(
  "Download from http://example.com/v3.1.0/Browser4.jar"
  "Get it at https://releases.example.com/v3.1.0/installer.exe"
  "Download http://cdn.example.com/v3.1.0/app.zip or https://mirror.site.com/v3.1.0/app.zip"
  "Available at https://downloads.github.com/releases/v3.1.0/binary.tar.gz"
  "Old version at http://example.com/v2.5.0/legacy.jar"
  "Local path: /downloads/v3.1.0/file.txt"
  "Server: http://localhost:8080/v3.1.0/service.war"
  "Version 3.1.0 is available at https://repo.maven.org/maven2/com/example/v3.1.0/artifact.jar"
)

for i in "${!test_cases[@]}"; do
  echo "Test $((i+1)): ${test_cases[i]}"
  echo "Result:  $(sed "s|http\?://[^/]*/v$NEXT_VERSION/|/v$VERSION/|g" <<< "${test_cases[i]}")"
  echo "---"
done