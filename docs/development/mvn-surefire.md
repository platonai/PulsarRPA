## Maven Test 进程控制概述

Browser4 项目使用 Maven Surefire 插件来管理测试进程的执行，该插件提供了丰富的进程控制功能。

### 1. Surefire 插件配置

在项目的根 `pom.xml` 中，配置了 Maven Surefire 插件：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <excludedGroups>TimeConsumingTest,ExternalServiceTest</excludedGroups>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>${junit5.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

### 2. 测试控制选项

#### A. 跳过测试
项目提供了多种方式来跳过测试：

- **通过构建脚本**：
  ```bash
  # 默认跳过测试
  ./bin/build.sh
  
  # 启用测试
  ./bin/build.sh -test
  ```

- **通过 Maven 参数**：
  ```bash
  # 跳过测试编译和执行
  mvn install -DskipTests
  
  # 跳过测试编译
  mvn install -Dmaven.test.skip=true
  ```

#### B. Profile 配置
项目定义了不同的 Profile 来控制测试行为：

- **dev profile**（默认激活）：
  ```xml
  <profile>
      <id>dev</id>
      <activation>
          <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
          <skipTests>false</skipTests>
          <maven.javadoc.skip>true</maven.javadoc.skip>
      </properties>
  </profile>
  ```

- **ci profile**（CI 环境）：
  ```xml
  <profile>
      <id>ci</id>
      <properties>
          <skipTests>false</skipTests>
          <maven.test.failure.ignore>false</maven.test.failure.ignore>
      </properties>
  </profile>
  ```

### 3. 测试分组和过滤

#### A. 排除特定测试组
```xml
<configuration>
    <excludedGroups>TimeConsumingTest,ExternalServiceTest</excludedGroups>
</configuration>
```

#### B. 运行特定测试
```bash
# 测试特定类的特定方法
./mvnw -Pall-modules -pl pulsar-tests \
  -Dtest=ai.platon.pulsar.basic.crawl.TestEventHandlers#whenLoadAListenableLink_ThenEventsAreTriggered test

# 测试特定浏览器场景
./mvnw -Pall-modules -pl pulsar-tests \
  -Dtest="ai.platon.pulsar.heavy.BrowserRotationTest#testWithSequentialBrowser" \
  -DBrowserRotationTest_TestFileCount=10000 test
```

### 4. JVM 配置

项目使用 `.mvn/jvm.config` 文件来配置 JVM 参数：

```
--add-opens jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED 
--add-opens jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED 
--add-opens jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED 
--add-opens jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED 
--add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED 
--add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED 
--add-opens jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED 
--add-opens jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
```

这些配置主要是为了解决 "Kapt is not compatible with JDK 16+" 的问题。

### 5. 进程控制最佳实践

#### A. 并发控制
虽然在当前配置中没有显式设置，但 Surefire 插件支持以下并发控制选项：

```xml
<configuration>
    <!-- 并发执行测试类 -->
    <parallel>classes</parallel>
    <threadCount>4</threadCount>
    
    <!-- 或者按方法并发 -->
    <parallel>methods</parallel>
    <threadCount>10</threadCount>
    
    <!-- 设置超时时间 -->
    <forkedProcessTimeoutInSeconds>1800</forkedProcessTimeoutInSeconds>
</configuration>
```

#### B. 内存管理
```xml
<configuration>
    <!-- 设置 JVM 参数 -->
    <argLine>-Xmx2048m -XX:MaxPermSize=256m</argLine>
    
    <!-- 每个测试类使用新的 JVM -->
    <forkCount>1</forkCount>
    <reuseForks>false</reuseForks>
</configuration>
```

### 6. 测试执行模式

#### A. 开发模式
```bash
# 快速构建，跳过测试
./bin/build.sh

# 运行特定模块测试
mvn test -pl pulsar-common
```

#### B. CI/CD 模式
```bash
# 完整测试，包含代码覆盖率
mvn clean verify -Pci

# 带部署的完整流程
./bin/release/maven-deploy.sh -test
```

### 7. 故障处理

#### A. 测试失败处理
```xml
<properties>
    <!-- CI 环境不忽略测试失败 -->
    <maven.test.failure.ignore>false</maven.test.failure.ignore>
</properties>
```

#### B. 调试模式
```bash
# 启用调试输出
mvn test -X

# 运行单个测试类
mvn test -Dtest=YourTestClass

# 跳过特定测试
mvn test -Dtest='!*IntegrationTest'
```

这个进程控制系统为 Browser4 项目提供了灵活的测试执行策略，既支持快速开发迭代，也满足了 CI/CD 环境的严格测试要求。