# 🚀 jemalloc 内存优化指南

## 📝 概述

jemalloc 5.3.1 可以大幅度提高 Linux 底层内存利用率，约提升 20%。特别适用于：
- Linux 下的 Chrome 浏览器
- 其他 C++ 底层应用
- Java 应用处理 ZIP 包等场景
- MongoDB

## ⚙️ 安装步骤

### 📦 安装 jemalloc
```shell
apk add jemalloc
```

安装输出示例:
```
fetch https://dl-cdn.alpinelinux.org/alpine/v3.21/main/x86_64/APKINDEX.tar.gz
fetch https://dl-cdn.alpinelinux.org/alpine/v3.21/community/x86_64/APKINDEX.tar.gz
(1/1) Installing jemalloc (5.3.0-r6)
```

### ☕ 在 Java 中使用 jemalloc
```shell
export LD_PRELOAD="/usr/lib/libjemalloc.so.2"
java -server -jar foo.jar
```

在 Java 中启动 Chrome 时的设置方法:
```shell
LD_PRELOAD=/usr/lib/libjemalloc.so.2 java -server -jar foo.jar
```

### ✅ 验证 jemalloc 是否启动
```shell
lsof -n | grep jemalloc
ps -ef | grep jemalloc
```

### 📊 性能监控工具
```shell
apk add htop
```

## 🔧 多层次优化方案

### 🗜️ 压缩优化 - zstd
```shell
apk add zstd
```

zstd 适用场景:
- 本地文件压缩存储
- 本地缓存
- MongoDB 的 zstd 压缩

在 Java 项目中添加 zstd 依赖:
```xml
<dependency>
    <groupId>com.github.luben</groupId>
    <artifactId>zstd-jni</artifactId>
    <version>1.5.6-6</version>
</dependency>
```

### 💡 JVM 优化策略

- **IBM Semeru OpenJ9** 替代 OpenJDK
    - JVM 内存优化可达 50%
    - 下载地址: https://developer.ibm.com/languages/java/semeru-runtimes/downloads/

- **垃圾回收器优化**
    - JDK 17: 使用 ZGC，GC 时间降低到 10ms 以内，避免 STW
    - JDK 21: 使用分代 ZGC，进一步降低老年代 GC

- **备选方案**
    - OpenJDK Shenandoah GC
    - 阿里龙井 JDK (但 JDK 17/21 优先考虑 IBM OpenJ9)

### 🌐 Chrome 优化

- 优化请求参数
- 使用 Chrome 119+ 版本支持的 zstd 压缩
- 启用 HTTP/3 (h3)

## 🏆 综合优化效果

1. **缓存优化**: 使用 pzstd 进行并行压缩，IO 降低 90%，体积减少 90%，CPU 消耗极少
2. **JVM 优化**: IBM Semeru OpenJ9 替代 OpenJDK，内存占用减少 50%
3. **内存分配优化**: jemalloc 5.3.0 使 Chrome 内存优化 20%，JVM 调用 Linux SO 内存优化 20%
4. **GC 优化**: JDK 17/21 使用 ZGC，大幅降低 GC 停顿时间
5. **浏览器优化**: Chrome 参数优化，提高页面初始化速度

这些优化整合后，可显著提升性能和稳定性，创建一个高度优化的解决方案。
