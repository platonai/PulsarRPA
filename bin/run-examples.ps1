# 强制 PowerShell 使用 UTF-8 输出
$OutputEncoding = [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding  = [System.Text.Encoding]::UTF8

# 强制 Maven 使用 UTF-8（尤其是 exec:java）
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

# 运行
./mvnw.cmd `
  -D"file.encoding=UTF-8" `
  -D"exec.mainClass=ai.platon.pulsar.examples.agent.Browser4AgentKt" `
  -pl pulsar-examples `
  exec:java
