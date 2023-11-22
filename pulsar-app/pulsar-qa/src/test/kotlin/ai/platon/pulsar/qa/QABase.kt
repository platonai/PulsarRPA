package ai.platon.pulsar.qa

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class QABase {

}
