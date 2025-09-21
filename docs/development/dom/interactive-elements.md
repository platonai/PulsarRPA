# 交互元素 (InteractiveElement) 规范

> 版本: v1 (可向后兼容扩展)  
> 目的: 为页面自动化 / Text-To-Action / 智能指令解析提供统一、稳定、语义充分的可交互元素抽取数据结构。

---
## 目录
1. 核心数据结构概览  
2. 字段语义与用途  
3. JSON 示例  
4. 生成与解析流程  
5. 在 LLM / 智能指令工作流中的角色  
6. 扩展字段与演进建议  
7. 稳定性与鲁棒性注意点  
8. 示例：从用户指令到操作  
9. 与执行引擎接口关系  
10. 常见改进点  
11. 最小复用片段  
12. 测试与质量建议  
13. 字段新增模板  
14. Prompt 集成建议  
15. 总结  

---
## 1. 核心数据结构概览
```kotlin
data class InteractiveElement(
    val id: String,
    val tagName: String,
    val selector: String,
    val text: String,
    val type: String?,
    val href: String?,
    val className: String?,
    val placeholder: String?,
    val value: String?,
    val isVisible: Boolean,
    val bounds: ElementBounds
) {
    val description: String
        get() = buildString {
            append("[")
            append(tagName)
            if (!type.isNullOrBlank()) append(" type='" + type + "'")
            append("] ")
            if (text.isNotBlank()) append("'${text.take(60)}' ")
            else if (!placeholder.isNullOrBlank()) append("ph='${placeholder.take(40)}' ")
            if (!value.isNullOrBlank()) append("val='${value.take(40)}' ")
            append("selector='${selector}'")
        }
}

data class ElementBounds(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)
```
说明：
- 仅包含核心扁平字段，利于序列化、压缩与向后兼容。
- `description` 为派生字段：面向 LLM 的紧凑语义串。

---
## 2. 字段语义与用途
| 字段 | 类型 | 必填 | 说明 | 典型用途 |
|------|------|------|------|----------|
| id | String | 否 | DOM `id`，可能为空且不保证唯一 | 构造稳定位策略 | 
| tagName | String | 是 | 标签名（小写） | 动作类型初筛 (input/button/a) |
| selector | String | 是 | 主定位 CSS Selector（需相对稳定） | 执行 click/fill 等 |
| text | String | 否 | 元素可见文本（≤100 截断） | 语义匹配 / 排序 |
| type | String? | 否 | input/button 的 `type` | 推断交互方式 |
| href | String? | 否 | a 标签链接 | 判断跳转 / 外链 |
| className | String? | 否 | 原始 class 串 | 模式匹配 / 降级定位 |
| placeholder | String? | 否 | 占位符 | 补足语义空白输入框 |
| value | String? | 否 | 当前值 | 避免重复填充 / 状态感知 |
| isVisible | Boolean | 是 | 当前视口可见 & 未隐藏 | 过滤不可交互元素 |
| bounds | ElementBounds | 是 | 几何信息 | 视区优先 / 距离策略 |
| description | String(派生) | - | 摘要串 | LLM 上下文投喂 |

特殊说明：
- 文本截断建议：`text.take(100)`，placeholder/value 可更短以控 token。
- bounds 用于排序时可计算：上方优先、靠近中心、面积权重等。

---
## 3. JSON 示例
```json
[
  {
    "id": "search-input",
    "tagName": "input",
    "selector": "#search-input",
    "text": "",
    "type": "search",
    "href": null,
    "className": "search-box",
    "placeholder": "Search...",
    "value": "",
    "isVisible": true,
    "bounds": { "x": 50, "y": 100, "width": 300, "height": 40 }
  },
  {
    "id": "submit-btn",
    "tagName": "button",
    "selector": "#submit-btn",
    "text": "Search",
    "type": "submit",
    "href": null,
    "className": "btn btn-primary",
    "placeholder": null,
    "value": null,
    "isVisible": true,
    "bounds": { "x": 360, "y": 100, "width": 80, "height": 40 }
  }
]
```

---
## 4. 生成与解析流程
1. 页面注入抽取脚本 (JS) 收集候选元素 → 返回结构化对象/数组。  
2. 宿主（Kotlin/Java）执行 `driver.evaluate(script)` 获取结果。  
3. 解析策略：
   - 若为 `List`：逐项 map → InteractiveElement。  
   - 若为 `Map`：尝试从 `elements` 字段获取数组。  
   - 若为 `String`：尝试 JSON parse。  
4. 容错：缺失字段用默认值（`""/null/0/false`），不抛异常。  
5. 过滤：`selector` 为空或 `tagName` 空 → 丢弃。  
6. 生成 `description` 进入后续流水。  

---
## 5. 在 LLM / 智能指令工作流中的角色
- 抽象层：统一多页面、多框架（iframe）元素的最小可交互描述。  
- Token 控制：仅传前 N（如 10~20）个候选，加启发式排序。  
- 语义桥梁：`text | placeholder | type` 与用户自然语言指令映射。  
- 降级策略：本地启发式先匹配 → LLM 复核/确认。  

典型流程：用户指令 → 解析关键词 → 初筛候选 → 评分排序 →(可选) LLM 选择 → 生成 driver 操作。  

---
## 6. 扩展字段与演进建议
| 字段 | 价值 | 说明 |
|------|------|------|
| ariaLabel | 语义补强 | 无障碍属性支撑无文本按钮 |
| role | 语义分类 | 与可访问性/组件类型匹配 |
| dataAttributes(Map) | 域特征 | data-* 聚合, 降级定位 |
| tabIndex | 可聚焦顺序 | 推断可交互性 |
| enabled/disabled | 状态 | 避免无效操作 |
| zIndex | 遮挡判断 | 叠层策略 |
| computedStyles | 诊断/排查 | display/visibility/opacity |
| framePath | 跨 iframe | 定位上下文链 |
| orderScore | 预计算排序 | 复用计算结果 |
| schemaVersion | 向后兼容 | 解析分支控制 |

扩展方式：  
1. JS 脚本加 key；2. data class 加可空字段；3. 解析统一 `as?`；4. description 选择性拼接。  

---
## 7. 稳定性与鲁棒性注意点
- Selector 生成策略优先级：`id > 稳定 class (语义) > 属性筛选 > 结构层级 > nth-child`。  
- 避免使用动态哈希类名（如含随机串）。  
- DOM 变动：执行前 `driver.exists(selector)` 校验，不存在则触发重抽取。  
- 文本截断：保留前缀 + 可选 hash 保障二次确认 (未来可加)。  
- 多语言：关键字匹配加入中英文（搜索/search, 提交/submit 等）。  

---
## 8. 示例：从用户指令到操作
指令："在搜索框输入 智能手机 然后点击搜索"  
流程：
1. 候选：`input#search-input`，`button#submit-btn`。  
2. 匹配：关键词(输入→input; 搜索→button[text~Search])。  
3. 操作：
```kotlin
driver.fill("#search-input", "智能手机")
driver.click("#submit-btn")
```

---
## 9. 与执行引擎接口关系
- ActionDescription：封装 LLM 生成/本地生成的一组 functionCalls。  
- InstructionResult：记录执行结果（成功/异常），便于反馈与重试。  
- selectedElement：可携带本地启发式最佳候选，减少 LLM 回合。  

---
## 10. 常见改进点
1. 使用正式 JSON 序列化 (kotlinx.serialization / Jackson)。  
2. 引入 embedding 相似度对文本/placeholder 排序。  
3. 加入视区遮挡检测 (交叉面积 < 阈值 → isVisible=false)。  
4. 评分函数：`score(e, cmd)` 综合（文本匹配 + 语义 + 位置 + 可见）。  
5. 元素缓存：页面结构 hash 命中 → 复用上次抽取。  

---
## 11. 最小复用片段
```kotlin
val elements = extractInteractiveElements(driver)
val best = selectBestMatchingElement(userCommand, elements)
if (best != null) {
    println("Chosen: ${best.selector} -> ${best.description}")
}
```

---
## 12. 测试与质量建议
用例建议：
- 空结果输入 → 返回空列表。  
- 缺失 bounds → 默认 0 值且不崩溃。  
- 仅字符串 JSON → 正确解析。  
- 多语言匹配："搜索" 应匹配 text=Search。  
- 不可见元素过滤：`isVisible=false` 不出现在前 N。  
- 重抽取：selector 失效自动重试。  

---
## 13. 字段新增模板
Kotlin：
```kotlin
val ariaLabel: String? = null
```
JS：
```js
{
  ariaLabel: el.getAttribute('aria-label') || null,
  // ... 其它字段
}
```
解析：
```kotlin
ariaLabel = map["ariaLabel"] as? String
```
描述拼接（可选）：
```kotlin
if (!ariaLabel.isNullOrBlank()) append("aria='${ariaLabel.take(30)}' ")
```

---
## 14. Prompt 集成建议
- 仅列出前 N（排序后）元素：`index. description`。  
- 强调：输出必须返回一个合法 selector。  
- 给予上下文：当前页面任务/用户指令/历史操作（避免循环）。  
- 控制长度：description 内避免长 value、placeholder。  
- 可加系统提示：若匹配度低提示需要重试或请求更多元素。  

---
## 15. 总结
InteractiveElement 作为“真实页面 → 智能策略/LLM → 执行动作”之间的中间语义层：
- 简洁：核心字段最小化；
- 语义充分：text/placeholder/type/visibility/bounds；
- 易扩展：全部新增字段可空；
- 面向智能：内置 description 以减少 Prompt 拼接成本；
- 易可靠：容错解析 + 重抽取策略可保证执行稳定性。  

建议后续演进：加入版本号、embedding 预处理、可视遮挡检测与评分缓存。  

---
附录：未来可能加入的 schemaVersion = 2 的新增候选字段（仅规划，不立即实现）：
- `importanceScore` (Double)：预估操作重要性；
- `interactionHistory` (Int)：当前会话已操作次数；
- `lastObserved` (Long Epoch ms)：上次抽取时间；
- `stabilityRank` (Int)： selector 稳定性打分。  

> 本文档可与 Dokka/Javadoc 生成的 API 文档协同，保持“概念层规范 + API 参考”双轨输出。

