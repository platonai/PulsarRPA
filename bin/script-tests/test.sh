#!/bin/bash

JAR_NAME=${{ env.PRODUCTION_JAR_NAME }}

echo "=== Building PulsarRPA JAR ==="
./mvnw package -rf :"$JAR_NAME" -pl scent-app/scent-"$JAR_NAME" \
  -DskipTests=true -Dmaven.javadoc.skip=true

JAR_PATH=${{ env.PRODUCTION_JAR_PATH }}
if [ ! -f "$JAR_PATH" ]; then
  echo "❌ $JAR_NAME.jar not found at $JAR_PATH"
  echo "All created jars: "
  find . -name "$JAR_NAME.jar" -print
  exit 1
fi
echo "✅ $JAR_NAME.jar built successfully"
