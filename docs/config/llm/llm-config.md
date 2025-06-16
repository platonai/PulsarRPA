# 🤖 LLM Configuration

## ⚙️ Method 1: Use `application.properties` or `application-private.properties`

PulsarRPA supports Spring Boot–style property files. You can place your private config in:

* `application.properties`
* Or `application-private.properties` (recommended for sensitive data)

### ✅ Sample Configuration

#### 🔍 DeepSeek

```properties
deepseek.api.key=sk-your-deepseek-key
```

#### 📦 Doubao

```properties
volcengine.api.key=9cc8e998-4655-4g90-a54c-1234567890
volcengine.model.name=doubao-1.5-pro-32k-250115
volcengine.base.url=https://ark.cn-beijing.volces.com/api/v3
```

#### 🌐 OpenAI

```properties
openai.api.key=9cc8e998-4655-4g90-a54c-1234567890
openai.model.name=gpt-4o
openai.base.url=https://api.openai.com/v1
```

---

## 🔌 Method 2: Configure via Environment Variables

You can configure it at runtime using JVM system properties:

OpenAI-compatible API providers (e.g., DeepSeek, Moonshot, Doubao, etc.):
```bash
java -DOPENAI_API_KEY=${OPENAI_API_KEY} \
     -DOPENAI_MODEL_NAME=${OPENAI_MODEL_NAME} \
     -DOPENAI_BASE_URL=${OPENAI_BASE_URL} \
     -jar PulsarRPA.jar
```

<details>
<summary>💡 PowerShell Version</summary>

```powershell
java -DOPENAI_API_KEY=${OPENAI_API_KEY} `
     -DOPENAI_MODEL_NAME=${OPENAI_MODEL_NAME} `
     -DOPENAI_BASE_URL=${OPENAI_BASE_URL} `
     -jar PulsarRPA.jar
```

</details>

The table formatting in the environment variables section can be improved for better readability. Let me update it with better structure, removing the empty cells and organizing it by provider.

<!-- replace lines 60 to 72 -->
### 🧩 Supported Environment Variables

#### DeepSeek

| Variable              | Description                                               |
|-----------------------|-----------------------------------------------------------|
| `DEEPSEEK_API_KEY`    | DeepSeek API key                                          |
| `DEEPSEEK_MODEL_NAME` | DeepSeek model name (optional, default: `deepseek-chat`)  |

#### Doubao (VolcEngine)

| Variable                | Description                                                                               |
|------------------------|-------------------------------------------------------------------------------------------|
| `VOLCENGINE_API_KEY`   | Your VolcEngine API key                                                                    |
| `VOLCENGINE_MODEL_NAME`| VolcEngine model name (optional, default: `doubao-1.5-pro-32k-250115`)                    |
| `VOLCENGINE_BASE_URL`  | VolcEngine API base URL (optional, default: `https://ark.cn-beijing.volces.com/api/v3`)   |

#### OpenAI (and compatible providers)

| Variable            | Description                                                              |
|--------------------|--------------------------------------------------------------------------|
| `OPENAI_API_KEY`    | Your API key                                                            |
| `OPENAI_MODEL_NAME` | Model name (e.g., `gpt-4o`, `doubao-1.5-pro-32k`)                       |
| `OPENAI_BASE_URL`   | API base URL (optional for default OpenAI, any compatible URL allowed)  |


### Example: Doubao with OpenAI-compatible API

```bash
java -DOPENAI_API_KEY="9cc8e998-4655-4e90-a54c1-12345abcdefg" \
     -DOPENAI_MODEL_NAME="doubao-1.5-pro-32k-250115" \
     -DOPENAI_BASE_URL="https://ark.cn-beijing.volces.com/api/v3" \
     -jar PulsarRPA.jar
```

Corresponding curl example:

```bash
curl https://ark.cn-beijing.volces.com/api/v3 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 9cc8e998-4655-4e90-a54c1-12345abcdefg" \
  -d '{
    "model": "doubao-1.5-pro-256k-250115",
    "messages": [
      {"role": "system", "content": "你是人工智能助手。"},
      {"role": "user", "content": "常见的十字花科植物有哪些？"}
    ]
  }'
```
