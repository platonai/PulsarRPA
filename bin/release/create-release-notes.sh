#bin

# 🔍 Find the first parent directory containing the VERSION file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
[[ -f "$APP_HOME/VERSION" ]] && cd "$APP_HOME" || exit

VERSION=$(head -n 1 "$APP_HOME/VERSION" | tr -d '\r\n' | tr -d '[:space:]')
UBERJAR_FILE="PulsarRPA.jar"
UBERJAR_PATH=$(find pulsar-app/pulsar-master -name $UBERJAR_FILE | head -n 1)

echo "::group::📝 Generating Release Notes"

PREV_TAG=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "")

if [ -n "$PREV_TAG" ]; then
  echo "📋 Changes since $PREV_TAG:"
  CHANGELOG=$(git log --pretty=format:"- %s (%h)" "$PREV_TAG"..HEAD)
else
  echo "📋 Initial release changes:"
  CHANGELOG=$(git log --pretty=format:"- %s (%h)" HEAD~10..HEAD)
fi

JAR_SIZE=$(stat -c%s "$UBERJAR_PATH" 2>/dev/null || echo 0)

cat > release_notes.md << EOF
## Release \`$VERSION\`

### 📦 Artifacts
- **JAR File**: "$UBERJAR_FILE"
- **Size**: $(numfmt --to=iec --suffix=B "$JAR_SIZE")
- **Java Version**: 17+

### 🔄 Changes
$CHANGELOG

### 🚀 Usage
\`\`\`bash
# LLM features enabled
java -DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} -jar $UBERJAR_FILE
# No LLM features
java -jar $UBERJAR_FILE
\`\`\`

Built on $(date -u +'%Y-%m-%d %H:%M:%S UTC')
EOF

echo "Generated release notes:"
cat release_notes.md
echo "::endgroup::"
