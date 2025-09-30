# 🚦 WebDriverAgent Developer Guide

## 📋 Prerequisites

Before starting, read the following documents to understand the project structure:

1. **Root Directory** `README-AI.md` - Global development guidelines and project structure

## Core Tasks:

- Optimize this documentation
- Implement code according to this specification

## 🎯 Overview

[WebDriverAgent.kt](WebDriverAgent.kt) is a **multi-round planning executor** that enables AI models to perform 
web automation through screenshot observation and historical action analysis. It plans and executes atomic 
actions (act/extract/navigate) step-by-step until the target is achieved.

### Key Architecture Principles

- **Atomic Actions**: Each step performs exactly one atomic action (single click, single input, single selection)
- **Multi-round Planning**: AI model plans next action based on screenshot + action history
- **Structured Output**: Model returns JSON-formatted function calls
- **Termination Control**: Loop ends via `stop` function call or `taskComplete=true`
- **Result Summarization**: Final summary generated using `operatorSummarySchema`

### Core Workflow

1. **System Prompt** (`buildOperatorSystemPrompt(goal)`):
   - Establishes AI as general-purpose agent for step-by-step task completion
   - Enforces atomic action decomposition
   - Provides tool specification and user goal

2. **User Message** (per iteration):
   - Previous action summary (last 8 steps)
   - Current page screenshot (base64 encoded)
   - Target instruction and current URL

3. **AI Response Processing**:
   - Parse structured JSON with tool calls
   - Execute single atomic action via WebDriver
   - Update history and continue loop

4. **Termination & Summary**:
   - Loop ends on `taskComplete=true`, `method=close`, or `maxSteps` reached
   - Generate final summary of execution trajectory

## 🔧 Key Components

### TextToAction Integration

```kotlin
// Generate EXACT ONE step using AI
val action = tta.generateWebDriverAction(message, driver, screenshotB64)

// Execute the generated action
suspend fun act(action: ActionDescription): InstructionResult
```

### Action Execution Pipeline

1. **Screenshot Capture**: `safeScreenshot()` - Base64 encoded current page
2. **Message Construction**: Combine system prompt + user context + screenshot
3. **AI Action Generation**: Single atomic action via TextToAction
4. **Action Execution**: WebDriver command execution with error handling
5. **History Tracking**: Maintain execution trajectory for context

### Supported Tool Calls

- **Navigation**: `navigateTo(url)`, `waitForSelector(selector, timeout)`
- **Interactions**: `click(selector)`, `fill(selector, text)`, `press(selector, key)`
- **Form Controls**: `check(selector)`, `uncheck(selector)`
- **Scrolling**: `scrollDown(count)`, `scrollUp(count)`, `scrollToTop()`, `scrollToBottom()`
- **Screenshots**: `captureScreenshot()`, `captureScreenshot(selector)`
- **Timing**: `delay(millis)`

### Error Handling & Resilience

- **Graceful Degradation**: Continue execution on individual action failures
- **Screenshot Safety**: Handle screenshot capture failures without crashing
- **Tool Call Validation**: Skip invalid/unknown tool calls with warnings
- **Navigation Safety**: URL validation and navigation error handling

## 📊 Performance & Monitoring

- **Step Limit**: Configurable `maxSteps` (default: 100) prevents infinite loops
- **History Management**: Keep last 8 actions for context efficiency
- **Screenshot Persistence**: Optional step-by-step screenshot saving
- **Session Logging**: Complete execution transcript with timestamps

## 🔒 Security Considerations

- **Input Validation**: All user inputs sanitized before execution
- **URL Validation**: Navigation targets validated for safety
- **Resource Limits**: Configurable timeouts and step limits
- **Error Isolation**: Individual action failures don't crash entire session

## Human Reviewer Comments

### Nonsense tests

Nonsense tests in WebDriverAgentBasicTest, none of the tests make sense.

### Examples

[SessionInstructionsExample](/pulsar-examples/src/main/kotlin/ai/platon/pulsar/examples/agent/SessionInstructions.kt)


