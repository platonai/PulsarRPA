# 🤖 Additional LLM Configuration Methods

## 🔌 Method 1: OpenAI-Compatible LLM Configuration

Use any OpenAI-compatible API provider (e.g., DeepSeek, Moonshot, etc.) by setting environment variables at runtime.

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

### Examples:

#### **Doubao:**

Run PulsarRPA:

```bash
java -DOPENAI_API_KEY="9cc8e998-4655-4e90-a54c1-66659a524a97" \
     -DOPENAI_MODEL_NAME="doubao-1-5-pro-32k-250115" \
     -DOPENAI_BASE_URL="https://ark.cn-beijing.volces.com/api/v3" \
     -jar PulsarRPA.jar
```

The corresponding curl command:

```shell
curl https://ark.cn-beijing.volces.com/api/v3/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 9cc8e998-4655-4e90-a54c1-66659a524a97" \
  -d '{
    "model": "doubao-1-5-pro-256k-250115",
    "messages": [
      {"role": "system","content": "你是人工智能助手."},
      {"role": "user","content": "常见的十字花科植物有哪些？"}
    ]
  }'
```

## ⚙️ Method 2: XML Configuration

To configure an LLM using XML, simply copy one of the provided configuration files into your PulsarRPA config directory.

### 📋 Example Commands

**Linux/macOS:**

```shell
cp docs/config/llm/template/pulsar-volcengine-deepseek.xml ~/.pulsar/config/conf-enabled
```

> 💡 **Note:** Don't forget to update the XML file with your own API keys and parameters!

---
