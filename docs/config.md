# Pulsar Config

Pulsar load configs from the following directory:

1. Java environment variable
2. Java system property
3. config files in `conf-enabled` directory

The `conf-enabled` directory is located at:
```kotlin
AppPaths.DATA_DIR.resolve("config").resolve("conf-enabled")
```
For example: `C:\Users\pereg\.pulsar\config\conf-enabled`

## LLM Config

To enable LLM, copy the LLM config to `conf-enabled` directory.

For example, copy `docs/config/llm/template/pulsar-deepseek.xml` to `conf-enabled/pulsar-deepseek.xml`
