# 🚦 WebDriverAgent Developer Guide

## 📋 Prerequisites

Before starting development, ensure you understand:

1. **Root Directory** `README-AI.md` - Global development guidelines and project structure
2. **Project Architecture** - Multi-module Maven project with Kotlin as primary language
3. **Testing Guidelines** - Comprehensive testing strategy with unit, integration, and E2E tests
   - Unless explicitly required, web page access during testing must be directed to the Mock Server
   - The relevant web page resources are located in the directory `pulsar-tests-common/src/main/resources/static/generated/tta`

## 🎯 Overview

[WebDriverAgent.kt](WebDriverAgent.kt) is an **enterprise-grade multi-round planning executor** that enables AI models to perform
web automation through screenshot observation and historical action analysis. It plans and executes atomic
actions step-by-step until the target is achieved.

### Key Architecture Principles

- **Atomic Actions**: Each step performs exactly one atomic action (single click, single input, single selection)
- **Multi-round Planning**: AI model plans next action based on screenshot + action history
- **Structured Output**: Model returns JSON-formatted function calls
- **Termination Control**: Loop ends via `taskComplete=true`
- **Result Summarization**: Final summary generated using `operatorSummarySchema`
- **Error Resilience**: Graceful handling of failures with fallback strategies
- **Safety First**: URL validation and secure execution environment

## 🧪 Testing Strategy

### Integration Tests
- **Real browser automation** with Spring context

### Test Coverage Areas
1. **Action Execution Pipeline** - All tool calls (navigation, interactions, scrolling, screenshots)
2. **Error Handling** - Graceful degradation and recovery
3. **Termination Logic** - Task completion detection and cleanup
4. **Security Validation** - URL safety and input sanitization
5. **Performance Boundaries** - Step limits and timeout handling
