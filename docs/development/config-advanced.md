# 🛠️ Pulsar Configuration Guide

## 📋 Configuration Sources

PulsarRPA loads configurations from multiple sources in the following order of precedence:

1. 🔧 Java Environment Variables
2. ⚙️ Java System Properties
3. 📝 Spring Boot `application.properties` or `application.yml` (REST API only)
4. 📁 Configuration files in `${PULSAR_DATA_HOME}/config/conf-enabled` directory

Where `${PULSAR_DATA_HOME}` is the data directory of PulsarRPA:
- 🪟 Windows: `C:\Users\<username>\.pulsar`
- 🐧 Linux/macOS: `/home/<username>/.pulsar`

### 🔑 Configuration Key Normalization

PulsarRPA normalizes all configuration keys to lowercase and replaces underscores with dots. 

For example:

* 📝 `browser.max.active.tabs` is normalized to `browser.max.active.tabs`
* 📝 Both forms are associated with the same key: `browser.max.active.tabs`

## 🔧 Configuration Methods

### 1. 📚 Native API Configuration

For native API users, configurations can be set through:

#### 🌍 Environment Variables
```bash
export PRIVACY_CONTEXT_NUMBER=2
```

#### ⚙️ System Properties
```java
System.setProperty("browser.max.active.tabs", "8");
System.setProperty("browser.display.mode", "GUI");
```

#### 📄 Configuration Files
Place XML configuration files in the `${PULSAR_DATA_HOME}/config/conf-enabled` directory. The system will load all `.xml` files from this directory.

Example configuration file structure:
```xml
<?xml version="1.0" encoding="UTF-8"?>
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

### 2. 🌐 REST API Configuration

For REST API users, PulsarRPA supports standard Spring Boot configuration methods:

#### 📝 application.properties
```properties
privacy.context.number=2
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

### 3. 🐳 Docker Configuration

For Docker users, configurations can be set using environment variables:

```shell
docker run -d -p 8182:8182 \
  -e llm.provider=volcengine \
  -e llm.name=${YOUR-MODEL_NAME} \
  -e llm.apiKey=${YOUR-LLM_API_KEY} \
  galaxyeye88/pulsar-rpa:latest
```

## ⚙️ Common Configuration Options

### 🌐 Browser Settings
- `browser.max.active.tabs`: Maximum number of tabs allowed per browser (default: 8)
- `browser.display.mode`: Browser display mode (GUI, HEADLESS, or SUPERVISED)

### 🔒 Privacy Context Settings
- `privacy.context.number`: Number of privacy contexts (default: 2)

### 🤖 LLM Configuration

To enable LLM functionality:

1. 📋 Copy the LLM configuration template:
    ```bash
    cp ${project.baseDir}/docs/config/llm/template/pulsar-deepseek.xml ${PULSAR_HOME}/conf-enabled/pulsar-deepseek.xml
    ```

2. ✏️ Modify the configuration file with your specific LLM settings.

## 💡 Configuration Best Practices

1. 🔐 Use environment variables for sensitive information
2. 📁 Use configuration files for complex settings
3. ⚡ Use system properties for runtime overrides
4. 📂 Keep configuration files organized in the `conf-enabled` directory
5. 📝 Document your configuration changes for team members

## 🚨 Troubleshooting

If configurations are not being applied:
1. 🔍 Check the order of precedence
2. 🔒 Verify file permissions
3. 📂 Ensure configuration files are in the correct directory
4. ❌ Check for syntax errors in configuration files
5. 📊 Review application logs for configuration-related errors
