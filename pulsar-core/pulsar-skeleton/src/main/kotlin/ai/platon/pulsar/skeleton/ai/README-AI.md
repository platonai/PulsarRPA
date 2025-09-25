# 🚦 Coder Guideline For WebDriverAgent

你的任务：
- 优化本文档
- 根据本文档实现代码

[WebDriverAgent.kt](WebDriverAgent.kt) 是一个“多轮计划执行器”：
它让通用模型基于截图观察与历史动作来规划下一步（act/extract/goto 等），每步只做一个原子动作，
直到判断目标完成。

关键点：
- 首条系统消息要求“拆解为原子动作，一步一步来”
- 每轮将“上一轮动作摘要 + 当前截图”作为 `user` 消息输入模型
- 模型输出结构化 JSON 决定下一步
- 执行动作后继续下一轮，终止条件可由 `method=close` 或 `taskComplete=true` 等判断
- 循环结束后调用 `operatorSummarySchema` 要求模型对原始目标产出总结


Prompt 摘要：
- `buildOperatorSystemPrompt(goal)`（system）：
    - 你是通用代理，需要基于步骤完成用户目标
    - 重要指南：
        1) 将复杂动作拆成原子步骤
        2) act 一次仅做一个动作（单击一次、输入一次、选择一次）
        3) 不要在一步中合并多个动作
        4) 多个动作用多步表达
- 运行时 `user` 消息：
    - “此前动作摘要” 文本
    - 当前页面的截图（Anthropic 用 base64 image 块；OpenAI 用 `image_url` data URI）
