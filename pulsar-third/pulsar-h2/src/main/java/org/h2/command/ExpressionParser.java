package org.h2.command;

import org.h2.api.ErrorCode;
import org.h2.command.dml.Query;
import org.h2.command.dml.Select;
import org.h2.command.dml.SelectOrderBy;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.schema.Schema;
import org.h2.schema.Sequence;
import org.h2.table.Column;
import org.h2.table.TableFilter;
import org.h2.util.MathUtils;
import org.h2.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import static org.h2.api.ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1;

/**
 * Created by vincent on 17-10-24.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
abstract class ExpressionParser extends LexicalAnalyzer {
    String schemaName;

    // TODO: remove dependency on database and session
    final Database database;
    final Session session;
    final Trace trace;

    Prepared currentPrepared;
    boolean currentIsExternal;
    int currentSelectIndex;
    boolean recompileAlways;

    Select currentSelect;
    ArrayList<Select> indexedSelectList;

    HashSet<String> expressionColumnNames;

    ArrayList<Parameter> parameters;
    ArrayList<Parameter> indexedParameterList;

    ExpressionParser(Session session, String sql) {
        super(sql, session.getDatabase().getMode(), session.getDatabase().getSettings());
        this.session = session;
        this.database = session.getDatabase();
        this.trace = session.getTrace();
    }

    abstract Query parseSelect();

    abstract Schema getSchema();

    Column parseColumnWithType(String columnName) {
        String original = currentToken;
        boolean regular = false;
        if (readIf("LONG")) {
            if (readIf("RAW")) {
                original += " RAW";
            }
        } else if (readIf("DOUBLE")) {
            if (readIf("PRECISION")) {
                original += " PRECISION";
            }
        } else if (readIf("CHARACTER")) {
            if (readIf("VARYING")) {
                original += " VARYING";
            }
        } else if (readIf("TIMESTAMP")) {
            if (readIf("WITH")) {
                // originally we used TIMEZONE, which turns out not to be
                // standards-compliant, but lets keep backwards compatibility
                if (readIf("TIMEZONE")) {
                    read("TIMEZONE");
                    original += " WITH TIMEZONE";
                } else {
                    read("TIME");
                    read("ZONE");
                    original += " WITH TIME ZONE";
                }
            }
        } else {
            regular = true;
        }
        long precision = -1;
        int displaySize = -1;
        String[] enumerators = null;
        int scale = -1;
        String comment = null;
        Column templateColumn = null;
        DataType dataType;
        if (!identifiersToUpper) {
            original = StringUtils.toUpperEnglish(original);
        }
        UserDataType userDataType = database.findUserDataType(original);
        if (userDataType != null) {
            templateColumn = userDataType.getColumn();
            dataType = DataType.getDataType(templateColumn.getType());
            comment = templateColumn.getComment();
            original = templateColumn.getOriginalSQL();
            precision = templateColumn.getPrecision();
            displaySize = templateColumn.getDisplaySize();
            scale = templateColumn.getScale();
            enumerators = templateColumn.getEnumerators();
        } else {
            dataType = DataType.getTypeByName(original);
            if (dataType == null || mode.disallowedTypes.contains(original)) {
                throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, currentToken);
            }
        }
        if (database.getIgnoreCase() && dataType.type == Value.STRING &&
                !equalsToken("VARCHAR_CASESENSITIVE", original)) {
            original = "VARCHAR_IGNORECASE";
            dataType = DataType.getTypeByName(original);
        }
        if (regular) {
            readToken();
        }
        precision = precision == -1 ? dataType.defaultPrecision : precision;
        displaySize = displaySize == -1 ? dataType.defaultDisplaySize
                : displaySize;
        scale = scale == -1 ? dataType.defaultScale : scale;
        if (dataType.supportsPrecision || dataType.supportsScale) {
            if (readIf("(")) {
                if (!readIf("MAX")) {
                    long p = readLong();
                    if (readIf("K")) {
                        p *= 1024;
                    } else if (readIf("M")) {
                        p *= 1024 * 1024;
                    } else if (readIf("G")) {
                        p *= 1024 * 1024 * 1024;
                    }
                    if (p > Long.MAX_VALUE) {
                        p = Long.MAX_VALUE;
                    }
                    original += "(" + p;
                    // Oracle syntax
                    readIf("CHAR");
                    if (dataType.supportsScale) {
                        if (readIf(",")) {
                            scale = readInt();
                            original += ", " + scale;
                        } else {
                            // special case: TIMESTAMP(5) actually means
                            // TIMESTAMP(23, 5)
                            if (dataType.type == Value.TIMESTAMP) {
                                scale = MathUtils.convertLongToInt(p);
                                p = precision;
                            } else {
                                scale = 0;
                            }
                        }
                    }
                    precision = p;
                    displaySize = MathUtils.convertLongToInt(precision);
                    original += ")";
                }
                read(")");
            }
        } else if (dataType.type == Value.ENUM) {
            if (readIf("(")) {
                java.util.List<String> enumeratorList = new ArrayList<String>();
                original += '(';
                String enumerator0 = readString();
                enumeratorList.add(enumerator0);
                original += "'" + enumerator0 + "'";
                while (readIf(",")) {
                    original += ',';
                    String enumeratorN = readString();
                    original += "'" + enumeratorN + "'";
                    enumeratorList.add(enumeratorN);
                }
                read(")");
                original += ')';
                enumerators = enumeratorList.toArray(new String[enumeratorList.size()]);
            }
            try {
                ValueEnum.check(enumerators);
            } catch (DbException e) {
                throw e.addSQL(original);
            }
        } else if (readIf("(")) {
            // Support for MySQL: INT(11), MEDIUMINT(8) and so on.
            // Just ignore the precision.
            readPositiveInt();
            read(")");
        }
        if (readIf("FOR")) {
            read("BIT");
            read("DATA");
            if (dataType.type == Value.STRING) {
                dataType = DataType.getTypeByName("BINARY");
            }
        }
        // MySQL compatibility
        readIf("UNSIGNED");
        int type = dataType.type;
        if (scale > precision) {
            throw DbException.get(ErrorCode.INVALID_VALUE_SCALE_PRECISION,
                    Integer.toString(scale), Long.toString(precision));
        }

        Column column = new Column(columnName, type, precision, scale, displaySize, enumerators);
        if (templateColumn != null) {
            column.setNullable(templateColumn.isNullable());
            column.setDefaultExpression(session, templateColumn.getDefaultExpression());
            int selectivity = templateColumn.getSelectivity();
            if (selectivity != Constants.SELECTIVITY_DEFAULT) {
                column.setSelectivity(selectivity);
            }
            Expression checkConstraint = templateColumn.getCheckConstraint(session, columnName);
            column.addCheckConstraint(session, checkConstraint);
        }
        column.setComment(comment);
        column.setOriginalSQL(original);
        return column;
    }

    private Sequence findSequence(String schema, String sequenceName) {
        Sequence sequence = database.getSchema(schema).findSequence(sequenceName);
        if (sequence != null) {
            return sequence;
        }
        String[] schemaNames = session.getSchemaSearchPath();
        if (schemaNames != null) {
            for (String n : schemaNames) {
                sequence = database.getSchema(n).findSequence(sequenceName);
                if (sequence != null) {
                    return sequence;
                }
            }
        }
        return null;
    }

    Expression readExpression() {
        Expression r = readAnd();
        while (readIf("OR")) {
            r = new ConditionAndOr(ConditionAndOr.OR, r, readAnd());
        }
        return r;
    }

    private Expression readAnd() {
        Expression r = readCondition();
        while (readIf("AND")) {
            r = new ConditionAndOr(ConditionAndOr.AND, r, readCondition());
        }
        return r;
    }

    private Expression readCondition() {
        if (readIf("NOT")) {
            return new ConditionNot(readCondition());
        }

        if (readIf("EXISTS")) {
            read("(");
            Query query = parseSelect();
            // can not reduce expression because it might be a union except
            // query with distinct
            read(")");
            return new ConditionExists(query);
        }

        if (readIf("INTERSECTS")) {
            read("(");
            Expression r1 = readConcat();
            read(",");
            Expression r2 = readConcat();
            read(")");
            return new Comparison(session, Comparison.SPATIAL_INTERSECTS, r1, r2);
        }

        Expression r = readConcat();
        if (currentIsExternal) {
            String columnName = r.getColumnName();
            if (r instanceof ExpressionColumn) {
                expressionColumnNames.add(columnName);
                ((ExpressionColumn) r).setSquery(columnName);
            }

            trace.debug("readCondition#concat: " + r.getClass().getName()
                    + ", columnName: " + columnName + ", sql: " + r.getSQL());
        }

        while (true) {
            // special case: NOT NULL is not part of an expression (as in CREATE
            // TABLE TEST(ID INT DEFAULT 0 NOT NULL))
            int backup = parseIndex;
            boolean not = false;
            if (readIf("NOT")) {
                not = true;
                if (isToken("NULL")) {
                    // this really only works for NOT NULL!
                    parseIndex = backup;
                    currentToken = "NOT";
                    break;
                }
            }

            if (readIf("LIKE")) {
                Expression b = readConcat();
                Expression esc = null;
                if (readIf("ESCAPE")) {
                    esc = readConcat();
                }
                recompileAlways = true;
                r = new CompareLike(database, r, b, esc, false);
            } else if (readIf("ILIKE")) {
                Function function = Function.getFunction(database, "CAST");
                function.setDataType(new Column("X", Value.STRING_IGNORECASE));
                function.setParameter(0, r);
                r = function;
                Expression b = readConcat();
                Expression esc = null;
                if (readIf("ESCAPE")) {
                    esc = readConcat();
                }
                recompileAlways = true;
                r = new CompareLike(database, r, b, esc, false);
            } else if (readIf("REGEXP")) {
                Expression b = readConcat();
                recompileAlways = true;
                r = new CompareLike(database, r, b, null, true);
            } else if (readIf("IS")) {
                if (readIf("NOT")) {
                    if (readIf("NULL")) {
                        r = new Comparison(session, Comparison.IS_NOT_NULL, r, null);
                    } else if (readIf("DISTINCT")) {
                        read("FROM");
                        r = new Comparison(session, Comparison.EQUAL_NULL_SAFE, r, readConcat());
                    } else {
                        r = new Comparison(session, Comparison.NOT_EQUAL_NULL_SAFE, r, readConcat());
                    }
                } else if (readIf("NULL")) {
                    r = new Comparison(session, Comparison.IS_NULL, r, null);
                } else if (readIf("DISTINCT")) {
                    read("FROM");
                    r = new Comparison(session, Comparison.NOT_EQUAL_NULL_SAFE, r, readConcat());
                } else {
                    r = new Comparison(session, Comparison.EQUAL_NULL_SAFE, r, readConcat());
                }
            } else if (readIf("IN")) {
                read("(");
                if (readIf(")")) {
                    if (mode.prohibitEmptyInPredicate) {
                        throw getSyntaxError();
                    }
                    r = ValueExpression.get(ValueBoolean.get(false));
                } else {
                    if (isSelect()) {
                        Query query = parseSelect();
                        // can not be lazy because we have to call
                        // method ResultInterface.containsDistinct
                        // which is not supported for lazy execution
                        query.setNeverLazy(true);
                        r = new ConditionInSelect(database, r, query, false, Comparison.EQUAL);
                    } else {
                        ArrayList<Expression> v = new ArrayList<>();
                        Expression last;
                        do {
                            last = readExpression();
                            v.add(last);
                        } while (readIf(","));
                        if (v.size() == 1 && (last instanceof Subquery)) {
                            Subquery s = (Subquery) last;
                            Query q = s.getQuery();
                            r = new ConditionInSelect(database, r, q, false, Comparison.EQUAL);
                        } else {
                            r = new ConditionIn(database, r, v);
                        }
                    }
                    read(")");
                }
            } else if (readIf("BETWEEN")) {
                Expression low = readConcat();
                read("AND");
                Expression high = readConcat();
                Expression condLow = new Comparison(session, Comparison.SMALLER_EQUAL, low, r);
                Expression condHigh = new Comparison(session, Comparison.BIGGER_EQUAL, high, r);
                r = new ConditionAndOr(ConditionAndOr.AND, condLow, condHigh);
            } else {
                int compareType = getCompareType(currentTokenType);
                if (compareType < 0) {
                    // return: normal column expression
                    break;
                }
                readToken();

                if (readIf("ALL")) {
                    read("(");
                    Query query = parseSelect();
                    r = new ConditionInSelect(database, r, query, true, compareType);
                    read(")");
                } else if (readIf("ANY") || readIf("SOME")) {
                    read("(");
                    Query query = parseSelect();
                    r = new ConditionInSelect(database, r, query, false, compareType);
                    read(")");
                } else {
                    Expression right = readConcat();
                    if (SysProperties.OLD_STYLE_OUTER_JOIN &&
                            readIf("(") && readIf("+") && readIf(")")) {
                        // support for a subset of old-fashioned Oracle outer
                        // join with (+)

                        if (r instanceof ExpressionColumn && right instanceof ExpressionColumn) {
                            ExpressionColumn leftCol = (ExpressionColumn) r;
                            ExpressionColumn rightCol = (ExpressionColumn) right;
                            ArrayList<TableFilter> filters = currentSelect.getTopFilters();
                            for (TableFilter f : filters) {
                                while (f != null) {
                                    leftCol.mapColumns(f, 0);
                                    rightCol.mapColumns(f, 0);
                                    f = f.getJoin();
                                }
                            }

                            TableFilter leftFilter = leftCol.getTableFilter();
                            TableFilter rightFilter = rightCol.getTableFilter();
                            r = new Comparison(session, compareType, r, right);
                            if (leftFilter != null && rightFilter != null) {
                                int idx = filters.indexOf(rightFilter);
                                if (idx >= 0) {
                                    filters.remove(idx);
                                    leftFilter.addJoin(rightFilter, true, false, r);
                                } else {
                                    rightFilter.mapAndAddFilter(r);
                                }
                                r = ValueExpression.get(ValueBoolean.get(true));
                            }
                        }
                    } else {
                        r = new Comparison(session, compareType, r, right);
                    }
                }
            }

            if (not) {
                r = new ConditionNot(r);
            }
        }
        return r;
    }

    boolean isSelect() {
        int start = lastParseIndex;
        while (readIf("(")) {
            // need to read ahead, it could be a nested union:
            // ((select 1) union (select 1))
        }
        boolean select = isToken("SELECT") || isToken("FROM") || isToken("WITH");
        parseIndex = start;
        readToken();
        return select;
    }

    ArrayList<SelectOrderBy> parseSimpleOrderList() {
        ArrayList<SelectOrderBy> orderList = new ArrayList<>();
        do {
            SelectOrderBy order = new SelectOrderBy();
            order.expression = readExpression();
            if (readIf("DESC")) {
                order.descending = true;
            } else {
                readIf("ASC");
            }
            orderList.add(order);
        } while (readIf(","));
        return orderList;
    }

    private Expression readConcat() {
        Expression r = readSum();
        while (true) {
            if (readIf("||")) {
                r = new Operation(Operation.CONCAT, r, readSum());
            } else if (readIf("~")) {
                if (readIf("*")) {
                    Function function = Function.getFunction(database, "CAST");
                    Objects.requireNonNull(function);
                    function.setDataType(new Column("X", Value.STRING_IGNORECASE));
                    function.setParameter(0, r);
                    r = function;
                }
                r = new CompareLike(database, r, readSum(), null, true);
            } else if (readIf("!~")) {
                if (readIf("*")) {
                    Function function = Function.getFunction(database, "CAST");
                    Objects.requireNonNull(function);
                    function.setDataType(new Column("X", Value.STRING_IGNORECASE));
                    function.setParameter(0, r);
                    r = function;
                }
                r = new ConditionNot(new CompareLike(database, r, readSum(), null, true));
            } else {
                return r;
            }
        }
    }

    private Expression readSum() {
        Expression r = readFactor();
        while (true) {
            if (readIf("+")) {
                r = new Operation(Operation.PLUS, r, readFactor());
            } else if (readIf("-")) {
                r = new Operation(Operation.MINUS, r, readFactor());
            } else {
                return r;
            }
        }
    }

    private Expression readFactor() {
        Expression r = readTerm();
        while (true) {
            if (readIf("*")) {
                r = new Operation(Operation.MULTIPLY, r, readTerm());
            } else if (readIf("/")) {
                r = new Operation(Operation.DIVIDE, r, readTerm());
            } else if (readIf("%")) {
                r = new Operation(Operation.MODULUS, r, readTerm());
            } else {
                return r;
            }
        }
    }

    private Expression readAggregate(int aggregateType, String aggregateName) {
        if (currentSelect == null) {
            throw getSyntaxError();
        }
        currentSelect.setGroupQuery();
        Expression r;
        if (aggregateType == Aggregate.COUNT) {
            if (readIf("*")) {
                r = new Aggregate(Aggregate.COUNT_ALL, null, currentSelect, false);
            } else {
                boolean distinct = readIf("DISTINCT");
                Expression on = readExpression();
                if (on instanceof Wildcard && !distinct) {
                    // PostgreSQL compatibility: count(t.*)
                    r = new Aggregate(Aggregate.COUNT_ALL, null, currentSelect, false);
                } else {
                    r = new Aggregate(Aggregate.COUNT, on, currentSelect, distinct);
                }
            }
        } else if (aggregateType == Aggregate.GROUP_CONCAT) {
            Aggregate agg = null;
            boolean distinct = readIf("DISTINCT");

            if (equalsToken("GROUP_CONCAT", aggregateName)) {
                agg = new Aggregate(Aggregate.GROUP_CONCAT,
                        readExpression(), currentSelect, distinct);
                if (readIf("ORDER")) {
                    read("BY");
                    agg.setGroupConcatOrder(parseSimpleOrderList());
                }

                if (readIf("SEPARATOR")) {
                    agg.setGroupConcatSeparator(readExpression());
                }
            } else if (equalsToken("STRING_AGG", aggregateName)) {
                // PostgreSQL compatibility: string_agg(expression, delimiter)
                agg = new Aggregate(Aggregate.GROUP_CONCAT, readExpression(), currentSelect, distinct);
                read(",");
                agg.setGroupConcatSeparator(readExpression());
                if (readIf("ORDER")) {
                    read("BY");
                    agg.setGroupConcatOrder(parseSimpleOrderList());
                }
            }
            r = agg;
        } else {
            boolean distinct = readIf("DISTINCT");
            r = new Aggregate(aggregateType, readExpression(), currentSelect, distinct);
        }
        read(")");
        return r;
    }

    private JavaFunction readJavaFunction(Schema schema, String functionName) {
        FunctionAlias functionAlias;
        if (schema != null) {
            functionAlias = schema.findFunction(functionName);
        } else {
            functionAlias = findFunctionAlias(session.getCurrentSchemaName(), functionName);
        }
        if (functionAlias == null) {
            throw DbException.get(ErrorCode.FUNCTION_NOT_FOUND_1, functionName);
        }

        ArrayList<Expression> argList = new ArrayList<>();
        int numArgs = 0;
        while (!readIf(")")) {
            if (numArgs++ > 0) {
                read(",");
            }
            argList.add(readExpression());
        }
        return new JavaFunction(functionAlias, argList.toArray(new Expression[numArgs]));
    }

    private JavaAggregate readJavaAggregate(UserAggregate aggregate) {
        ArrayList<Expression> params = new ArrayList<>();
        do {
            params.add(readExpression());
        } while (readIf(","));
        read(")");
        Expression[] list = new Expression[params.size()];
        params.toArray(list);
        JavaAggregate agg = new JavaAggregate(aggregate, list, currentSelect);
        currentSelect.setGroupQuery();
        return agg;
    }

    int getAggregateType(String name) {
        if (!identifiersToUpper) {
            // if not yet converted to uppercase, do it now
            name = StringUtils.toUpperEnglish(name);
        }

        return Aggregate.getAggregateType(name);
    }

    private FunctionAlias findFunctionAlias(String schema, String aliasName) {
        FunctionAlias functionAlias = database.getSchema(schema).findFunction(aliasName);
        if (functionAlias != null) {
            return functionAlias;
        }
        String[] schemaNames = session.getSchemaSearchPath();
        if (schemaNames != null) {
            for (String n : schemaNames) {
                functionAlias = database.getSchema(n).findFunction(aliasName);
                if (functionAlias != null) {
                    return functionAlias;
                }
            }
        }
        return null;
    }

    Expression readFunction(Schema schema, String name) {
        // UDF - User defined functions
        if (schema != null) {
            return readJavaFunction(schema, name);
        }

        // UDA - User defined aggregations
        int agg = getAggregateType(name);
        if (agg >= 0) {
            return readAggregate(agg, name);
        }

        // Built-in functions
        Function function = Function.getFunction(database, name);
        if (function == null) {
            // Built-in aggregations
            UserAggregate aggregate = database.findAggregate(name);
            if (aggregate != null) {
                return readJavaAggregate(aggregate);
            }
            return readJavaFunction(null, name);
        }

        switch (function.getFunctionType()) {
            case Function.CAST: {
                function.setParameter(0, readExpression());
                read("AS");
                Column type = parseColumnWithType(null);
                function.setDataType(type);
                read(")");
                break;
            }
            case Function.CONVERT: {
                if (mode.swapConvertFunctionParameters) {
                    Column type = parseColumnWithType(null);
                    function.setDataType(type);
                    read(",");
                    function.setParameter(0, readExpression());
                    read(")");
                } else {
                    function.setParameter(0, readExpression());
                    read(",");
                    Column type = parseColumnWithType(null);
                    function.setDataType(type);
                    read(")");
                }
                break;
            }
            case Function.EXTRACT: {
                function.setParameter(0, ValueExpression.get(ValueString.get(currentToken)));
                readToken();
                read("FROM");
                function.setParameter(1, readExpression());
                read(")");
                break;
            }
            case Function.DATE_ADD:
            case Function.DATE_DIFF: {
                if (Function.isDatePart(currentToken)) {
                    function.setParameter(0, ValueExpression.get(ValueString.get(currentToken)));
                    readToken();
                } else {
                    function.setParameter(0, readExpression());
                }
                read(",");
                function.setParameter(1, readExpression());
                read(",");
                function.setParameter(2, readExpression());
                read(")");
                break;
            }
            case Function.SUBSTRING: {
                // Different variants include:
                // SUBSTRING(X,1)
                // SUBSTRING(X,1,1)
                // SUBSTRING(X FROM 1 FOR 1) -- Postgres
                // SUBSTRING(X FROM 1) -- Postgres
                // SUBSTRING(X FOR 1) -- Postgres
                function.setParameter(0, readExpression());
                if (readIf("FROM")) {
                    function.setParameter(1, readExpression());
                    if (readIf("FOR")) {
                        function.setParameter(2, readExpression());
                    }
                } else if (readIf("FOR")) {
                    function.setParameter(1, ValueExpression.get(ValueInt.get(0)));
                    function.setParameter(2, readExpression());
                } else {
                    read(",");
                    function.setParameter(1, readExpression());
                    if (readIf(",")) {
                        function.setParameter(2, readExpression());
                    }
                }
                read(")");
                break;
            }
            case Function.POSITION: {
                // can't read expression because IN would be read too early
                function.setParameter(0, readConcat());
                if (!readIf(",")) {
                    read("IN");
                }
                function.setParameter(1, readExpression());
                read(")");
                break;
            }
            case Function.TRIM: {
                Expression space = null;
                if (readIf("LEADING")) {
                    function = Function.getFunction(database, "LTRIM");
                    if (!readIf("FROM")) {
                        space = readExpression();
                        read("FROM");
                    }
                } else if (readIf("TRAILING")) {
                    function = Function.getFunction(database, "RTRIM");
                    if (!readIf("FROM")) {
                        space = readExpression();
                        read("FROM");
                    }
                } else if (readIf("BOTH")) {
                    if (!readIf("FROM")) {
                        space = readExpression();
                        read("FROM");
                    }
                }
                Expression p0 = readExpression();
                if (readIf(",")) {
                    space = readExpression();
                } else if (readIf("FROM")) {
                    space = p0;
                    p0 = readExpression();
                }
                Objects.requireNonNull(function);
                function.setParameter(0, p0);
                if (space != null) {
                    function.setParameter(1, space);
                }
                read(")");
                break;
            }
            case Function.TABLE:
            case Function.TABLE_DISTINCT: {
                // The table function:
                // select a,b from table(a int=1, b char='x');
                // select * from table(id int = (1, 2), name varchar=('Hello', 'World'));
                int i = 0;
                ArrayList<Column> columns = new ArrayList<>();
                do {
                    String columnName = readAliasIdentifier();
                    Column column = parseColumnWithType(columnName);
                    columns.add(column);
                    read("=");
                    function.setParameter(i, readExpression());
                    i++;
                } while (readIf(","));
                read(")");
                TableFunction tf = (TableFunction) function;
                tf.setColumns(columns);
                break;
            }
            case Function.ROW_NUMBER:
                read(")");
                read("OVER");
                read("(");
                read(")");
                if (currentSelect == null && currentPrepared == null) {
                    throw getSyntaxError();
                }
                return new Rownum(currentSelect == null ? currentPrepared : currentSelect);
            case Function.EXPLODE:

                break;
            default:
                if (!readIf(")")) {
                    int i = 0;
                    do {
                        function.setParameter(i++, readExpression());
                    } while (readIf(","));
                    read(")");
                }
        }
        function.doneWithParameters();
        return function;
    }

    private Expression readFunctionWithoutParameters(String name) {
        if (readIf("(")) {
            read(")");
        }
        if (database.isAllowBuiltinAliasOverride()) {
            FunctionAlias functionAlias = database.getSchema(session.getCurrentSchemaName()).findFunction(name);
            if (functionAlias != null) {
                return new JavaFunction(functionAlias, new Expression[0]);
            }
        }
        Function function = Function.getFunction(database, name);
        Objects.requireNonNull(function);
        function.doneWithParameters();
        return function;
    }

    private Expression readWildcardOrSequenceValue(String schema, String objectName) {
        if (readIf("*")) {
            return new Wildcard(schema, objectName);
        }

        if (schema == null) {
            schema = session.getCurrentSchemaName();
        }

        if (readIf("NEXTVAL")) {
            Sequence sequence = findSequence(schema, objectName);
            if (sequence != null) {
                return new SequenceValue(sequence);
            }
        } else if (readIf("CURRVAL")) {
            Sequence sequence = findSequence(schema, objectName);
            if (sequence != null) {
                Function function = Function.getFunction(database, "CURRVAL");
                Objects.requireNonNull(function);
                function.setParameter(0, ValueExpression.get(ValueString.get(sequence.getSchema().getName())));
                function.setParameter(1, ValueExpression.get(ValueString.get(sequence.getName())));
                function.doneWithParameters();
                return function;
            }
        }

        return null;
    }

    private Expression readTermObjectDot(String objectName) {
        Expression expr = readWildcardOrSequenceValue(null, objectName);
        if (expr != null) {
            return expr;
        }

        String name = readColumnIdentifier();
        Schema s = database.findSchema(objectName);
        if ((!SysProperties.OLD_STYLE_OUTER_JOIN || s != null) && readIf("(")) {
            // only if the token before the dot is a valid schema name,
            // otherwise the old style Oracle outer join doesn't work:
            // t.x = t2.x(+)
            // this additional check is not required
            // if the old style outer joins are not supported
            return readFunction(s, name);
        } else if (readIf(".")) {
            String schema = objectName;
            objectName = name;
            expr = readWildcardOrSequenceValue(schema, objectName);
            if (expr != null) {
                return expr;
            }
            name = readColumnIdentifier();
            if (readIf("(")) {
                String databaseName = schema;
                if (!equalsToken(database.getShortName(), databaseName)) {
                    throw DbException.get(ErrorCode.DATABASE_NOT_FOUND_1, databaseName);
                }
                schema = objectName;
                return readFunction(database.getSchema(schema), name);
            } else if (readIf(".")) {
                String databaseName = schema;
                if (!equalsToken(database.getShortName(), databaseName)) {
                    throw DbException.get(ErrorCode.DATABASE_NOT_FOUND_1, databaseName);
                }
                schema = objectName;
                objectName = name;
                expr = readWildcardOrSequenceValue(schema, objectName);
                if (expr != null) {
                    return expr;
                }
                name = readColumnIdentifier();
                return new ExpressionColumn(database, schema, objectName, name);
            }
            return new ExpressionColumn(database, schema, objectName, name);
        }
        return new ExpressionColumn(database, null, objectName, name);
    }

    Expression readTerm() {
        Expression r;
        switch (currentTokenType) {
            case AT:
                // @
                // Declare a variable
                // @variable := value
                readToken();
                r = new Variable(session, readAliasIdentifier());
                if (readIf(":=")) {
                    Expression value = readExpression();
                    Function function = Function.getFunction(database, "SET");
                    Objects.requireNonNull(function);
                    function.setParameter(0, r);
                    function.setParameter(1, value);
                    r = function;
                }
                break;
            case PARAMETER:
                // ?, $
                // A parameter placeholder
                // INSERT INTO table t(id, name) values(?, ?), (?, ?);
                // or
                // INSERT INTO table t(id, name) values(?1, ?2), (?3, ?4);
                // there must be no space between ? and the number
                boolean indexed = Character.isDigit(cleanSqlChars[parseIndex]);

                Parameter p;
                if (indexed) {
                    readParameterIndex();
                    if (indexedParameterList == null) {
                        if (parameters == null) {
                            // this can occur when parsing expressions only (for
                            // example check constraints)
                            throw getSyntaxError();
                        } else if (!parameters.isEmpty()) {
                            throw DbException.get(ErrorCode.CANNOT_MIX_INDEXED_AND_UNINDEXED_PARAMS);
                        }
                        indexedParameterList = new ArrayList<>();
                    }

                    int index = currentValue.getInt() - 1;
                    if (index < 0 || index >= Constants.MAX_PARAMETER_INDEX) {
                        throw DbException.getInvalidValueException("parameter index", index);
                    }

                    if (indexedParameterList.size() <= index) {
                        indexedParameterList.ensureCapacity(index + 1);
                        while (indexedParameterList.size() <= index) {
                            indexedParameterList.add(null);
                        }
                    }

                    p = indexedParameterList.get(index);
                    if (p == null) {
                        p = new Parameter(index);
                        indexedParameterList.set(index, p);
                    }
                    readToken();
                } else {
                    readToken();
                    if (indexedParameterList != null) {
                        throw DbException.get(ErrorCode.CANNOT_MIX_INDEXED_AND_UNINDEXED_PARAMS);
                    }
                    p = new Parameter(parameters.size());
                }
                parameters.add(p);
                r = p;
                break;
            case KEYWORD:
                if (isToken("SELECT") || isToken("FROM") || isToken("WITH")) {
                    Query query = parseSelect();
                    r = new Subquery(query);
                } else {
                    throw getSyntaxError();
                }
                break;
            case IDENTIFIER:
                String name = currentToken;

                if (currentTokenQuoted) {
                    readToken();
                    if (readIf("(")) {
                        r = readFunction(null, name);
                    } else if (readIf(".")) {
                        r = readTermObjectDot(name);
                    } else {
                        // TODO: a new ExpressionUri expression might be useful
//                    URI uri = URI.create(name);
//                    if (uri != null) {
//                        // r = new ExpressionUri()
//                        r = new ExpressionColumn(database, null, null, name);
//                    }
//                    else {
//                        String squery = currentIsExternal ? name : null;
//                        r = new ExpressionColumn(database, null, null, name, squery);
//                    }

                        String squery = currentIsExternal ? name : null;
                        r = new ExpressionColumn(database, null, null, name, squery);
                    }
                } else {
                    readToken();
                    if (readIf(".")) {
                        r = readTermObjectDot(name);
                        // System.out.println("[" + r + "]");

                    } else if (equalsToken("CASE", name)) {
                        // CASE must be processed before (,
                        // otherwise CASE(3) would be a function call, which it is
                        // not
                        r = readCase();
                    } else if (readIf("(")) {
                        r = readFunction(null, name);
                    } else if (equalsToken("CURRENT_USER", name)) {
                        r = readFunctionWithoutParameters("USER");
                    } else if (equalsToken("CURRENT_TIMESTAMP", name)) {
                        r = readFunctionWithoutParameters("CURRENT_TIMESTAMP");
                    } else if (equalsToken("SYSDATE", name)) {
                        r = readFunctionWithoutParameters("CURRENT_TIMESTAMP");
                    } else if (equalsToken("SYSTIMESTAMP", name)) {
                        r = readFunctionWithoutParameters("CURRENT_TIMESTAMP");
                    } else if (equalsToken("CURRENT_DATE", name)) {
                        r = readFunctionWithoutParameters("CURRENT_DATE");
                    } else if (equalsToken("TODAY", name)) {
                        r = readFunctionWithoutParameters("CURRENT_DATE");
                    } else if (equalsToken("CURRENT_TIME", name)) {
                        r = readFunctionWithoutParameters("CURRENT_TIME");
                    } else if (equalsToken("SYSTIME", name)) {
                        r = readFunctionWithoutParameters("CURRENT_TIME");
                    } else if (equalsToken("CURRENT", name)) {
                        if (readIf("TIMESTAMP")) {
                            r = readFunctionWithoutParameters("CURRENT_TIMESTAMP");
                        } else if (readIf("TIME")) {
                            r = readFunctionWithoutParameters("CURRENT_TIME");
                        } else if (readIf("DATE")) {
                            r = readFunctionWithoutParameters("CURRENT_DATE");
                        } else {
                            String squery = currentIsExternal ? name : null;
                            r = new ExpressionColumn(database, null, null, name, squery);
                        }
                    } else if (equalsToken("NEXT", name) && readIf("VALUE")) {
                        read("FOR");

                        Sequence sequence = readSequence();
                        r = new SequenceValue(sequence);
                    } else if (currentTokenType == VALUE && currentValue.getType() == Value.STRING) {
                        if (equalsToken("DATE", name) || equalsToken("D", name)) {
                            String date = currentValue.getString();
                            readToken();
                            r = ValueExpression.get(ValueDate.parse(date));
                        } else if (equalsToken("TIME", name) || equalsToken("T", name)) {
                            String time = currentValue.getString();
                            readToken();
                            r = ValueExpression.get(ValueTime.parse(time));
                        } else if (equalsToken("TIMESTAMP", name) || equalsToken("TS", name)) {
                            String timestamp = currentValue.getString();
                            readToken();
                            r = ValueExpression.get(ValueTimestamp.parse(timestamp, mode));
                        } else if (equalsToken("X", name)) {
                            readToken();
                            byte[] buffer = StringUtils.convertHexToBytes(currentValue.getString());
                            r = ValueExpression.get(ValueBytes.getNoCopy(buffer));
                        } else if (equalsToken("E", name)) {
                            String text = currentValue.getString();
                            // the PostgreSQL ODBC driver uses
                            // LIKE E'PROJECT\\_DATA' instead of LIKE
                            // 'PROJECT\_DATA'
                            // N: SQL-92 "National Language" strings
                            text = StringUtils.replaceAll(text, "\\\\", "\\");
                            readToken();
                            r = ValueExpression.get(ValueString.get(text));
                        } else if (equalsToken("N", name)) {
                            // SQL-92 "National Language" strings
                            String text = currentValue.getString();
                            readToken();
                            r = ValueExpression.get(ValueString.get(text));
                        } else {
                            String squery = currentIsExternal ? name : null;
                            r = new ExpressionColumn(database, null, null, name, squery);
                        }
                    }
                    else {
                        String squery = currentIsExternal ? name : null;
                        r = new ExpressionColumn(database, null, null, name, squery);
                    }
                }
                break;
            case MINUS:
                readToken();
                if (currentTokenType == VALUE) {
                    r = ValueExpression.get(currentValue.negate());
                    if (r.getType() == Value.LONG && r.getValue(session).getLong() == Integer.MIN_VALUE) {
                        // convert Integer.MIN_VALUE to type 'int'
                        // (Integer.MAX_VALUE+1 is of type 'long')
                        r = ValueExpression.get(ValueInt.get(Integer.MIN_VALUE));
                    } else if (r.getType() == Value.DECIMAL &&
                            r.getValue(session).getBigDecimal().compareTo(ValueLong.MIN_BD) == 0) {
                        // convert Long.MIN_VALUE to type 'long'
                        // (Long.MAX_VALUE+1 is of type 'decimal')
                        r = ValueExpression.get(ValueLong.get(Long.MIN_VALUE));
                    }
                    readToken();
                } else {
                    r = new Operation(Operation.NEGATE, readTerm(), null);
                }
                break;
            case PLUS:
                readToken();
                r = readTerm();
                break;
            case OPEN:
                readToken();
                if (readIf(")")) {
                    r = new ExpressionList(new Expression[0]);
                } else {
                    r = readExpression();
                    if (readIf(",")) {
                        ArrayList<Expression> list = new ArrayList<>();
                        list.add(r);
                        while (!readIf(")")) {
                            r = readExpression();
                            list.add(r);
                            if (!readIf(",")) {
                                read(")");
                                break;
                            }
                        }
                        Expression[] array = new Expression[list.size()];
                        list.toArray(array);
                        r = new ExpressionList(array);
                    } else {
                        read(")");
                    }
                }
                break;
            case TRUE:
                readToken();
                r = ValueExpression.get(ValueBoolean.get(true));
                break;
            case FALSE:
                readToken();
                r = ValueExpression.get(ValueBoolean.get(false));
                break;
            case ROWNUM:
                readToken();
                if (readIf("(")) {
                    read(")");
                }
                if (currentSelect == null && currentPrepared == null) {
                    throw getSyntaxError();
                }
                r = new Rownum(currentSelect == null ? currentPrepared : currentSelect);
                break;
            case NULL:
                readToken();
                r = ValueExpression.getNull();
                break;
            case VALUE:
                r = ValueExpression.get(currentValue);
                readToken();
                break;
            default:
                throw getSyntaxError();
        }

        if (readIf("[")) {
            Function function = Function.getFunction(database, "ARRAY_GET");
            Objects.requireNonNull(function);
            function.setParameter(0, r);
            r = readExpression();
            r = new Operation(Operation.PLUS, r, ValueExpression.get(ValueInt.get(1)));
            function.setParameter(1, r);
            r = function;
            read("]");
        }

        if (readIf("::")) {
            // PostgreSQL compatibility
            if (isToken("PG_CATALOG")) {
                read("PG_CATALOG");
                read(".");
            }
            if (readIf("REGCLASS")) {
                FunctionAlias f = findFunctionAlias(Constants.SCHEMA_MAIN, "PG_GET_OID");
                if (f == null) {
                    throw getSyntaxError();
                }
                Expression[] args = {r};
                r = new JavaFunction(f, args);
            } else {
                // PostgreSQL's casting syntax
                Column col = parseColumnWithType(null);
                Function function = Function.getFunction(database, "CAST");

                Objects.requireNonNull(function);
                function.setDataType(col);
                function.setParameter(0, r);
                r = function;
            }
        }

        if (currentIsExternal && (r instanceof ExpressionColumn)) {
            if (expressionColumnNames == null) {
                expressionColumnNames = new HashSet<>();
            }
            expressionColumnNames.add(r.getColumnName());
        }

        return r;
    }

    Sequence readSequence() {
        // same algorithm as readTableOrView
        String sequenceName = readIdentifierWithSchema(null);
        if (schemaName != null) {
            return getSchema().getSequence(sequenceName);
        }
        Sequence sequence = findSequence(session.getCurrentSchemaName(), sequenceName);
        if (sequence != null) {
            return sequence;
        }
        throw DbException.get(ErrorCode.SEQUENCE_NOT_FOUND_1, sequenceName);
    }

    private Expression readCase() {
        if (readIf("END")) {
            readIf("CASE");
            return ValueExpression.getNull();
        }
        if (readIf("ELSE")) {
            Expression elsePart = readExpression().optimize(session);
            read("END");
            readIf("CASE");
            return elsePart;
        }
        int i;
        Function function;
        if (readIf("WHEN")) {
            function = Function.getFunction(database, "CASE");
            Objects.requireNonNull(function);
            function.setParameter(0, null);
            i = 1;
            do {
                function.setParameter(i++, readExpression());
                read("THEN");
                function.setParameter(i++, readExpression());
            } while (readIf("WHEN"));
        } else {
            Expression expr = readExpression();
            if (readIf("END")) {
                readIf("CASE");
                return ValueExpression.getNull();
            }
            if (readIf("ELSE")) {
                Expression elsePart = readExpression().optimize(session);
                read("END");
                readIf("CASE");
                return elsePart;
            }
            function = Function.getFunction(database, "CASE");
            Objects.requireNonNull(function);
            function.setParameter(0, expr);
            i = 1;
            read("WHEN");
            do {
                function.setParameter(i++, readExpression());
                read("THEN");
                function.setParameter(i++, readExpression());
            } while (readIf("WHEN"));
        }
        if (readIf("ELSE")) {
            function.setParameter(i, readExpression());
        }
        read("END");
        readIf("CASE");
        function.doneWithParameters();
        return function;
    }

    String readString() {
        Expression expr = readExpression().optimize(session);
        if (!(expr instanceof ValueExpression)) {
            throw DbException.getSyntaxError(cleanSql, parseIndex, "string");
        }
        String s = expr.getValue(session).getString();
        return s;
    }

    String readIdentifierWithSchema(String defaultSchemaName) {
        if (currentTokenType != IDENTIFIER) {
            throw DbException.getSyntaxError(cleanSql, parseIndex, "identifier");
        }

        String s = currentToken;
        readToken();
        schemaName = defaultSchemaName;

        if (readIf(".")) {
            schemaName = s;
            if (currentTokenType != IDENTIFIER) {
                throw DbException.getSyntaxError(cleanSql, parseIndex, "identifier");
            }
            s = currentToken;
            readToken();
        }

        if (equalsToken(".", currentToken)) {
            if (equalsToken(schemaName, database.getShortName())) {
                read(".");
                schemaName = s;
                if (currentTokenType != IDENTIFIER) {
                    throw DbException.getSyntaxError(cleanSql, parseIndex, "identifier");
                }
                s = currentToken;
                readToken();
            }
        }

        return s;
    }

    String readIdentifierWithSchema() {
        return readIdentifierWithSchema(session.getCurrentSchemaName());
    }

    String readAliasIdentifier() {
        return readColumnIdentifier();
    }

    String readUniqueIdentifier() {
        return readColumnIdentifier();
    }

    String readColumnIdentifier() {
        if (currentTokenType != IDENTIFIER) {
            throw DbException.getSyntaxError(cleanSql, parseIndex, "identifier");
        }
        String s = currentToken;
        readToken();
        return s;
    }

    Column readTableColumn(TableFilter filter) {
        String tableAlias = null;
        String columnName = readColumnIdentifier();
        if (readIf(".")) {
            tableAlias = columnName;
            columnName = readColumnIdentifier();
            if (readIf(".")) {
                String schema = tableAlias;
                tableAlias = columnName;
                columnName = readColumnIdentifier();
                if (readIf(".")) {
                    String catalogName = schema;
                    schema = tableAlias;
                    tableAlias = columnName;
                    columnName = readColumnIdentifier();
                    if (!equalsToken(catalogName, database.getShortName())) {
                        throw DbException.get(ErrorCode.DATABASE_NOT_FOUND_1, catalogName);
                    }
                }
                if (!equalsToken(schema, filter.getTable().getSchema().getName())) {
                    throw DbException.get(ErrorCode.SCHEMA_NOT_FOUND_1, schema);
                }
            }

            if (!equalsToken(tableAlias, filter.getTableAlias())) {
                throw DbException.get(TABLE_OR_VIEW_NOT_FOUND_1, tableAlias);
            }
        }

        if (database.getSettings().rowId) {
            if (Column.ROWID.equals(columnName)) {
                return filter.getRowIdColumn();
            }
        }

        return filter.getTable().getColumn(columnName);
    }

    boolean readIfExists(boolean ifExists) {
        if (readIf("IF")) {
            read("EXISTS");
            ifExists = true;
        }
        return ifExists;
    }

    String readCommentIf() {
        if (readIf("COMMENT")) {
            readIf("IS");
            return readString();
        }
        return null;
    }

    boolean readIfMore() {
        if (readIf(",")) {
            return !readIf(")");
        }
        read(")");
        return false;
    }

    private static int getCompareType(int tokenType) {
        switch (tokenType) {
            case EQUAL:
                return Comparison.EQUAL;
            case BIGGER_EQUAL:
                return Comparison.BIGGER_EQUAL;
            case BIGGER:
                return Comparison.BIGGER;
            case SMALLER:
                return Comparison.SMALLER;
            case SMALLER_EQUAL:
                return Comparison.SMALLER_EQUAL;
            case NOT_EQUAL:
                return Comparison.NOT_EQUAL;
            case SPATIAL_INTERSECTS:
                return Comparison.SPATIAL_INTERSECTS;
            default:
                return -1;
        }
    }
}
