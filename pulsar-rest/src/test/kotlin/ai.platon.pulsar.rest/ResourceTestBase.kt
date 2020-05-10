package ai.platon.pulsar.rest

import ai.platon.pulsar.common.config.AppConstants
import org.junit.After
import org.junit.Before
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate

open class ResourceTestBase  {

    val ROOT_PATH = "api"

    val baseUri: String
    val restTemplate = RestTemplate()

    init {
        this.baseUri = String.format("http://%s:%d/%s", "localhost", AppConstants.DEFAULT_PULSAR_MASTER_PORT, ROOT_PATH)
    }


    @Before
    fun setUp() {
    }

//    fun configure(): Application {
//        val applicationContext = ClassPathXmlApplicationContext("classpath:/rest-context/rest-test-context.xml")
//        val application = applicationContext.getBean(MasterResourceConfig::class.java)
//        application.property("contextConfig", applicationContext)
//
//        this.conf = applicationContext.getBean(ImmutableConfig::class.java)
//
//        return application
//    }

    @After
    fun tearDown() {
    }

    companion object {
        val LOG = LoggerFactory.getLogger(ResourceTestBase::class.java)
    }
}
