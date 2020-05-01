package ai.platon.pulsar.filter

import ai.platon.pulsar.common.config.MutableConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
open class UrlFilterTestBase {
    @JvmField
    @Autowired
    protected var conf: MutableConfig = MutableConfig()

    companion object {
        @JvmField
        val TEST_DIR = System.getProperty("test.data", ".")
        @JvmField
        val LOG = LoggerFactory.getLogger(RegexUrlFilterBaseTest::class.java)
    }
}
