package ai.platon.pulsar.persist;

/**
 * <p>RetryScope class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public enum RetryScope {
    /**
     * Retry in crawl schedule scope
     * */
    CRAWL,
    /**
     * Retry in job schedule scope
     * */
    JOB,
    /**
     * Retry in fetch protocol scope, ignored in browser emulation mode
     * */
    PROTOCOL,
    /**
     * Change the privacy context and retry
     * */
    PRIVACY,
    /**
     * Change the proxy ip and retry
     * */
    PROXY,
    /**
     * Refresh in the same web driver
     * */
    WEB_DRIVER,
    /**
     * Retry in the same browser instance
     * */
    BROWSER
}
