package ai.platon.pulsar.persist;

public enum RetryScope {
    /**
     * Retry at crawl schedule level
     * */
    CRAWL,
    /**
     * Retry at job schedule level
     * */
    JOB,
    /**
     * Retry at fetch protocol, ignored in browser emulation mode
     * */
    PROTOCOL,
    /**
     * Change proxy and close all web drivers and retry
     * */
    PRIVACY,
    /**
     * Change proxy and retry
     * */
    PROXY,
    /**
     * Just refresh or load again using web driver
     * */
    WEB_DRIVER,
    /**
     * Retry inside browser, this can be done using javascript
     * */
    BROWSER
}
