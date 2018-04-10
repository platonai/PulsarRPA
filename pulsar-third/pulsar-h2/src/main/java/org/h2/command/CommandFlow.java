/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command;

import org.h2.engine.Constants;
import org.h2.engine.Session;
import org.h2.expression.ParameterInterface;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.schema.Schema;
import org.h2.table.ForwardResultSetTable;
import org.h2.table.Table;

import java.util.ArrayList;

/**
 * Represents a list of SQL statements.
 */
public class CommandFlow extends Command {

    private final Parser parser;
    // Every single command is a sub sql end with ";" or "=>"
    // For ";", it's a normal single query
    // For "=>", it's a streaming sql
    // The following is a simple streaming sql:
    //      SELECT a, b, c FROM t => FROM WHICH SELECT $._1, $._2, $_1 + $._2
    private final ArrayList<Command> commandList = new ArrayList<>();
    private SingleCommand currentCommand;

    public CommandFlow(Session session, String sql) {
        super(session, sql);
        parser = new Parser(session, sql);
    }

    public void prepare() {
        prepareNextCommand();
    }

    @Override
    public ResultInterface query(int maxrows) {
        ResultInterface lastResult = currentCommand.query(maxrows);
        if (currentCommand.hasDownstream()) {
            // Down stream operates on the result set of the last select
            Schema mainSchema = session.getDatabase().getSchema(Constants.SCHEMA_MAIN);
            Table table = new ForwardResultSetTable(mainSchema, session, "WHICH", lastResult);
            session.addLocalTempTable(table, true);
        }
        else {
            Table table = session.findLocalTempTable("WHICH");
            if (table != null) {
                session.removeLocalTempTable(table);
            }
        }

        if (!parser.isEnd()) {
            executeRemaining();
        }

        // Always returns the first command's result
        Command command = commandList.get(0);
        while (command.getDownstream() != null) {
            command = command.getDownstream();
        }
        result = command.getResult();
        return result;
    }

    @Override
    public int update() {
        affectedRows = currentCommand.executeUpdate();
        if (!parser.isEnd()) {
            executeRemaining();
        }
        return affectedRows;
    }

    private void prepareNextCommand() {
        // trace.debug("==Parser.prepareCommand==SQL:>>>" + parser.getUnparsedSql() + "<<<");
        // out.println("==CommandFlow::prepareNextCommand==");
        try {
            // 1. parse
            Prepared prepared = parser.parse();

            // 2. before prepare
            // load external resource, etc
            prepared.beforePrepare();

            // 3. prepare
            prepared.prepare();

            // 4. add to command flow
            addCommand(new SingleCommand(session, prepared, parser.isDownstream()));

            // 5. execute outside
        } catch (DbException e) {
            throw e.addSQL(getSql());
        }
    }

    private void executeRemaining() {
        if (parser.isNextStatement() || parser.isDownstream()) {
            // execute recursively
            prepareNextCommand();

            if (isQuery()) {
                // TODO: why maxrows is set to be 0?
                query(0);
            } else {
                update();
            }
        }
    }

    private void addCommand(SingleCommand command) {
        if (currentCommand != null && currentCommand.hasDownstream()) {
            // A stream query returns the last ResultSet, and close all previous temporary ResultSet
            currentCommand.setDownstream(command);
            command.setUpstream(currentCommand);
        }

        currentCommand = command;
        commandList.add(command);
    }

    public boolean hasDownstream() {
        return parser.isDownstream();
    }

    public Command getCurrentCommand() {
        return currentCommand;
    }

    @Override
    public void prepareJoinBatch() {
        currentCommand.prepareJoinBatch();
    }

    @Override
    public ArrayList<? extends ParameterInterface> getParameters() {
        return currentCommand.getParameters();
    }

    @Override
    public boolean isQuery() {
        // TODO: should throw?
        return currentCommand.isQuery();
    }

    @Override
    public boolean isTransactional() {
        return true;
    }

    /**
     * TODO: check whether it's really not read only
     * TODO: true if No any more commands and all commands are read only
     * */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ResultInterface queryMeta() {
        return currentCommand.queryMeta();
    }

    @Override
    public int getCommandType() {
        return currentCommand.getCommandType();
    }

    /**
     * Allow just the simplest case: only one cacheable command without stream
     *
     * TODO: check if a single stream is cacheable
     * */
    @Override
    public boolean isCacheable() {
        return commandList.size() == 1
          && currentCommand.isCacheable()
          && parser.isEnd();
    }

    /**
     * Allow just the simplest case: only one cacheable command without stream
     *
     * TODO: check if a single stream is reusable
     * */
    @Override
    public boolean canReuse() {
        return commandList.size() == 1
          && currentCommand.canReuse()
          && parser.isEnd();
    }
}
