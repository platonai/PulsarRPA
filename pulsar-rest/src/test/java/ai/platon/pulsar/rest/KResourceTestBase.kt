package ai.platon.pulsar.rest

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.rest.rpc.PageResourceReference
import org.glassfish.jersey.test.JerseyTest
import org.junit.After
import org.junit.Before
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import javax.ws.rs.core.Application

open class KResourceTestBase : JerseyTest() {
    protected lateinit var conf: ImmutableConfig

    /**
     * TODO : create a mock site
     */
    protected lateinit var pageResourceReference: PageResourceReference

    @Before
    override fun setUp() {
        super.setUp()

        pageResourceReference = PageResourceReference(baseUri, client())
        pageResourceReference.fetch(seedUrl)
        pageResourceReference.fetch(detailUrl)
    }

    override fun configure(): Application {
        val applicationContext = ClassPathXmlApplicationContext("classpath:/rest-context/rest-test-context.xml")
        val application = applicationContext.getBean(MasterApplication::class.java)
        application.property("contextConfig", applicationContext)

        this.conf = applicationContext.getBean(ImmutableConfig::class.java)

        return application
    }

    @After
    override fun tearDown() {
        pageResourceReference.delete(seedUrl)
        pageResourceReference.delete(detailUrl)
        super.tearDown()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(ResourceTestBase::class.java)
        val seedUrl = "http://news.cqnews.net/html/2017-06/08/content_41874417.htm"
        val detailUrl = "http://news.163.com/17/0607/21/CMC14QCD000189FH.html"
    }
}
