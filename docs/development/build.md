Tests
=

Build without tests
```shell
bin/build.sh
```

Build with tests
```shell
bin/build.sh -test
```

Clean, build and test
```shell
bin/build.sh -clean -test
```

Test all modules
```shell
./mvnw -Pall-modules test
```

Test a single module
```shell
# Test only the pulsar-tests module
./mvnw -Pall-modules -pl pulsar-tests
```

Test a single class
```shell
# Test only the TestEventHandlers class
./mvnw -Pall-modules -pl pulsar-tests -Dtest=ai.platon.pulsar.test.crawl.TestEventHandlers test
./mvnw -Pall-modules -pl pulsar-tests -Dtest=ai.platon.pulsar.test3.heavy.BrowserRotationTest -DBrowserRotationTest_TestFileCount=10000 test

./mvnw -Pall-modules -pl pulsar-tests -Dtest="ai.platon.pulsar.test3.heavy.BrowserRotationTest#testWithSequentialBrowser" -DBrowserRotationTest_TestFileCount=10000 test
./mvnw -Pall-modules -pl pulsar-tests -Dtest="ai.platon.pulsar.test3.heavy.BrowserRotationTest#testWithTemporaryBrowser" -DBrowserRotationTest_TestFileCount=10000 test
```

Test a single method
```shell
# Test only the TestEventHandlers class
./mvnw -Pall-modules -pl pulsar-tests -Dtest=ai.platon.pulsar.test.crawl.TestEventHandlers#whenLoadAListenableLink_ThenEventsAreTriggered test
```
