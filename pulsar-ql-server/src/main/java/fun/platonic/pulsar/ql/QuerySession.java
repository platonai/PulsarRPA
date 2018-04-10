package fun.platonic.pulsar.ql;

import fun.platonic.pulsar.PulsarSession;
import fun.platonic.pulsar.ql.annotation.UDFGroup;
import fun.platonic.pulsar.ql.annotation.UDFunction;
import fun.platonic.pulsar.ql.h2.udfs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by vincent on 18-1-17.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Query session is a bridge between H2 session and pulsar session
 *
 * TODO: remove dependency on h2 session
 */
public class QuerySession extends PulsarSession {
    public static final Logger LOG = LoggerFactory.getLogger(QuerySession.class);

    private QueryEngine engine;
    private final DbSession dbSession;
    private int totalUdfs;
    private int sqlSequence;

    /**
     * Construct a {@link QuerySession}
     */
    public QuerySession(QueryEngine engine, DbSession dbSession, ConfigurableApplicationContext applicationContext) {
        super(applicationContext);
        this.engine = engine;
        this.dbSession = dbSession;
        registerUdfsInPackage("fun.platonic.pulsar.ql.function");
    }

    public int getSqlSequence() {
        return sqlSequence;
    }

    public void setSqlSequence(int sqlSequence) {
        this.sqlSequence = sqlSequence;
    }

    @Override
    public void setTTL(String name, int ttl) {
        if (ttl > 0) {
            ttl = 1 + ttl + dbSession.getSqlSequence();
        }

        super.setTTL(name, ttl);
    }

    @Override
    public boolean isExpired(String propertyName) {
        int sequence = dbSession.getSqlSequence();
        int ttl = getTTL(propertyName);
        boolean expired = sequence > ttl;
        // LOG.debug("Property {}, sequence: {}, ttl: {}", propertyName, sequence, ttl);
        if (LOG.isDebugEnabled() && expired) {
            LOG.debug("Property {} is expired at the {}th command", propertyName, sequence);
        }

        return expired;
    }

    /**
     * Register user defined functions into database
     * TODO: use @autoscan like Spring framework
     * TODO: Hot register UDFs
     * TODO: Database independent udfs, udf definitions should be supported by multiple underlying databases
     * */
    public void registerUdfsInPackage(String pack) {
        try {
            registerUdfs(CommonFunctions.class);
            registerUdfs(CommonFunctionTables.class);
            registerUdfs(PulsarFunctions.class);
            registerUdfs(DomFunctions.class);
            registerUdfs(NewsFunctions.class);
            registerUdfs(NewsFunctionTables.class);
            registerUdfs(MetadataFunctions.class);
            registerUdfs(MetadataFunctionTables.class);
            registerUdfs(AdminFunctions.class);
        } catch (SQLException e) {
            LOG.error("Failed to register UDFs" + e);
        }

        if (totalUdfs > 0) {
            LOG.info("Added total {} new UDFs", totalUdfs);
        }
    }

    public void registerUdfs(Class<?> udfClass) throws SQLException {
        UDFGroup group = udfClass.getDeclaredAnnotation(UDFGroup.class);
        String namespace = group.namespace();
        Set<String> udfs = new HashSet<>();

        for (Method method : udfClass.getMethods()) {
            Annotation annotation = method.getAnnotation(UDFunction.class);
            if (annotation != null) {
                udfs.add(method.getName());
            }
        }
        udfs.forEach(method -> registerUdf(namespace, udfClass, method));
    }

    private void registerUdf(String namespace, Class<?> udfClass, String method) {
        String alias = namespace.isEmpty() ? method : namespace + "_" + method;

        // We support arbitrary "_" in a UDF name, for example, the following functions are the same:
        // some_fun_(), _____some_fun_(), some______fun()
        alias = alias.replaceAll("_", "").toUpperCase();

        String sql = "DROP ALIAS IF EXISTS " + alias;
        // Notice : can not use session.prepare(sql) here, which causes a call cycle
        dbSession.executeUpdate(sql);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Execute udfa registration: " + sql);
        }

        // Notice : can not use session.prepare(sql) here, which causes a call cycle
        sql = "CREATE ALIAS IF NOT EXISTS " + alias + " FOR \"" + udfClass.getName() + "." + method + "\"";
        dbSession.executeUpdate(sql);
        ++totalUdfs;

        if (LOG.isTraceEnabled()) {
            LOG.trace("Execute udf registration: " + sql);
        }
    }
}
