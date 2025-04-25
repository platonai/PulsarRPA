# 🛠️ Pulsar Configuration Guide

## 📋 Configuration Sources

PulsarRPA is a standard Spring Boot application, which supports multiple configuration sources:

1. 🔧 **Java Environment Variables**
2. ⚙️ **Java System Properties**
3. 📝 **Spring Boot `application.properties` or `application.yml`**

## 🔧 Configuration Methods

### 🐳 **Docker Configuration**
For Docker users, configurations can be set using environment variables:

Linux:

```shell
docker run -d -p 8182:8182 \
  -e DEEPSEEK_API_KEY=${YOUR-DEEPSEEK_API_KEY} \
  -e PRIVACY_CONTEXT_NUMBER=2 \
  -e BROWSER_MAX_ACTIVE_TABS=8 \
  -e BROWSER_DISPLAY_MODE=HEADLESS \
  galaxyeye88/pulsar-rpa:latest
```

Windows (PowerShell):
```powershell
docker run -d -p 8182:8182 `
  -e DEEPSEEK_API_KEY=$env:YOUR_DEEPSEEK_API_KEY `
  -e PRIVACY_CONTEXT_NUMBER=2 `
  -e BROWSER_MAX_ACTIVE_TABS=8 `
  -e BROWSER_DISPLAY_MODE=HEADLESS `
  galaxyeye88/pulsar-rpa:latest
```

### 🌍 **Environment Variables**
Set configurations using environment variables:
```bash
export DEEPSEEK_API_KEY=
export PRIVACY_CONTEXT_NUMBER=2
export BROWSER_MAX_ACTIVE_TABS=8
export BROWSER_DISPLAY_MODE=GUI
```

### 📝 **Spring Boot Configuration**

For REST API users, PulsarRPA supports standard Spring Boot configuration methods:

#### `application.properties`
```properties
deepseek.api.key=
privacy.context.number=2
browser.max.active.tabs=8
browser.display.mode=GUI
```

## ⚙️ Common Configuration Options

- `deepseek.api.key`: Your DeepSeek API key
- `privacy.context.number`: Number of privacy contexts (default: 2)
- `browser.max.active.tabs`: Maximum number of tabs allowed per browser (default: 8)
- `browser.display.mode`: Browser display mode (`GUI`, `HEADLESS`, or `SUPERVISED`)

## 💡 Configuration Best Practices

1. 🔐 **Use environment variables** for sensitive information.
2. 📁 **Use configuration files** for complex settings.
3. ⚡ **Use system properties** for runtime overrides.
4. 📝 **Document your configuration changes** for team members.
