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
mvn -Pall-modules test
```

Test a single module
```shell
# Test only the pulsar-tests module
mvn -pl pulsar-tests
```

Test a single class
```shell
# Test only the TestEventHandlers class
mvn -pl pulsar-tests -Dtest=ai.platon.pulsar.test.crawl.TestEventHandlers test
mvn -pl pulsar-tests -Dtest=ai.platon.pulsar.test3.heavy.BrowserRotationTest test
```

Test a single method
```shell
# Test only the TestEventHandlers class
mvn -pl pulsar-tests -Dtest=ai.platon.pulsar.test.crawl.TestEventHandlers#whenLoadAListenableLink_ThenEventsAreTriggered test
```
