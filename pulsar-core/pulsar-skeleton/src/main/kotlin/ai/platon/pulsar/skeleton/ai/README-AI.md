# 🚦 Coder Guideline

## 🎯 Purpose

Design an AI agent for **browser automation**.
Its main job is to **translate user’s natural language command into a callable API** inside **`MiniWebDriver`**.

---

## 📌 Main Responsibilities

1. **Action Translation**

    * Implement logic in **`ai.platon.pulsar.skeleton.ai.tta.TextToAction#generateWebDriverAction`**
    * Generate EXACT ONE WebDriver action with interactive elements.
    * Convert user’s plain text commands (e.g., *"scroll to the middle"*) into concrete **MiniWebDriver APIs**.

2. **Element Selection**

    * Always ask AI to **return the best matching interactive element** for the required action.
    * Example commands:

        * *"type `best AI toy` in the search box"*
        * *"scroll to the middle"*
        * *"click next button"*
        * *"go back"*
        * *"search `best AI toy`"*

3. **Prompt Construction**

    * Tool use style prompt and response
    * Provide **interactive element lists only** (not the entire page DOM).

4. **Interactive Element Handling**

    * The **element list** is computed by running JavaScript on the **active DOM**.
    * Ensure element references are **stable** against DOM mutations.

5. **Fallback Rule**

    * If **no suitable interactive element** exists for the requested action:
      → Generate an **empty suspend function** instead of throwing errors.

---

## ✅ Key Design Goals

* Minimal, accurate **action-to-API mapping**
* Reliable **element reference** under DOM changes
* Efficient prompts leveraging only **element list** (avoid full DOM dumps)

## Implementation Notes

* Only use MiniWebDriver as a contract, when you code, use WebDriver as the interface
* Reference `ai.platon.pulsar.skeleton.ai.tta.InteractiveElementExtractionTests` to lean how to create tests
