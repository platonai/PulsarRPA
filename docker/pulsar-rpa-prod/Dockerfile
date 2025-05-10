# 第一阶段：构建阶段
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# 设置工作目录
WORKDIR /build

# 复制项目的 Maven 相关文件（避免拷贝源码导致缓存失效）
COPY ../../pom.xml .
# 下载所有依赖并进行编译（如果依赖没有变，则会使用缓存）
RUN mvn dependency:go-offline -B

# 复制整个项目
COPY ../.. .

RUN ls -l

# 构建应用
RUN mvn clean install -Pall-modules -DskipTests \
    -Dmaven.javadoc.skip=true \
    -Dmaven.source.skip=true

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

ENV BROWSER_DISPLAY_MODE=HEADLESS \
    PRIVACY_AGENT_GENERATOR_CLASS=ai.platon.pulsar.skeleton.crawl.fetch.privacy.RandomPrivacyAgentGenerator

# 设置环境变量
ENV JAVA_OPTS="-Xms2G -Xmx10G -XX:+UseG1GC"

# 暴露端口
# 8182: api server
# 8082: H2 web server
# 9092: H2 TCP server

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
LABEL description="PulsarRPA: An AI-Enabled, Super-Fast, Thread-Safe Browser Automation Solution! 💖"

# 启动命令，支持动态端口配置
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
