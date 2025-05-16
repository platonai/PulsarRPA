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

> ⚠️ **Note**: If you're not running a full Spring Boot application (e.g., CLI tool, unit test, native launch), 
> you can place the properties file under `.pulsar/config/conf-enabled` to ensure it's picked up manually.

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

### 🧩 Supported Environment Variables

| Variable            | Description                                       |
| ------------------- | ------------------------------------------------- |
| `OPENAI_API_KEY`    | Your API key                                      |
| `OPENAI_MODEL_NAME` | Model name (e.g., `gpt-4o`, `doubao-1.5-pro-32k`) |
| `OPENAI_BASE_URL`   | API base URL (optional for default OpenAI)        |

### Example: Doubao

```bash
java -DOPENAI_API_KEY="9cc8e998-4655-4e90-a54c1-66659a524a97" \
     -DOPENAI_MODEL_NAME="doubao-1.5-pro-32k-250115" \
     -DOPENAI_BASE_URL="https://ark.cn-beijing.volces.com/api/v3" \
     -jar PulsarRPA.jar
```

Corresponding curl example:

```bash
curl https://ark.cn-beijing.volces.com/api/v3 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 9cc8e998-4655-4e90-a54c1-66659a524a97" \
  -d '{
    "model": "doubao-1.5-pro-256k-250115",
    "messages": [
      {"role": "system", "content": "你是人工智能助手。"},
      {"role": "user", "content": "常见的十字花科植物有哪些？"}
    ]
  }'
```
