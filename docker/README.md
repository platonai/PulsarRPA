# 🐳 Docker for Development

## 🛠️ Create a Docker Image for Development

```shell
docker build -t pulsar-rpa-dev .
```

## 🚀 Run Docker Image

```shell
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} galaxyeye88/pulsar-rpa:latest
```

## 🏠 Run Local Docker Image

```shell
docker run -d -p 8182:8182 -e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} pulsar-rpa-dev:latest
```
## ⚙️ Run Default Configuration

```shell
export DEEPSEEK_API_KEY=YOUR_API_KEY
docker compose up -d
```

## 🌐 Run All Services

```shell
docker compose up -d --profile proxy
```

## 🗄️ Run MongoDB Only

```shell
docker compose up -d mongodb
```

## 🔗 Run ProxyHub Only

```shell
docker compose up -d proxyhub
```
