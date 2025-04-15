好的，Vincent！以下是符合你要求的 `docker-release.sh` 脚本：

---

## ✅ 脚本功能汇总：

- 🔍 自动定位项目根目录（以存在 `VERSION` 文件为准）
- 🏷️ 从 `VERSION` 文件中读取版本号
- 🐳 使用 `Dockerfile-jar` 构建两个镜像：
    - `$IMAGE_NAME:$VERSION`
    - `$IMAGE_NAME:latest`
- 🚀 自动推送两个镜像到远程仓库（如 Docker Hub）

---

## 📜 `docker-release.sh`

```bash
#!/bin/bash

set -e
set -o pipefail

# ========== CONFIG ==========
IMAGE_NAME="galaxyeye88/pulsar-rpa"  # 👉 修改为你的镜像名
DOCKERFILE_NAME="Dockerfile-jar"
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
echo "🏷️ Version: $VERSION"

# ========== STEP 3: 构建镜像 ==========
cd "$ROOT_DIR"

echo "🐳 Building Docker image: $IMAGE_NAME:$VERSION ..."
docker build -f $DOCKERFILE_NAME -t $IMAGE_NAME:$VERSION .

echo "🏷️ Tagging as latest..."
docker tag $IMAGE_NAME:$VERSION $IMAGE_NAME:latest

# ========== STEP 4: 推送 ==========
echo "🚀 Pushing images..."
docker push $IMAGE_NAME:$VERSION
docker push $IMAGE_NAME:latest

# ========== DONE ==========
echo "✅ Docker images pushed successfully:"
echo "   - $IMAGE_NAME:$VERSION"
echo "   - $IMAGE_NAME:latest"
```

---

## 📦 使用方法：

1. 将此脚本保存为 `docker-release.sh`
2. 替换里面的 `IMAGE_NAME` 为你自己的镜像名，例如：
   ```bash
   IMAGE_NAME="vincentlab/ai-gateway"
   ```
3. 添加执行权限：
   ```bash
   chmod +x docker-release.sh
   ```
4. 运行：
   ```bash
   ./docker-release.sh
   ```

---

## 🧠 可选增强（告诉我即可加上）：

| 增强功能             | 说明                            |
|----------------------|---------------------------------|
| 自动构建 JAR         | 加上 `mvn clean package` 步骤   |
| CI/CD 集成           | 写成 GitHub Actions 或 GitLab   |
| 推送前登录 DockerHub | `docker login` 加入脚本         |
| 检查是否重复推送     | 用 `docker manifest` 查询       |

需要我帮你加上构建 jar 包逻辑或适配 CI/CD 吗？随时说 👌