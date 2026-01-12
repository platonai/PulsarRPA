# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Import common utility script
. $AppHome\bin\common\Util.ps1

Fix-Encoding-UTF8

# 运行
./mvnw.cmd `
  -D"file.encoding=UTF-8" `
  -D"exec.mainClass=ai.platon.pulsar.examples.agent.Browser4AgentKt" `
  -pl examples/browser4-examples `
  exec:java
