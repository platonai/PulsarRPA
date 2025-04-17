# 🛠️ Pulsar Configuration Guide

## 📋 Configuration Sources

PulsarRPA is a standard Spring Boot application, which supports multiple configuration sources:

1. 🔧 **Java Environment Variables**
2. ⚙️ **Java System Properties**
3. 📝 **Spring Boot `application.properties` or `application.yml`**

## 🔧 Configuration Methods

### 🌍 **Environment Variables**
Set configurations using environment variables:
```bash
export PRIVACY_CONTEXT_NUMBER=2
```


### ⚙️ **System Properties**
Set configurations programmatically in Java:
```java
System.setProperty("browser.max.active.tabs", "8");
System.setProperty("browser.display.mode", "GUI");
```


### 📝 **Spring Boot Configuration**

For REST API users, PulsarRPA supports standard Spring Boot configuration methods:

#### `application.properties`
```properties
browser.display.mode=GUI
privacy.context.number=2
browser.max.active.tabs=8
server.port=8182
```


#### `application.yml`
```yaml
privacy:
  context:
    number: 2
browser:
  display:
    mode: GUI
  max:
    active:
      tabs: 8
server:
  port: 8182
```


### 🐳 **Docker Configuration**
For Docker users, configurations can be set using environment variables:
```shell
docker run -d -p 8182:8182 \
  -e LLM_PROVIDER=volcengine \
  -e LLM_NAME=${YOUR-MODEL_NAME} \
  -e LLM_API_KEY=${YOUR-LLM_API_KEY} \
  galaxyeye88/pulsar-rpa:latest
```


## ⚙️ Common Configuration Options

### 🌐 **Browser Settings**
- `browser.max.active.tabs`: Maximum number of tabs allowed per browser (default: 8)
- `browser.display.mode`: Browser display mode (`GUI`, `HEADLESS`, or `SUPERVISED`)

### 🔒 **Privacy Context Settings**
- `privacy.context.number`: Number of privacy contexts (default: 2)

## 💡 Configuration Best Practices

1. 🔐 **Use environment variables** for sensitive information.
2. 📁 **Use configuration files** for complex settings.
3. ⚡ **Use system properties** for runtime overrides.
4. 📝 **Document your configuration changes** for team members.
