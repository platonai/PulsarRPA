package fun.platonic.pulsar.ql;

import org.h2.command.CommandInterface;

/**
 * DbSession is a wrapper for underlying database session, and is the bridge between database session and pulsar query
 * session
 * */
public class DbSession {
    private int sqlSequence;
    private int id;
    private String name;
    private Object implementation;

    public DbSession(Object implementation) {
        this.implementation = implementation;
        if (org.h2.engine.Session.class.isAssignableFrom(implementation.getClass())) {
            org.h2.engine.Session session = (org.h2.engine.Session)implementation;
            id = session.getId();
            name = session.toString();
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSqlSequence() {
        if (org.h2.engine.Session.class.isAssignableFrom(implementation.getClass())) {
            org.h2.engine.Session session = (org.h2.engine.Session)implementation;
            sqlSequence = session.getCommandSequence();
        }

        return sqlSequence;
    }

    public void setSqlSequence(int sqlSequence) {
        this.sqlSequence = sqlSequence;
    }

    public int executeUpdate(String sql) {
        if (org.h2.engine.Session.class.isAssignableFrom(implementation.getClass())) {
            org.h2.engine.Session session = (org.h2.engine.Session)implementation;
            CommandInterface command = session.prepareCommand(sql);
            return command.executeUpdate();
        }

        return 0;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DbSession && ((DbSession) obj).id == id;
    }
}
