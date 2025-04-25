#!/bin/bash

set -e
set -o pipefail

# ========== CONFIG ==========
IMAGE_NAME="galaxyeye88/pulsar-rpa"  # 👉 修改为你的镜像名
ROOT_DIR=""

# ========== STEP 1: 查找项目根目录 ==========
echo "🔍 Searching for project root..."
ROOT_DIR=$(pwd)
while [ ! -f "$ROOT_DIR/VERSION" ]; do
  ROOT_DIR=$(dirname "$ROOT_DIR")
  if [ "$ROOT_DIR" == "/" ]; then
    echo "❌ VERSION file not found. Please ensure you're inside a project with a VERSION file."
    exit 1
  fi
done
echo "📁 Project root found: $ROOT_DIR"

# ========== STEP 2: 读取版本号 ==========
VERSION=$(cat "$ROOT_DIR/VERSION" | tr -d ' \n')
if [ -z "$VERSION" ]; then
  echo "❌ VERSION file is empty."
  exit 1
fi
# 去掉-SNAPSHOT后缀
VERSION=${VERSION%-SNAPSHOT}
echo "🏷️ Version: $VERSION"

# ========== STEP 3: 构建镜像 ==========
cd "$ROOT_DIR"

DOCKERFILE_NAME="$ROOT_DIR/docker/dev/Dockerfile"

# If pulsar-app/pulsar-master/target/PulsarRPA.jar is not found, build it first
if [ ! -f "$ROOT_DIR/pulsar-app/pulsar-master/target/PulsarRPA.jar" ]; then
  echo "❌ PulsarRPA.jar not found. Please build it first."
  exit 1
fi

echo "🐳 Building Docker image: $IMAGE_NAME:$VERSION ..."
docker build -f $DOCKERFILE_NAME -t $IMAGE_NAME:$VERSION .

echo "🏷️ Tagging as latest..."
docker tag $IMAGE_NAME:$VERSION $IMAGE_NAME:latest

# ========== STEP 4: 推送 ==========

# Ask the user if they want to push the image
read -p "Do you want to push the image to Docker Hub? (y/n) " answer
if [ "$answer" != "${answer#[Yy]}" ] ;then
  echo "Pushing..."
else
  echo "Skipping..."
  exit 0
fi

echo "🚀 Pushing images..."
docker push $IMAGE_NAME:$VERSION
docker push $IMAGE_NAME:latest

# ========== DONE ==========
echo "✅ Docker images pushed successfully:"
echo "   - $IMAGE_NAME:$VERSION"
echo "   - $IMAGE_NAME:latest"