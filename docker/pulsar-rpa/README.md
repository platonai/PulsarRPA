# Docker for development

Create a docker image for development:
```shell
docker build -t pulsar-rpa-dev . -f docker/pulsar-rpa/Dockerfile
```

Run all services:
```shell
docker-compose -f docker-compose.yaml up -d
```

Run MongoDB only:
```shell
docker-compose -f docker-compose.yaml up -d mongodb
```

Run ProxyHub only:
```shell
docker-compose -f docker-compose.yaml up -d proxyhub
```
