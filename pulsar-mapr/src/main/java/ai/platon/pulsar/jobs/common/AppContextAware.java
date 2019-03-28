package ai.platon.pulsar.jobs.common;

import org.springframework.context.ApplicationContext;

/**
 * Created by vincent on 17-4-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
public interface AppContextAware {

    ApplicationContext getApplicationContext();

    void setApplicationContext(ApplicationContext applicationContext);
}
