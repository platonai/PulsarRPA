# Browser4 Build & Test Guide

This document provides instructions for building the project and running tests on different platforms.

本文档提供了在不同平台上构建项目和运行测试的说明。

>💡对于国内开发者，我们强烈建议您按照 [这个](https://github.com/platonai/pulsar/blob/master/bin/tools/maven/maven-settings.md) 指导来加速构建。

## Basic Build Commands

### Build Without Tests

For Linux/macOS:
```shell
bin/build.sh
```

For Windows:
```shell
bin/build.ps1
```

### Build With Tests

For Linux/macOS:
```shell
bin/build.sh -test
```

For Windows:
```shell
bin/build.ps1 -test
```

### Clean, Build and Test

For Linux/macOS:
```shell
bin/build.sh -clean -test
```

For Windows:
```shell
bin/build.ps1 -clean -test
```

## Running Tests

### Test All Modules
Run all tests across all modules:
```shell
./mvnw test
```

### Test a Single Module
Test a specific module (example with pulsar-tests):
```shell
./mvnw -pl pulsar-tests test
```

### Test a Single Class
Test a specific class:
```shell
# Test the TestEventHandlers class
./mvnw -pl pulsar-tests -Dtest=ai.platon.pulsar.basic.crawl.TestEventHandlers test

# Test with specific parameters
./mvnw -pl pulsar-tests -Dtest=ai.platon.pulsar.test3.heavy.BrowserRotationTest -DBrowserRotationTest_TestFileCount=10000 test
```

### Test a Specific Method in a Class
Test a single method within a class:
```shell
./mvnw -pl pulsar-tests -Dtest=ai.platon.pulsar.basic.crawl.TestEventHandlers#whenLoadAListenableLink_ThenEventsAreTriggered test
```

### Test Specific Browser Rotation Scenarios
```shell
# Test sequential browser scenario
./mvnw -pl pulsar-tests -Dtest="ai.platon.pulsar.heavy.BrowserRotationTest#testWithSequentialBrowser" -DBrowserRotationTest_TestFileCount=10000 test
```

```shell
# Test temporary browser scenario
./mvnw -pl pulsar-tests -Dtest="ai.platon.pulsar.heavy.BrowserRotationTest#testWithTemporaryBrowser" -DBrowserRotationTest_TestFileCount=10000 test
```
