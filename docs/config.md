# 🛠️ PulsarRPA Configuration Guide

## 📋 Configuration Sources

PulsarRPA is a standard Spring Boot application and supports multiple configuration sources in order of precedence:

1. 🔧 **Environment Variables**
2. ⚙️ **JVM System Properties**
3. 📝 **Spring Boot `application.properties` or `application.yml`**

---

## 🔧 Configuration Methods

### 🌍 Environment Variables / JVM System Properties

You can configure PulsarRPA using either OS environment variables or JVM system properties.

#### 💻 Example - OS environment variables

For standard desktop usage:
```bash
export DEEPSEEK_API_KEY=sk-yourdeepseekapikey
```

If you want to use your daily used browser profile (remember closed the browser first):
```bash
export BROWSER_CONTEXT_MODE=SYSTEM_DEFAULT
```

For high-performance parallel crawling:

```bash
export BROWSER_CONTEXT_MODE=SEQUENTIAL
export BROWSER_CONTEXT_NUMBER=2
export BROWSER_MAX_ACTIVE_TABS=8
export BROWSER_DISPLAY_MODE=HEADLESS
```

#### ☕ Example – JVM Arguments

Set configuration via command-line JVM args:

```bash
-Ddeepseek.api.key=sk-yourdeepseekapikey
```

---

### 📝 Spring Boot Configuration Files

PulsarRPA supports standard Spring Boot configuration files.

Place your custom config in either the current directory (`.`) or the `./config` directory.

Example: `application-private.properties`

For desktop user:

```properties
deepseek.api.key=
```

For high performance, parallel crawling users:
```properties
browser.context.mode=SEQUENTIAL
browser.context.number=2
browser.max.active.tabs=8
browser.display.mode=HEADLESS
```

---

### 🐳 Docker Configuration

For Docker deployments, use environment variables in the `docker run` command.

**Linux/macOS:**

```bash
docker run -d -p 8182:8182 \
  -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} \
  -e BROWSER_CONTEXT_MODE=SEQUENTIAL \
  -e BROWSER_CONTEXT_NUMBER=2 \
  -e BROWSER_MAX_ACTIVE_TABS=8 \
  -e BROWSER_DISPLAY_MODE=HEADLESS \
  galaxyeye88/pulsar-rpa:latest
```

**Windows (PowerShell):**

```powershell
docker run -d -p 8182:8182 `
  -e DEEPSEEK_API_KEY=$env:DEEPSEEK_API_KEY `
  -e BROWSER_CONTEXT_MODE=SEQUENTIAL `
  -e BROWSER_CONTEXT_NUMBER=2 `
  -e BROWSER_MAX_ACTIVE_TABS=8 `
  -e BROWSER_DISPLAY_MODE=HEADLESS `
  galaxyeye88/pulsar-rpa:latest
```

---

## ⚙️ Common Configuration Options

* **`deepseek.api.key`**
  Your DeepSeek API key.

- **`browser.context.mode`** (`DEFAULT` | `SYSTEM_DEFAULT` | `PROTOTYPE` | `SEQUENTIAL` | `TEMPORARY`)  
  Defines how the user data directory is assigned for each browser instance.

  - `DEFAULT`: Uses the default PulsarRPA-managed user data directory.
  - `SYSTEM_DEFAULT`: Uses the system's default browser profile (e.g., your personal Chrome/Edge profile).
  - `PROTOTYPE` **[Advanced]**: Uses a predefined prototype user data directory.
    - All `SEQUENTIAL` and `TEMPORARY` modes inherit from this prototype.
  - `SEQUENTIAL` **[Advanced]**: Selects a user data directory from a managed pool to enable sequential isolation.
  - `TEMPORARY` **[Advanced]**: Generates a new, isolated user data directory for each browser instance.

* **`browser.context.number`** *(default: 2)*
  Number of browser contexts (isolated, incognito-like sessions).
  Each context has its own cookies, local storage, and cache.

  > For `DEFAULT`, `SYSTEM_DEFAULT`, and `PROTOTYPE` browser contexts, this value is **1**.

* **`browser.max.active.tabs`** *(default: 8)*
  Maximum number of tabs per browser instance.

  > For `DEFAULT`, `SYSTEM_DEFAULT`, and `PROTOTYPE` browser contexts, there is **no limit**.

* **`browser.display.mode`** (`GUI` | `HEADLESS` | `SUPERVISED`)
  Controls how the browser is displayed:

    * `GUI`: Launches a visible browser window.
    * `HEADLESS`: Runs without a graphical window.
    * `SUPERVISED`: Linux-only; uses Xvfb for headless GUI simulation.

### 📦 `browser.context.mode` Comparison Table

| Mode           | Description                                                                 | User Data Directory Behavior                             | Use Case            |
|----------------|-----------------------------------------------------------------------------|-----------------------------------------------------------|---------------------|
| `DEFAULT`      | Uses the PulsarRPA-managed default profile.                                 | Shared across Pulsar sessions (not your system browser).  | General purpose     |
| `SYSTEM_DEFAULT` | Uses the system browser's default profile.                                | Shares your daily-used browser profile.                   | For quick integration or debugging with real session data |
| `PROTOTYPE` ⚠️ | **[Advanced]** Uses a predefined prototype profile.                         | Acts as the base for `SEQUENTIAL` and `TEMPORARY`.        | Controlled state inheritance |
| `SEQUENTIAL` ⚠️ | **[Advanced]** Picks a profile from a pool sequentially.                   | Rotates through a pool of pre-initialized directories.     | Avoid session reuse in batch runs |
| `TEMPORARY` ⚠️  | **[Advanced]** Creates a new, isolated profile for each browser instance. | Discarded after session ends.                             | Maximum isolation / stateless crawling |

---

## 💡 Configuration Best Practices

1. 🔐 Use **environment variables** for credentials or sensitive values.
2. 📁 Use **configuration files** for structured or shared settings.
3. ⚡ Use **system properties** for quick runtime overrides.
4. 📝 Always **document changes** to ensure team transparency.
