package ai.platon.pulsar.rest

import ai.platon.pulsar.common.config.ImmutableConfig
import org.glassfish.jersey.test.JerseyTest
import org.junit.After
import org.junit.Before
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import javax.ws.rs.core.Application

open class ResourceTestBase : JerseyTest() {
    protected lateinit var conf: ImmutableConfig

    /**
     * TODO : create a mock site
     */

    @Before
    override fun setUp() {
        super.setUp()
    }

    override fun configure(): Application {
        val applicationContext = ClassPathXmlApplicationContext("classpath:/rest-context/rest-test-context.xml")
        val application = applicationContext.getBean(MasterResourceConfig::class.java)
        application.property("contextConfig", applicationContext)

        this.conf = applicationContext.getBean(ImmutableConfig::class.java)

        return application
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    companion object {
        val log = LoggerFactory.getLogger(ResourceTestBase::class.java)
    }
}
