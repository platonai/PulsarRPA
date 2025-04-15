好的，Vincent！下面是一个 **一键同步 `main` 到 `release` 的 Bash 脚本**，功能包括：

✅ 备份当前 `main`  
✅ 强制重置 `main` 内容为 `release`  
✅ 强制推送到远程（同步 GitHub）  
✅ 全程打印状态日志

---

## 📜 脚本内容：`sync-main-to-release.sh`

```bash
#!/bin/bash

set -e  # 出错即停止
set -o pipefail

# === 配置分支名 ===
MAIN_BRANCH="main"
SOURCE_BRANCH="release"
BACKUP_BRANCH="backup-main-$(date +%Y%m%d-%H%M%S)"

echo "🚀 [1/5] Fetching latest branches..."
git fetch origin

echo "📦 [2/5] Creating backup branch: $BACKUP_BRANCH"
git checkout $MAIN_BRANCH
git branch "$BACKUP_BRANCH"

echo "🔄 [3/5] Resetting $MAIN_BRANCH to match $SOURCE_BRANCH"
git reset --hard "origin/$SOURCE_BRANCH"

echo "🚀 [4/5] Pushing $MAIN_BRANCH to origin (force)"
git push -f origin "$MAIN_BRANCH"

echo "✅ [5/5] Done! '$MAIN_BRANCH' is now synced to 'origin/$SOURCE_BRANCH'."
echo "📂 Backup branch created: $BACKUP_BRANCH (local only)"
```

---

## 📦 使用方式

1. 将脚本保存为 `sync-main-to-release.sh`
2. 给它执行权限：

```bash
chmod +x sync-main-to-release.sh
```

3. 运行它：

```bash
./sync-main-to-release.sh
```

---

## 🛡️ 脚本特点

| 功能              | 说明                                         |
|-------------------|----------------------------------------------|
| 自动备份           | 避免误操作导致 main 丢失                     |
| 强制同步           | 彻底保证 `main` 与 `release` 完全一致       |
| 可复用             | 可适配任意两个分支（改变量即可）            |

---

如果你希望这个脚本集成到 CI/CD，比如 GitHub Actions、GitLab CI，我也可以帮你定制一份流水线版本。

需要吗？🚀