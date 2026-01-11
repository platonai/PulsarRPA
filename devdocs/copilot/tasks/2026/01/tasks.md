# Coding Tasks

## 创建 AI 友好的系统使用文档

任务要求：

创建 AI 友好的系统使用文档，使得 AI 阅读文档后，能够理解系统的功能和使用方法，生成高质量，强约束的自然语言指令。
该指令通过 commands/plain API 提交给系统执行，确保系统能够准确理解并执行指令。

主要入口：

ai.platon.pulsar.rest.api.controller.CommandController

实现功能：

1. 提交开放任务，由 Agent 自主执行，入口为 ai.platon.pulsar.rest.api.service.CommandService.executeAgentCommand
2. 当任务可转换为 CommandRequest 格式时，约束到 ai.platon.pulsar.rest.api.service.CommandService.executeCommand

关键数据结构：

ai.platon.pulsar.rest.api.entities.CommandRequest
ai.platon.pulsar.rest.api.entities.CommandStatus
ai.platon.pulsar.rest.api.entities.CommandResult

参考文档：

load-options-guide.md
load-options-guide-zh.md
load-options-quick-ref.md
page-event-handlers.md
pulsar-settings-guide.md
pulsar-settings-quick-ref.md
