package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.boot.autoconfigure.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.session.PulsarSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureDataMongo
class IntegrationTestBase {
    
    @LocalServerPort
    protected var port = 0
    
    @Autowired
    lateinit var session: PulsarSession
    
    @Autowired
    lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    lateinit var unmodifiedConfig: ImmutableConfig
    
    val baseUri get() = String.format("http://%s:%d", "localhost", port)
}
