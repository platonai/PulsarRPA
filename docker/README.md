# Docker for development

Create a docker image for development:
```shell
docker build -t pulsar-rpa-dev .
```

Run all services:
```shell
docker compose -f docker-compose.yaml up -d --profile proxy
```

Run MongoDB only:
```shell
docker compose -f docker-compose.yaml up -d mongodb
```

Run ProxyHub only:
```shell
docker compose -f docker-compose.yaml up -d proxyhub
```
