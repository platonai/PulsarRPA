package ai.platon.pulsar.filter

import ai.platon.pulsar.common.config.MutableConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
open class UrlFilterTestBase(val testResourcePrefix: String = "") {
    @Autowired
    open var conf: MutableConfig = MutableConfig()

    companion object {
        @JvmField
        val LOG = LoggerFactory.getLogger(RegexUrlFilterBaseTest::class.java)
    }
}
