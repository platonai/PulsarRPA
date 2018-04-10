/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command;

import org.h2.api.DatabaseEventListener;
import org.h2.command.dml.Explain;
import org.h2.command.dml.Query;
import org.h2.engine.Session;
import org.h2.expression.Parameter;
import org.h2.expression.ParameterInterface;
import org.h2.result.ResultInterface;
import org.h2.table.TableView;
import org.h2.value.Value;
import org.h2.value.ValueNull;

import java.util.ArrayList;

/**
 * Represents a single SQL statements.
 * It wraps a prepared statement.
 */
public class SingleCommand extends Command {

    private Prepared prepared;
    private boolean readOnlyKnown;
    private boolean readOnly;
    private boolean hasDownstream;

    SingleCommand(Session session, Prepared prepared, boolean hasDownstream) {
        super(session, prepared.sqlStatement);
        prepared.setCommand(this);
        this.prepared = prepared;
        this.hasDownstream = hasDownstream;
    }

    @Override
    public ResultInterface query(int maxrows) {
        // System.out.println("==SingleCommand.query==");

        recompileIfRequired();
        setProgress(DatabaseEventListener.STATE_STATEMENT_START);

        // log start time if required
        start();

        prepared.checkParameters();

//        System.out.println("\nquery>>>\n" + prepared.getPlanSQL() + "\n" + prepared.getSQL());
        // prepared.setUpstream(getUpstream());
        result = prepared.query(maxrows);

        prepared.trace(startTimeNanos, result.isLazy() ? 0 : result.getRowCount());
        setProgress(DatabaseEventListener.STATE_STATEMENT_END);

        return result;
    }

    @Override
    public int update() {
//        System.out.println("SingleCommand#update");

        recompileIfRequired();
        setProgress(DatabaseEventListener.STATE_STATEMENT_START);

        // log start time if required
        start();

        session.setLastScopeIdentity(ValueNull.INSTANCE);
        prepared.checkParameters();

//        System.out.println("\nupdate>>>\n" + prepared.getPlanSQL() + "\n" + prepared.getSQL());
        affectedRows = prepared.update();

        prepared.trace(startTimeNanos, affectedRows);
        setProgress(DatabaseEventListener.STATE_STATEMENT_END);
        return affectedRows;
    }

    @Override
    public ArrayList<? extends ParameterInterface> getParameters() {
        return prepared.getParameters();
    }

    @Override
    public boolean isTransactional() {
        return prepared.isTransactional();
    }

    @Override
    public boolean isQuery() {
        return prepared.isQuery();
    }

    @Override
    public boolean hasDownstream() {
        return hasDownstream;
    }

    @Override
    public void prepareJoinBatch() {
        if (session.isJoinBatchEnabled()) {
            prepareJoinBatch(prepared);
        }
    }

    private static void prepareJoinBatch(Prepared prepared) {
        if (prepared.isQuery()) {
            int type = prepared.getType();

            if (type == CommandInterface.SELECT) {
                ((Query) prepared).prepareJoinBatch();
            } else if (type == CommandInterface.EXPLAIN || type == CommandInterface.EXPLAIN_ANALYZE) {
                prepareJoinBatch(((Explain) prepared).getCommand());
            }
        }
    }

    private void recompileIfRequired() {
        if (prepared.needRecompile()) {
            // System.out.println("==SingleCommand#needRecompile==" + getSql());

            // TODO test with 'always recompile'
            prepared.setModificationMetaId(0);
            String sql = prepared.getSQL();
            ArrayList<Parameter> oldParams = prepared.getParameters();

            // System.out.println("recompileIfRequired - before parse");

            Parser parser = new Parser(session, sql);
            prepared = parser.parse();

            // System.out.println("recompileIfRequired - after parse");

            long mod = prepared.getModificationMetaId();
            prepared.setModificationMetaId(0);
            ArrayList<Parameter> newParams = prepared.getParameters();
            for (int i = 0; i < newParams.size(); i++) {
                Parameter old = oldParams.get(i);
                if (old.isValueSet()) {
                    Value v = old.getValue(session);
                    Parameter p = newParams.get(i);
                    p.setValue(v);
                }
            }

            // System.out.println("recompileIfRequired - before prepare");

            prepared.prepare();

            // System.out.println("recompileIfRequired - after prepare");

            prepared.setModificationMetaId(mod);
            prepareJoinBatch();
        }
    }

    @Override
    public void stop() {
        super.stop();
        // Clean up after the command was run in the session.
        // Must restart query (and dependency construction) to reuse.
        if (prepared.getCteCleanups() != null) {
            for (TableView view : prepared.getCteCleanups()) {
                // check if view was previously deleted as their name is set to
                // null
                if (view.getName() != null) {
                    session.removeLocalTempTable(view);
                }
            }
        }
    }

    @Override
    public boolean canReuse() {
        return super.canReuse() && prepared.getCteCleanups() == null;
    }

    @Override
    public boolean isReadOnly() {
        if (!readOnlyKnown) {
            readOnly = prepared.isReadOnly();
            readOnlyKnown = true;
        }
        return readOnly;
    }

    @Override
    public ResultInterface queryMeta() {
        return prepared.queryMeta();
    }

    @Override
    public boolean isCacheable() {
        return prepared.isCacheable();
    }

    @Override
    public int getCommandType() {
        return prepared.getType();
    }
}
