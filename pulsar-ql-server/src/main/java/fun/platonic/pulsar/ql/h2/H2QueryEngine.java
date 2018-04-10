package fun.platonic.pulsar.ql.h2;

import fun.platonic.pulsar.ql.DbSession;
import fun.platonic.pulsar.ql.H2Config;
import fun.platonic.pulsar.ql.QueryEngine;
import fun.platonic.pulsar.ql.QuerySession;
import org.h2.engine.ConnectionInfo;
import org.h2.engine.Mode;
import org.h2.engine.SessionInterface;
import org.h2.util.JdbcUtils;

import java.sql.SQLException;

public class H2QueryEngine implements org.h2.engine.SessionFactory {

    private static H2QueryEngine INSTANCE;

    private org.h2.engine.Engine h2Engine;
    private QueryEngine queryEngine;

    public static void config() {
        JdbcUtils.addClassFactory(new QueryEngine.ClassFactory());
    }

    private H2QueryEngine() {
        H2Config.config();
        H2QueryEngine.config();

        h2Engine = org.h2.engine.Engine.getInstance();
        queryEngine = QueryEngine.getInstance();
    }

    /**
     * Required by h2 database runtime
     * */
    public static H2QueryEngine getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new H2QueryEngine();
        }

        return INSTANCE;
    }

    @Override
    public SessionInterface createSession(ConnectionInfo ci) throws SQLException {
        org.h2.engine.Session h2session = h2Engine.createSession(ci);
        h2session.getDatabase().setMode(Mode.getInstance(Mode.SIGMA));
        queryEngine.createQuerySession(new DbSession(h2session));
        return h2session;
    }

    @Override
    public void closeSession(int sessionId) throws SQLException {
    }
}
