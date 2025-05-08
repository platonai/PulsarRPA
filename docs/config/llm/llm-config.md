# 🤖 Additional LLM Configuration Methods

## ⚙️ Method 1: XML Configuration

To configure an LLM using XML, simply copy one of the provided configuration files into your PulsarRPA config directory.

### 📋 Example Commands

**Linux/macOS:**

```shell
cp docs/config/llm/template/pulsar-volcengine-deepseek.xml ~/.pulsar/config/conf-enabled
```

> 💡 **Note:** Don't forget to update the XML file with your own API keys and parameters!

---

## 🔌 Method 2: OpenAI-Compatible LLM Configuration

Use any OpenAI-compatible API provider (e.g., DeepSeek, Moonshot, etc.) by setting environment variables at runtime.

### 📥 Step 1: Download the Latest JAR

```shell
# Linux/macOS and Windows (if curl is available)
curl -L -o PulsarRPA.jar https://github.com/platonai/PulsarRPA/releases/download/v3.0.4/PulsarRPA.jar
```

### ▶️ Step 2: Run with Custom LLM Settings

```bash
java -DOPENAI_API_KEY=${OPENAI_API_KEY} \
     -DOPENAI_MODEL_NAME=${OPENAI_MODEL_NAME} \
     -DOPENAI_BASE_URL=${OPENAI_BASE_URL} \
     -jar PulsarRPA.jar
```

### 🧩 Supported Environment Variables:

- `OPENAI_API_KEY` – Your API key.
- `OPENAI_MODEL_NAME` – The model name (e.g., `gpt-4o`).
- `OPENAI_BASE_URL` – Base URL for the API endpoint (optional if using default OpenAI).
