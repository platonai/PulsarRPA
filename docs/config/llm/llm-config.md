# 🤖 LLM Configuration

## 🔌 Method 1: Configure via Environment Variables

You can configure the API key at runtime using JVM system properties.

---

### 🌐 Built-in LLM Providers

* DeepSeek
* Doubao (VolcEngine)
* OpenAI (and compatible providers)

---

### 📦 Examples

#### 💻 Linux / macOS:

```bash
export DEEPSEEK_API_KEY="sk-your-deepseek-key"
java -DDEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} -jar PulsarRPA.jar
````

#### 🪟 Windows (PowerShell):

✅ **Option 1: Using Local (Script-Only) Variable**

```powershell
$DEEPSEEK_API_KEY = "sk-your-deepseek-key"
java -D"DEEPSEEK_API_KEY=$DEEPSEEK_API_KEY" -jar PulsarRPA.jar
```

⚠️ This sets a PowerShell *local variable*, which works **only inside PowerShell**, and is passed correctly because Java gets the interpolated string from PowerShell.

✅ **Option 2: Using Environment Variable (System-Wide)**

```powershell
$env:DEEPSEEK_API_KEY = "sk-your-deepseek-key"
java -D"DEEPSEEK_API_KEY=$env:DEEPSEEK_API_KEY" -jar PulsarRPA.jar
```

---

## ⚙️ Method 2: Use Spring Boot Configuration Files

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

## 🧩 Supported Configurations

PulsarRPA follows **Spring Boot configuration rules**, which means you can use configuration keys in two equivalent formats:

## 🔑 Key Format Equivalence

**Important**: The following formats are **identical** and interchangeable:

| Environment Variable Format | Properties File Format | Description |
|----------------------------|----------------------|-------------|
| `DEEPSEEK_API_KEY` | `deepseek.api.key` | DeepSeek API key |
| `VOLCENGINE_API_KEY` | `volcengine.api.key` | VolcEngine (Doubao) API key |
| `OPENAI_API_KEY` | `openai.api.key` | OpenAI API key |
| `BROWSER_CONTEXT_MODE` | `browser.context.mode` | Browser context mode |
| `BROWSER_MAX_OPEN_TABS` | `browser.max.active.tabs` | Max browser tabs |
| `PROXY_ROTATION_URL` | `proxy.rotation.url` | Proxy rotation endpoint |

> 💡 **Spring Boot Convention**: Environment variables use `UPPER_CASE_WITH_UNDERSCORES` while properties files use `lower.case.with.dots`. Both formats reference the same configuration value.

---

### DeepSeek

| Variable              | Description                                               |
|-----------------------|-----------------------------------------------------------|
| `DEEPSEEK_API_KEY`    | DeepSeek API key                                          |
| `DEEPSEEK_MODEL_NAME` | DeepSeek model name (optional, default: `deepseek-chat`)  |

### Doubao (VolcEngine)

| Variable                | Description                                                                               |
|------------------------|-------------------------------------------------------------------------------------------|
| `VOLCENGINE_API_KEY`   | Your VolcEngine API key                                                                    |
| `VOLCENGINE_MODEL_NAME`| VolcEngine model name (optional, default: `doubao-1.5-pro-32k-250115`)                    |
| `VOLCENGINE_BASE_URL`  | VolcEngine API base URL (optional, default: `https://ark.cn-beijing.volces.com/api/v3`)   |

### OpenAI (and compatible providers)

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
