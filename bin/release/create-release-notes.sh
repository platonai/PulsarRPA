#!/bin/bash

# 🔍 Find the first parent directory containing the VERSION file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ ! -f "$APP_HOME/VERSION" && "$APP_HOME" != "/" ]]; do
  APP_HOME=$(dirname "$APP_HOME")
done
[[ -f "$APP_HOME/VERSION" ]] && cd "$APP_HOME" || exit

VERSION=$(head -n 1 "$APP_HOME/VERSION" | tr -d '\r\n' | tr -d '[:space:]')
UBERJAR_FILE="PulsarRPA.jar"
UBERJAR_PATH=$(find pulsar-app/pulsar-browser4 -name $UBERJAR_FILE | head -n 1)
JAVA_VERSION="17+"
OUTPUT_FILE="release_notes.md"
REPO_URL=$(git config --get remote.origin.url | sed 's/\.git$//' | sed 's|git@github.com:|https://github.com/|')
REPO_NAME=$(echo "$REPO_URL" | sed 's|.*/||')
REPO_OWNER=$(echo "$REPO_URL" | sed -E 's|.*/([^/]+)/[^/]+$|\1|')
ACTOR=$(git config user.name)

echo "📝 Generating release notes..."

# Get previous tag for changelog
PREV_TAG=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "")

# Get JAR size
JAR_SIZE=$(stat -c%s "$UBERJAR_PATH" 2>/dev/null || echo 0)
JAR_SIZE_HUMAN=$(numfmt --to=iec --suffix=B $JAR_SIZE)

# Get commit count and contributors
if [ -n "$PREV_TAG" ]; then
  COMMIT_COUNT=$(git rev-list --count "$PREV_TAG"..HEAD 2>/dev/null || echo "0")
  CHANGELOG=$(git log --pretty=format:"- %s ([%h](${REPO_URL}/commit/%H))" "$PREV_TAG"..HEAD)
  CHANGELOG_LINK="[${PREV_TAG}...v${VERSION}](${REPO_URL}/compare/${PREV_TAG}...v${VERSION})"
else
  COMMIT_COUNT=$(git rev-list --count HEAD~10..HEAD 2>/dev/null || echo "0")
  CHANGELOG=$(git log --pretty=format:"- %s ([%h](${REPO_URL}/commit/%H))" HEAD~10..HEAD)
  CHANGELOG_LINK="Initial release"
fi

CONTRIBUTOR_COUNT=$(git shortlog -sn ${PREV_TAG:+$PREV_TAG..}HEAD | wc -l)
BUILD_DATE=$(date -u +'%Y-%m-%d %H:%M:%S UTC')

# Create release notes
cat > $OUTPUT_FILE << EOF
# 🚀 PulsarRPA v${VERSION} Release Notes

**Release Date:** $BUILD_DATE
**Java Version:** $JAVA_VERSION
**Built by:** @$ACTOR

---

## 📦 Release Highlights

### 📊 Release Statistics
| Metric | Value |
|--------|-------|
| 📁 **JAR Size** | $JAR_SIZE_HUMAN |
| 📝 **Commits** | $COMMIT_COUNT |
| 👥 **Contributors** | $CONTRIBUTOR_COUNT |
| 🏗️ **Build Date** | $BUILD_DATE |

### 🔄 Changes Since Last Release
$CHANGELOG

---

## 🚀 Quick Start

### 📥 Download & Run

#### 🧩 Download

\`\`\`bash
curl -L -o ${UBERJAR_FILE} ${REPO_URL}/releases/download/v${VERSION}/${UBERJAR_FILE}
\`\`\`

#### 🚀 Run

\`\`\`shell
echo \$DEEPSEEK_API_KEY # make sure LLM api key is set. VOLCENGINE_API_KEY/OPENAI_API_KEY also supported.
java -D"DEEPSEEK_API_KEY=\${DEEPSEEK_API_KEY}" -jar PulsarRPA.jar
\`\`\`

> 🔍 **Tip:** Make sure \`DEEPSEEK_API_KEY\` or other LLM API key is set in your environment, or AI features will not be available.

> 🔍 **Tip:** On Windows, both \`\$DEEPSEEK_API_KEY\` and \`\$env:DEEPSEEK_API_KEY\` works, but they are different variables.

### 🐳 Docker

\`\`\`shell
# make sure LLM api key is set. VOLCENGINE_API_KEY/OPENAI_API_KEY also supported.
echo \$DEEPSEEK_API_KEY
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=\${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa:${VERSION}
\`\`\`

GitHub Container Registry:
\`\`\`shell
docker pull ghcr.io/${REPO_OWNER}/pulsar-rpa:${VERSION}
\`\`\`
---

## 📄 Full Changelog
$CHANGELOG_LINK

---

*Built with ❤️ by the PulsarRPA team*
EOF

echo "✅ Release notes generated: $OUTPUT_FILE"