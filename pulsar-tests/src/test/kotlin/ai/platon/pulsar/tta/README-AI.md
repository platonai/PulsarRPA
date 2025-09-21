# Text To Action Tests Guideline for AI

测试目标：

```shell
${project.baseDir}/browser4-spa/src/main/kotlin/ai/platon/pulsar/agent/README-AI.md
```

测试目录：

```shell
${project.baseDir}/pulsar-tests/src/test/kotlin/ai/platon/pulsar/tta
```

测试网页路径：

```shell
${project.baseDir}/pulsar-tests/src/main/resources/static/generated
```

提示：

1. 所有继承 WebDriverTestBase 的测试，将会启动一个网页服务器，提供测试网页
2. 检查测试网页 interactive-*.html，如果能满足你的测试需要，直接使用已存在的测试网页，如果没有能够满足你测试需要的网页，创建一个测试网页
