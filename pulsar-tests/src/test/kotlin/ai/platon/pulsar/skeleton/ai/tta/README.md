
  Run all TTA tests:
  ./mvnw test -Dtest="*TTA*Test" -pl pulsar-tests

  Run specific test classes:
  ./mvnw test -Dtest="TextToActionSimpleTest" -pl pulsar-tests
  ./mvnw test -Dtest="TextToActionComprehensiveTests" -pl pulsar-tests
  ./mvnw test -Dtest="TextToActionElementInteractionTests" -pl pulsar-tests

  Run tests matching a pattern:
  ./mvnw test -Dtest="TextToAction*Test" -pl pulsar-tests

  Run with specific test method:
  ./mvnw test -Dtest="TextToActionSimpleTest#testMethodName" -pl pulsar-tests

