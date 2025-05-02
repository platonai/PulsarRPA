# Additional LLM Configuration Methods

## ⚙️ XML Configuration

Copy one of the XML files in the directory to `~/.pulsar/config/conf-enabled`.

### Example Commands:

**Linux/macOS:**

```bash
cp docs/config/llm/template/pulsar-volcengine-deepseek.xml ~/.pulsar/config/conf-enabled
```

**Windows:**

```powershell
cp docs/config/llm/template/pulsar-volcengine-deepseek.xml $env:USERPROFILE\.pulsar\config\conf-enabled
```

> 💡 **Note:** Remember change the parameters to your own.

## ⚙️ OpenAI Compatible LLM Configuration

To use OpenAI compatible API, follow these steps:

1. Download the latest PulsarRPA JAR file:

   ```shell
   # Linux/macOS and Windows (if curl is available)
   curl -L -o PulsarRPA.jar https://github.com/platonai/PulsarRPA/releases/download/v3.0.2/PulsarRPA.jar
   ```

2. Run the JAR with the required environment variables:

   ```shell
   java -DOPENAI_API_KEY=${DEEPSEEK_API_KEY} \
   -DOPENAI_MODEL_NAME=${OPENAI_MODEL_NAME} \
   -DOPENAI_BASE_URL=${OPENAI_BASE_URL}
   -jar PulsarRPA.jar
   ```
