# 第一阶段：构建阶段
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# 设置工作目录
WORKDIR /build

# 复制整个项目
COPY ../../pulsar-app/pulsar-master .

RUN ls -l

# 复制 JAR 以便在下一阶段使用
RUN cp $(find /build -name "*.jar" | grep "PulsarRPA.jar") /build/app.jar

# 第二阶段：运行阶段
FROM eclipse-temurin:21-jre-alpine AS runner

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apk add curl
# 安装 Chromium 和必要的依赖
RUN apk add --no-cache \
    chromium \
    nss \
    freetype \
    freetype-dev \
    harfbuzz \
    ca-certificates \
    ttf-freefont

# 设置 Chromium 环境变量
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/ \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium-browser

# 复制构建产物
COPY --from=builder /build/app.jar app.jar

# 设置环境变量
ENV JAVA_OPTS="-Xms2G -Xmx10G -XX:+UseG1GC -Dbrowser.display.mode=HEADLESS"

# 暴露端口
# 8182: api server
# 8082: H2 web server
# 9092: H2 TCP server

EXPOSE 8082
EXPOSE 8182

# 创建非 root 用户
RUN addgroup --system --gid 1001 appuser \
    && adduser --system --uid 1001 --ingroup appuser appuser

# 设置目录权限
RUN chown -R appuser:appuser /app

# 切换到非 root 用户
USER appuser

# 添加构建参数
LABEL maintainer="Vincent Zhang <ivincent.zhang@gmail.com>"
LABEL description="PulsarRPA: Your Ultimate AI-RPA Solution!"

# 启动命令，支持动态端口配置
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${SERVER_PORT:-8182}"]
