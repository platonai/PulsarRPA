为了实现每分钟检查 Git 仓库是否有更新，并在有更新时运行 [build.sh](file://D:\workspace\PulsarRPA\PulsarRPA-3.0.x\bin\build.sh) 脚本，你可以使用以下 PowerShell 脚本。

---

### ✅ 实现思路

1. **每分钟执行一次 Git Pull**
2. **检查是否有更新（通过比较 HEAD 的哈希值）**
3. **如果有更新，运行 [build.sh](file://D:\workspace\PulsarRPA\PulsarRPA-3.0.x\bin\build.sh)**

---

### 📜 PowerShell 脚本

```powershell
#!/usr/bin/env pwsh

# Configuration
$repoPath = "D:\workspace\PulsarRPA\PulsarRPA-3.0.x"  # 你的 Git 仓库路径
$buildScript = "build.sh"                             # 你的构建脚本
$intervalSeconds = 60                                 # 检查间隔（秒）

# Enter the repository directory
Set-Location -Path $repoPath

# Function to get the current HEAD hash
function Get-HeadHash {
    return (git rev-parse HEAD)
}

# Main loop
$lastHash = Get-HeadHash
while ($true) {
    Write-Output "[INFO] Checking for updates at $(Get-Date)"
    
    # Perform git pull
    git pull
    
    # Get the new HEAD hash
    $newHash = Get-HeadHash
    
    # Compare hashes
    if ($newHash -ne $lastHash) {
        Write-Output "[INFO] New updates detected (Old: $lastHash, New: $newHash)"
        
        # Run build script if updates are detected
        if (Test-Path $buildScript) {
            Write-Output "[INFO] Running $buildScript..."
            & ".\$buildScript"
        } else {
            Write-Output "[ERROR] $buildScript not found in $repoPath"
        }
        
        # Update the last hash
        $lastHash = $newHash
    } else {
        Write-Output "[INFO] No updates detected."
    }
    
    # Wait for the next check
    Start-Sleep -Seconds $intervalSeconds
}
```


---

### 🔍 脚本说明

| 部分 | 功能 |
|------|------|
| `$repoPath` | 你的 Git 仓库路径 |
| `$buildScript` | 你的构建脚本（如 `build.sh`） |
| `$intervalSeconds` | 检查间隔时间（秒） |
| `Get-HeadHash` | 获取当前 Git 仓库的 HEAD 哈希值 |
| `git pull` | 拉取最新代码 |
| `Test-Path $buildScript` | 检查 `build.sh` 是否存在 |
| `& ".\$buildScript"` | 运行 `build.sh` |
| `Start-Sleep` | 等待指定时间后继续循环 |

---

### ✅ 如何使用

1. **保存脚本**：将上述脚本保存为 `git-pull-and-build.ps1`。
2. **修改配置**：
    - 将 `$repoPath` 设置为你的 Git 仓库路径。
    - 将 `$buildScript` 设置为你的构建脚本（如 [build.sh](file://D:\workspace\PulsarRPA\PulsarRPA-3.0.x\bin\build.sh)）。
3. **运行脚本**：
   ```powershell
   pwsh .\git-pull-and-build.ps1
   ```


---

### ✅ 示例输出

```
[INFO] Checking for updates at 04/05/2025 12:00:00
[INFO] No updates detected.
[INFO] Checking for updates at 04/05/2025 12:01:00
[INFO] New updates detected (Old: abcdefg, New: 1234567)
[INFO] Running build.sh...
[INFO] Build completed successfully.
[INFO] Checking for updates at 04/05/2025 12:02:00
[INFO] No updates detected.
```


---

### ✅ 注意事项

1. **确保 Git 已安装**：脚本依赖 Git 命令行工具。
2. **确保 [build.sh](file://D:\workspace\PulsarRPA\PulsarRPA-3.0.x\bin\build.sh) 可执行**：如果 [build.sh](file://D:\workspace\PulsarRPA\PulsarRPA-3.0.x\bin\build.sh) 是 Bash 脚本，确保它已赋予执行权限：
   ```bash
   chmod +x build.sh
   ```

3. **后台运行**：如果你希望脚本在后台运行，可以使用 `Start-Process`：
   ```powershell
   Start-Process pwsh -ArgumentList ".\git-pull-and-build.ps1" -NoNewWindow
   ```


---

### ✅ 增强功能（可选）

- **日志记录**：将输出重定向到日志文件：
  ```powershell
  Start-Process pwsh -ArgumentList ".\git-pull-and-build.ps1" -NoNewWindow -RedirectStandardOutput "log.txt"
  ```

- **错误处理**：捕获并处理 `git pull` 或 [build.sh](file://D:\workspace\PulsarRPA\PulsarRPA-3.0.x\bin\build.sh) 的错误。
- **通知**：在有更新时发送通知（如邮件或桌面通知）。

---

如果你需要进一步定制或遇到问题，请告诉我！
