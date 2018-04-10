/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import org.h2.command.CommandInterface;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.expression.Parameter;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.result.ResultTarget;
import org.h2.table.ColumnResolver;
import org.h2.table.Table;
import org.h2.table.TableFilter;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Represents a union SELECT statement.
 */
public class SelectPipeline extends Query {

    private boolean isPrepared;
    private boolean checkInit;

    private ArrayList<Expression> expressions;
    private Expression[] expressionArray;

    private ArrayList<Query> commands = new ArrayList<>();

    public SelectPipeline(Session session) {
        super(session);
    }

    @Override
    public boolean isUnion() {
        return false;
    }

    @Override
    public void prepareJoinBatch() {
//        left.prepareJoinBatch();
//        right.prepareJoinBatch();
    }

    public void addSelect(Query select) {
        commands.add(select);
    }

    @Override
    public void setSQL(String sql) {
        this.sqlStatement = sql;
    }

    @Override
    public void setOrder(ArrayList<SelectOrderBy> order) {

    }

    @Override
    public boolean hasOrder() {
        return false;
    }

    private Query getLastQuery() {
        if (commands.isEmpty()) {
            DbException.throwInternalError("no command or not initialized");
        }
        return commands.get(commands.size() - 1);
    }

    @Override
    public ResultInterface queryMeta() {
        return getLastQuery().queryMeta();
    }

    @Override
    protected ResultInterface queryWithoutCache(int maxRows, ResultTarget target) {
        ResultInterface result = commands.get(0).queryWithoutCache(maxRows, target);
        for (int i = 1; i < commands.size(); ++i) {
            result = commands.get(i).queryWithoutCache(maxRows, target);
        }
        return result;
    }

    @Override
    public void init() {
        if (SysProperties.CHECK && checkInit) {
            DbException.throwInternalError();
        }
        checkInit = true;

        for (Query command : commands) {
            command.init();
        }
    }

    @Override
    public void prepare() {
        if (isPrepared) {
            // sometimes a subquery is prepared twice (CREATE TABLE AS SELECT)
            return;
        }
        if (SysProperties.CHECK && !checkInit) {
            DbException.throwInternalError("not initialized");
        }
        isPrepared = true;

        for (Query command : commands) {
            command.prepare();
        }

        // TODO: How to determine the expressions? Can we?
    }

    @Override
    public double getCost() {
        double cost = 0;
        for (Query command : commands) {
            cost += command.getCost();
        }
        return cost;
    }

    @Override
    public HashSet<Table> getTables() {
        // TODO: Can we do it right now?
        HashSet<Table> set = new HashSet<>();
        for (Query command : commands) {
            set.addAll(command.getTables());
        }
        return set;
    }

    @Override
    public ArrayList<Expression> getExpressions() {
        // Nothing to do
        return null;
    }

    @Override
    public void setForUpdate(boolean forUpdate) {
        // TODO: We can support update
    }

    @Override
    public int getColumnCount() {
        return getLastQuery().getColumnCount();
    }

    @Override
    public void mapColumns(ColumnResolver resolver, int level) {
        for (Query command : commands) {
            command.mapColumns(resolver, level);
        }
    }

    @Override
    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        for (Query command : commands) {
            command.setEvaluatable(tableFilter, b);
        }
    }

    @Override
    public void addGlobalCondition(Parameter param, int columnId, int comparisonType) {
        // Nothing to do
//        for (Query command : commands) {
//            command.addGlobalCondition(param, columnId, comparisonType);
//        }
    }

    @Override
    public String getPlanSQL() {
        StringBuilder buff = new StringBuilder();
        for (Query command : commands) {
            buff.append(command.getPlanSQL());
            buff.append("\n=>\n");
        }
        return buff.toString();
    }

    @Override
    public boolean isEverything(ExpressionVisitor visitor) {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void updateAggregate(Session s) {
        for (Query command : commands) {
            command.updateAggregate(s);
        }
    }

    @Override
    public void fireBeforeSelectTriggers() {
        for (Query command : commands) {
            command.fireBeforeSelectTriggers();
        }
    }

    @Override
    public int getType() {
        return CommandInterface.SELECT;
    }

    @Override
    public boolean allowGlobalConditions() {
        return false;
    }
}
