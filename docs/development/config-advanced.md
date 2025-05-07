# 🛠️ Pulsar Advanced Configuration Guide

> 💡 **Note:** For PulsarRPA developers only.

## 📋 Configuration Sources

PulsarRPA loads configuration in the following order of precedence:

1. 🔧 Java Environment Variables
2. ⚙️ Java System Properties
3. 📝 Spring Boot `application.properties` / `application.yml` (REST API only)
4. 📁 Files in `${PULSAR_DATA_HOME}/config/conf-enabled`

Where `${PULSAR_DATA_HOME}` is:

- 🪟 Windows: `C:\Users\<username>\.pulsar`
- 🐧 Linux/macOS: `/home/<username>/.pulsar`

### 🔑 Key Normalization

All keys are normalized to lowercase with underscores replaced by dots.

Example:
- `browser.max.active.tabs` → `browser.max.active.tabs`
- `BROWSER_MAX_ACTIVE_TABS` → also normalized to `browser.max.active.tabs`

## 🔧 Configuration Methods

### 1. 📚 Native API

#### 🌍 Environment Variables
```bash
export BROWSER_CONTEXT_NUMBER=2
````

#### ⚙️ System Properties

```java
System.setProperty("browser.max.active.tabs", "8");
System.setProperty("browser.display.mode", "GUI");
```

#### 📄 Configuration Files

Place `.xml` files in `${PULSAR_DATA_HOME}/config/conf-enabled`

```xml
<configuration>
  <property>
    <name>browser.max.active.tabs</name>
    <value>8</value>
  </property>
  <property>
    <name>browser.display.mode</name>
    <value>GUI</value>
  </property>
</configuration>
```

---

### 2. 🌐 REST API (Spring Boot)

#### 📝 application.properties

```properties
browser.context.number=2
browser.display.mode=GUI
browser.max.active.tabs=8
server.port=8182
```

#### 📝 application.yml

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

#### ✅ Spring Boot Load Order (highest to lowest)

1. **Command-line arguments**
   `--server.port=9000`

2. **`SPRING_APPLICATION_JSON`**
   `SPRING_APPLICATION_JSON={"server":{"port":9000}}`

3. **Env vars / JVM system properties**
   `export SERVER_PORT=9000` or `-Dserver.port=9000`

4. **Config files:**

   * `./config/application.properties`
   * `./application.properties`
   * `classpath:/config/application.properties`
   * `classpath:/application.properties`

5. **Defaults inside JAR**
   `src/main/resources/application.properties`

6. **@PropertySource files**
   Loaded only if explicitly declared.

7. **Hardcoded defaults in code**
   Example: `@Value("${timeout:30}")`

---

### 3. 🐳 Docker

```bash
docker run -d -p 8182:8182 \
  -e LLM_PROVIDER=volcengine \
  -e LLM_NAME=${YOUR-MODEL_NAME} \
  -e LLM_API_KEY=${YOUR-LLM_API_KEY} \
  galaxyeye88/pulsar-rpa:latest
```

---

## ⚙️ Common Options

### 🌐 Browser

* `browser.max.active.tabs`: Max tabs (default: 8)
* `browser.display.mode`: GUI, HEADLESS, or SUPERVISED

### 🔒 Privacy

* `browser.context.number`: Privacy context count (default: 2)

### 🤖 LLM

1. Copy template:

```bash
cp ${project.baseDir}/docs/config/llm/template/pulsar-deepseek.xml ${PULSAR_HOME}/conf-enabled/pulsar-deepseek.xml
```

2. Edit config with your model settings.

---

## 💡 Best Practices

1. 🔐 Use env vars for secrets
2. 📁 Use files for complex config
3. ⚡ Use system props for runtime override
4. 📂 Organize in `conf-enabled`
5. 📝 Document changes

---

## 🚨 Troubleshooting

* 🔍 Check precedence
* 🔒 Verify permissions
* 📂 Correct config path
* ❌ Validate file syntax
* 📊 Check logs for config errors
