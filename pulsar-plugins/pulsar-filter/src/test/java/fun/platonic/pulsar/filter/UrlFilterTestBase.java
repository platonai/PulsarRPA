package fun.platonic.pulsar.filter;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.MutableConfig;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by vincent on 17-4-12.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
@ContextConfiguration(locations = {"classpath:/test-context/filter-beans.xml"})
public class UrlFilterTestBase {

    public static final String TEST_DIR = System.getProperty("test.data", ".");
    protected static final Logger LOG = LoggerFactory.getLogger(RegexUrlFilterBaseTest.class);
    @Autowired
    protected MutableConfig conf;
}
