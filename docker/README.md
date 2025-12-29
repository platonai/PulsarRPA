# 🐳 Docker for Development

## 🛠️ Build Local Development Image

```bash
docker build -t browser4-dev .
````

## 🏠 Run Local Docker Image

```bash
docker run -p 8182:8182 \
  -e VOLCENGINE_API_KEY=${VOLCENGINE_API_KEY} \
  browser4-dev:latest
```

> 💡 Please make sure you have set `VOLCENGINE_API_KEY` environment.

## ✅ Test Browser4 API

```bash
curl -X POST "http://localhost:8182/api/ai/command" \
  -H "Content-Type: text/plain" \
  -d '
    Go to https://www.amazon.com/dp/B08PP5MSVB

    After browser launch: clear browser cookies.
    After page load: scroll to the middle.

    Summarize the product.
    Extract: product name, price, ratings.
    Find all links containing /dp/.
  '
```

## 🚀 Run Hosted Docker Image

```bash
docker run -d -p 8182:8182 \
  -e VOLCENGINE_API_KEY=${VOLCENGINE_API_KEY} \
  galaxyeye88/browser4:latest
```

## ⚙️ Run with Docker Compose

```bash
export VOLCENGINE_API_KEY=your-api-key
# export PROXY_ROTATION_URL=https://your-proxy-provider.com/rotation-endpoint
docker compose up -d
```

## 🌐 Run Docker Compose with Proxy Profile

```bash
docker compose up -d --profile proxy
```

## 🗄️ Run Only MongoDB Service

```bash
docker compose up -d mongodb
```

## 🔗 Run Only ProxyHub Service

```bash
docker compose up -d proxyhub
```
