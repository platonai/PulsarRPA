# Stage 1: Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /build

# Copy project, use .dockerignore to control which files to copy
COPY pulsar-app .

RUN ls -l

# Copy JAR for use in the next stage
RUN cp $(find . -type f -name PulsarRPA.jar | head -n 1) /build/app.jar

# Stage 2: Run stage
FROM eclipse-temurin:21-jre-alpine AS runner

# Set working directory
WORKDIR /app

# Set timezone
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Install Chromium and necessary dependencies
RUN apk add --no-cache curl chromium nss freetype freetype-dev harfbuzz ca-certificates ttf-freefont dbus

# Set Chromium environment variables
# Ignore BROWSER_CONTEXT_NUMBER, BROWSER_MAX_OPEN_TABS if BROWSER_CONTEXT_MODE is set to DEFAULT
ENV JAVA_OPTS="-Xms2G -Xmx10G -XX:+UseG1GC" \
    DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} \
    PROXY_ROTATION_URL=${PROXY_ROTATION_URL} \
    BROWSER_CONTEXT_MODE=DEFAULT \
    BROWSER_CONTEXT_NUMBER=2 \
    BROWSER_MAX_OPEN_TABS=8 \
    BROWSER_DISPLAY_MODE=HEADLESS

# Copy build artifact
COPY --from=builder /build/app.jar app.jar

# Expose port (documentation only)
EXPOSE 8182

# Create app data directory
RUN mkdir -p ~/.pulsar/

# Create non-root user and set directory permissions
RUN addgroup --system --gid 1001 appuser && \
    adduser --system --uid 1001 --ingroup appuser appuser && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Add build arguments
LABEL maintainer="Vincent Zhang <ivincent.zhang@gmail.com>" \
      description="PulsarRPA: An AI-Enabled, Super-Fast, Thread-Safe Browser Automation Solution! 💖"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
