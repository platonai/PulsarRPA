# 🛠️ Pulsar Configuration Guide

## 📋 Configuration Sources

PulsarRPA is a standard Spring Boot application, which supports multiple configuration sources:

1. 🔧 **Java Environment Variables**
2. ⚙️ **Java System Properties**
3. 📝 **Spring Boot `application.properties` or `application.yml`**

## 🔧 Configuration Methods

### 🌍 **OS environment variables / JVM system properties**

Example: `export DEEPSEEK_API_KEY=sk-abcdefghijklmn` or `-Ddeepseek.api.key=sk-abcdefghijklmn` in JVM args.

More examples:
```bash
export DEEPSEEK_API_KEY=
export BROWSER_CONTEXT_NUMBER=2
export BROWSER_MAX_ACTIVE_TABS=8
export BROWSER_DISPLAY_MODE=GUI
```

### 📝 **Spring Boot Configuration**

For REST API users, PulsarRPA supports standard Spring Boot configuration methods.
You can place your custom configuration files in the current directory (`.`) or in the `./config` subdirectory.

#### `application-private.properties`
```properties
deepseek.api.key=
browser.context.number=2
browser.max.active.tabs=8
browser.display.mode=GUI
```

### 🐳 **Docker Configuration**
For Docker users, configurations can be set using environment variables:

Linux:

```shell
docker run -d -p 8182:8182 \
  -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} \
  -e BROWSER_CONTEXT_NUMBER=2 \
  -e BROWSER_MAX_ACTIVE_TABS=8 \
  -e BROWSER_DISPLAY_MODE=HEADLESS \
  galaxyeye88/pulsar-rpa:latest
```

Windows (PowerShell):
```powershell
docker run -d -p 8182:8182 `
  -e DEEPSEEK_API_KEY=$env:DEEPSEEK_API_KEY `
  -e BROWSER_CONTEXT_NUMBER=2 `
  -e BROWSER_MAX_ACTIVE_TABS=8 `
  -e BROWSER_DISPLAY_MODE=HEADLESS `
  galaxyeye88/pulsar-rpa:latest
```

## ⚙️ Common Configuration Options

- `deepseek.api.key`: Your DeepSeek API key
- `browser.context.number`: Number of privacy contexts (default: 2)
- `browser.max.active.tabs`: Maximum number of tabs allowed per browser (default: 8)
- `browser.display.mode`: Browser display mode (`GUI`, `HEADLESS`, or `SUPERVISED`)

## 💡 Configuration Best Practices

1. 🔐 **Use environment variables** for sensitive information.
2. 📁 **Use configuration files** for complex settings.
3. ⚡ **Use system properties** for runtime overrides.
4. 📝 **Document your configuration changes** for team members.
