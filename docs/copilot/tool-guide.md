# Tool Guide For AI

## mvn

Windows:

```powershell
mvnw.cmd -v
mvnw.cmd -pl pulsar-tests -Dtest="*E2ETest*" test
mvnw.cmd -pl pulsar-tests -Dtest="*ChromeDomServiceE2ETest*" test -D"spotless.apply.skip=true"

mvnw.cmd -pl pulsar-tests -Dtest=ChromeDomServiceFullCoverageTest test
# Failed: mvnw.cmd -pl pulsar-tests -am -Dtest=ChromeDomServiceFullCoverageTest test

cmd /c D:\Browser4\mvnw.cmd -q -DskipTests package
cmd /c D:\Browser4\mvnw.cmd -q -pl pulsar-tests -am -DskipTests test-compile

cd /d D:\Browser4 && mvnw.cmd -q -DskipTests compile
# Failed: cd /d D:\Browser4 && mvnw.cmd -q -DskipTests compile

mvnw.cmd -q -pl pulsar-core/pulsar-skeleton -am -DskipTests package
mvnw.cmd -pl pulsar-core/pulsar-skeleton -am -DskipTests -B package

cd D:\Browser4 && mvnw.cmd -pl pulsar-tests -Dtest="ai.platon.pulsar.skeleton.ai.PulsarAgentExtractObserveE2ETest" test -DskipITs
# Failed: cd /d D:\Browser4 && mvnw.cmd -pl pulsar-tests -am -Dtest=ai.platon.pulsar.skeleton.ai.PulsarAgentExtractObserveE2ETest test -DskipITs
```
