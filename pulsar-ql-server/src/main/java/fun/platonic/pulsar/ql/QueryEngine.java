package fun.platonic.pulsar.ql;

import com.google.common.cache.*;
import fun.platonic.pulsar.Pulsar;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.config.MutableConfig;
import fun.platonic.pulsar.crawl.fetch.TaskStatusTracker;
import org.h2.api.ErrorCode;
import org.h2.message.DbException;
import org.h2.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static fun.platonic.pulsar.common.PulsarConstants.APP_CONTEXT_CONFIG_LOCATION;
import static fun.platonic.pulsar.common.config.CapabilityTypes.*;

/**
 * The QueryEngine fuses h2database and pulsar big data engine
 * So we can use SQL to do big data tasks, include but not limited:
 * <ul>
 * <li>Web spider</li>
 * <li>Web scraping</li>
 * <li>Search engine</li>
 * <li>Collect data from variable data source</li>
 * <li>Information extraction</li>
 * <li>TODO: NLP processing</li>
 * <li>TODO: knowledge graph</li>
 * <li>TODO: machine learning</li>
 * </ul>
 */
public final class QueryEngine implements AutoCloseable {
    public static final Logger LOG = LoggerFactory.getLogger(QueryEngine.class);

    private static QueryEngine INSTANCE;

    private Pulsar pulsar;

    private final ConfigurableApplicationContext applicationContext;

    private final ImmutableConfig conf;
    /**
     * The sessions container
     * A session will be closed if it's expired or the pool is full
     */
    private final LoadingCache<DbSession, QuerySession> sessions;

    private final TaskStatusTracker taskStatusTracker;

    private AtomicBoolean closed = new AtomicBoolean();

    private QueryEngine(ClassPathXmlApplicationContext applicationContext) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        this.applicationContext = applicationContext;
        this.applicationContext.registerShutdownHook();

        this.conf = applicationContext.getBean(MutableConfig.class);

        this.sessions = CacheBuilder.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener(new SessionRemovalListener())
                .build(new SessionCacheLoader());

        this.taskStatusTracker = applicationContext.getBean(TaskStatusTracker.class);
    }

    public static QueryEngine getInstance() {
        if (INSTANCE == null) {
            // TODO: a better way to initialize QueryEngine
            System.setProperty(PULSAR_CONFIG_RESOURCE_PREFIX, "ql-conf");

            String configLocation = System.getProperty(APPLICATION_CONTEXT_CONFIG_LOCATION, APP_CONTEXT_CONFIG_LOCATION);
            ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(configLocation);
            INSTANCE = new QueryEngine(applicationContext);
        }

        return INSTANCE;
    }

    public ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public ImmutableConfig getConf() {
        return conf;
    }

    public Pulsar getPulsar() {
        return pulsar;
    }

    /**
     * Get a query session from h2 session
     */
    public QuerySession getSession(DbSession dbSession) {
        try {
            return sessions.get(dbSession);
        } catch (ExecutionException e) {
            throw DbException.get(ErrorCode.DATABASE_IS_CLOSED, e);
        }
    }

    public QuerySession createQuerySession(@Nonnull DbSession dbSession) {
        QuerySession querySession = new QuerySession(this, dbSession, applicationContext);

        sessions.put(dbSession, querySession);

        return querySession;
    }

    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }

        LOG.info("[Destruction] Destructing QueryEngine ...");

        for (QuerySession pulsarSession : sessions.asMap().values()) {
            pulsarSession.close();
        }

        sessions.cleanUp();

        taskStatusTracker.close();
    }

    /**
     * The class factory used by h2database to load classes for udfs and udfas
     */
    public static class ClassFactory implements Utils.ClassFactory {

        @Override
        public boolean match(String name) {
            return name.startsWith(QueryEngine.class.getPackage().getName());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return QueryEngine.class.getClassLoader().loadClass(name);
        }
    }

    private class SessionCacheLoader extends CacheLoader<DbSession, QuerySession> {
        @Override
        public QuerySession load(@Nonnull DbSession dbSession) {
            LOG.warn("Create PulsarSession for h2 h2session {} via SessionCacheLoader (not expected ...)", dbSession);
            return createQuerySession(dbSession);
        }
    }

    private class SessionRemovalListener implements RemovalListener<DbSession, QuerySession> {
        @Override
        public void onRemoval(@Nonnull RemovalNotification<DbSession, QuerySession> notification) {
            RemovalCause cause = notification.getCause();
            DbSession dbSession = notification.getKey();
            switch (cause) {
                case EXPIRED:
                case SIZE: {
                    // It's safe to close h2 h2session, @see {org.h2.api.ErrorCode#DATABASE_CALLED_AT_SHUTDOWN}
                    // h2session.close();
                    notification.getValue().close();
                    LOG.info("Session {} is closed for reason '{}', remaining {} sessions",
                            dbSession, cause, sessions.size());
                }
                break;
                default:
                    break;
            }
        }
    }
}
