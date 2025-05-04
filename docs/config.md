# 🛠️ PulsarRPA Configuration Guide

## 📋 Configuration Sources

PulsarRPA is a standard Spring Boot application and supports multiple configuration sources in order of precedence:

1. 🔧 **Environment Variables**
2. ⚙️ **JVM System Properties**
3. 📝 **Spring Boot `application.properties` or `application.yml`**

---

## 🔧 Configuration Methods

### 🌍 Environment Variables / JVM System Properties

You can set configuration values using OS environment variables or JVM arguments.

Example (Linux/macOS):
```bash
export DEEPSEEK_API_KEY=sk-yourdeepseekapikey
export BROWSER_CONTEXT_NUMBER=2
export BROWSER_MAX_ACTIVE_TABS=8
export BROWSER_DISPLAY_MODE=GUI
````

Example (JVM args):

```bash
-Ddeepseek.api.key=sk-yourdeepseekapikey
```

---

### 📝 Spring Boot Configuration Files

PulsarRPA supports standard Spring Boot configuration files.

Place your custom config in either the current directory (`.`) or the `./config` directory.

Example: `application-private.properties`

```properties
deepseek.api.key=
browser.context.number=2
browser.max.active.tabs=8
browser.display.mode=GUI
```

---

### 🐳 Docker Configuration

For Docker deployments, use environment variables in the `docker run` command.

**Linux/macOS:**

```bash
docker run -d -p 8182:8182 \
  -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} \
  -e BROWSER_CONTEXT_NUMBER=2 \
  -e BROWSER_MAX_ACTIVE_TABS=8 \
  -e BROWSER_DISPLAY_MODE=HEADLESS \
  galaxyeye88/pulsar-rpa:latest
```

**Windows (PowerShell):**

```powershell
docker run -d -p 8182:8182 `
  -e DEEPSEEK_API_KEY=$env:DEEPSEEK_API_KEY `
  -e BROWSER_CONTEXT_NUMBER=2 `
  -e BROWSER_MAX_ACTIVE_TABS=8 `
  -e BROWSER_DISPLAY_MODE=HEADLESS `
  galaxyeye88/pulsar-rpa:latest
```

---

## ⚙️ Common Configuration Options

* **`deepseek.api.key`**
  Your DeepSeek API key.

* **`browser.context.number`** *(default: 2)*
  Number of browser contexts (isolated, incognito-like sessions).
  Each context has its own cookies, local storage, and cache.

  > For `DEFAULT`, `SYSTEM_DEFAULT`, and `PROTOTYPE` browsers, this value is **1**.

* **`browser.max.active.tabs`** *(default: 8)*
  Maximum number of tabs per browser instance.

  > For `DEFAULT`, `SYSTEM_DEFAULT`, and `PROTOTYPE` browsers, this value is **1000**.

* **`browser.display.mode`** (`GUI` | `HEADLESS` | `SUPERVISED`)
  Controls how the browser is displayed:

    * `GUI`: Launches a visible browser window.
    * `HEADLESS`: Runs without a graphical window.
    * `SUPERVISED`: Linux-only; uses Xvfb for headless GUI simulation.

---

## 💡 Configuration Best Practices

1. 🔐 Use **environment variables** for credentials or sensitive values.
2. 📁 Use **configuration files** for structured or shared settings.
3. ⚡ Use **system properties** for quick runtime overrides.
4. 📝 Always **document changes** to ensure team transparency.
