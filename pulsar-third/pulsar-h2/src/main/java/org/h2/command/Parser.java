/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 *
 * Nicolas Fortin, Atelier SIG, IRSTV FR CNRS 24888
 * Support for the operator "&&" as an alias for SPATIAL_INTERSECTS
 */
package org.h2.command;

import org.h2.api.ErrorCode;
import org.h2.api.Trigger;
import org.h2.command.ddl.*;
import org.h2.command.dml.Set;
import org.h2.command.dml.*;
import org.h2.constraint.ConstraintReferential;
import org.h2.engine.*;
import org.h2.expression.*;
import org.h2.ext.pulsar.ExternalTableInterface;
import org.h2.index.Index;
import org.h2.message.DbException;
import org.h2.result.SortOrder;
import org.h2.schema.Schema;
import org.h2.schema.Sequence;
import org.h2.table.*;
import org.h2.table.TableFilter.TableFilterVisitor;
import org.h2.util.New;
import org.h2.util.StringUtils;
import org.h2.value.*;

import java.nio.charset.Charset;
import java.text.Collator;
import java.util.*;

import static java.lang.System.out;
import static org.h2.api.ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1;

/**
 * Convert a SQL statement string to an command object.
 *
 * @author Thomas Mueller
 * @author Noel Grandin
 * @author Nicolas Fortin, Atelier SIG, IRSTV FR CNRS 24888
 */
public class Parser extends ExpressionParser {

    private static final String WITH_STATEMENT_SUPPORTS_LIMITED_STATEMENTS =
      "WITH statement supports only SELECT, CREATE TABLE, INSERT, UPDATE, MERGE or DELETE statements";

    private CreateView createView;

    private boolean literalsChecked;
    private boolean rightsChecked;

    private ArrayList<Parameter> suppliedParameterList;

    /**
     * Select appearance order
     */
    private int selectSequence;

    /**
     * Table appearance order
     */
    private int tableSequence;

    public Parser(Session session, String originalSQL) {
        super(session, originalSQL);
    }

    /**
     * Parse the statement and prepare it for execution.
     *
     * @return the prepared object
     */
    public Prepared prepare() {
        Prepared p = parse();
        p.prepare();
        if (currentTokenType != END) {
            throw getSyntaxError();
        }
        return p;
    }

    /**
     * Parse the statement, but don't prepare it for execution.
     *
     * @return the prepared object
     */
    Prepared parse() {
        Prepared p;
        try {
            // first, try the fast variant
            p = parse(false);
        } catch (DbException e) {
            if (e.getErrorCode() == ErrorCode.SYNTAX_ERROR_1) {
                // now, get the detailed exception
                p = parse(true);
            } else {
                throw e.addSQL(originalSQL);
            }
        }

        if (!isToken("=>") && !isToken(";") && currentTokenType != END) {
            throw getSyntaxError();
        }

        p.setPrepareAlways(recompileAlways);
        p.setParameterList(parameters);

        return p;
    }

    private Prepared parse(boolean withExpectedList) {
        // out.println("==Parser::parse==, withExpectedList: " + withExpectedList);

        if (!isAnnotated() || withExpectedList) {
            annotate();
        }

        if (withExpectedList) {
            expectedList = new ArrayList<>();
        } else {
            expectedList = null;
        }

        parameters = new ArrayList<>();
        currentIsExternal = false;
        currentSelect = null;
        currentSelectIndex = -1;
        currentPrepared = null;
        createView = null;
        recompileAlways = false;
        // TODO: check
        indexedParameterList = suppliedParameterList;
        expressionColumnNames = null;

        // keep the order still increasing
        // tableSequence = do not reset;
        // selectSequence = do not reset;

        // read the first token
        readToken();

        return parsePrepared();
    }

    private Prepared parsePrepared() {
        int start = lastParseIndex;
        Prepared p = null;
        String token = currentToken;

        if (token.isEmpty()) {
            p = new NoOperation(session);
        } else {
            char first = token.charAt(0);
            switch (first) {
                case '?':
                    // read the ? as a parameter
                    readTerm();
                    // this is an 'out' parameter - set a dummy value
                    parameters.get(0).setValue(ValueNull.INSTANCE);
                    read("=");
                    read("CALL");
                    p = parseCall();
                    break;
                case '(':
                    p = parseSelect();
                    break;
                case 'a':
                case 'A':
                    if (readIf("ALTER")) {
                        p = parseAlter();
                    } else if (readIf("ANALYZE")) {
                        p = parseAnalyze();
                    }
                    break;
                case 'b':
                case 'B':
                    if (readIf("BACKUP")) {
                        p = parseBackup();
                    } else if (readIf("BEGIN")) {
                        p = parseBegin();
                    }
                    break;
                case 'c':
                case 'C':
                    if (readIf("COMMIT")) {
                        p = parseCommit();
                    } else if (readIf("CREATE")) {
                        p = parseCreate();
                    } else if (readIf("CALL")) {
                        p = parseCall();
                    } else if (readIf("CHECKPOINT")) {
                        p = parseCheckpoint();
                    } else if (readIf("COMMENT")) {
                        p = parseComment();
                    }
                    break;
                case 'd':
                case 'D':
                    if (readIf("DELETE")) {
                        p = parseDelete();
                    } else if (readIf("DROP")) {
                        p = parseDrop();
                    } else if (readIf("DECLARE")) {
                        // support for DECLARE GLOBAL TEMPORARY TABLE...
                        p = parseCreate();
                    } else if (readIf("DEALLOCATE")) {
                        p = parseDeallocate();
                    }
                    break;
                case 'e':
                case 'E':
                    if (readIf("EXPLAIN")) {
                        p = parseExplain();
                    } else if (readIf("EXTRACT")) {
                        p = parseExtract();
                    } else if (readIf("EXECUTE")) {
                        p = parseExecute();
                    }
                    break;
                case 'f':
                case 'F':
                    if (isToken("FROM")) {
                        p = parseSelect();
                    }
                    break;
                case 'g':
                case 'G':
                    if (readIf("GRANT")) {
                        p = parseGrantRevoke(CommandInterface.GRANT);
                    }
                    break;
                case 'h':
                case 'H':
                    if (readIf("HELP")) {
                        p = parseHelp();
                    }
                    break;
                case 'i':
                case 'I':
                    if (readIf("INSERT")) {
                        p = parseInsert();
                    }
                    break;
                case 'm':
                case 'M':
                    if (readIf("MERGE")) {
                        p = parseMerge();
                    }
                    break;
                case 'p':
                case 'P':
                    if (readIf("PREPARE")) {
                        p = parsePrepare();
                    }
                    break;
                case 'r':
                case 'R':
                    if (readIf("ROLLBACK")) {
                        p = parseRollback();
                    } else if (readIf("REVOKE")) {
                        p = parseGrantRevoke(CommandInterface.REVOKE);
                    } else if (readIf("RUNSCRIPT")) {
                        p = parseRunScript();
                    } else if (readIf("RELEASE")) {
                        p = parseReleaseSavepoint();
                    } else if (readIf("REPLACE")) {
                        p = parseReplace();
                    }
                    break;
                case 's':
                case 'S':
                    if (isToken("SELECT")) {
                        p = parseSelect();
                    } else if (readIf("SET")) {
                        p = parseSet();
                    } else if (readIf("SAVEPOINT")) {
                        p = parseSavepoint();
                    } else if (readIf("SCRIPT")) {
                        p = parseScript();
                    } else if (readIf("SHUTDOWN")) {
                        p = parseShutdown();
                    } else if (readIf("SHOW")) {
                        p = parseShow();
                    }
                    break;
                case 't':
                case 'T':
                    if (readIf("TRUNCATE")) {
                        p = parseTruncate();
                    }
                    break;
                case 'u':
                case 'U':
                    if (readIf("UPDATE")) {
                        p = parseUpdate();
                    } else if (readIf("USE")) {
                        p = parseUse();
                    }
                    break;
                case 'v':
                case 'V':
                    if (readIf("VALUES")) {
                        p = parseValues();
                    }
                    break;
                case 'w':
                case 'W':
                    if (readIf("WITH")) {
                        p = parseWithStatementOrQuery();
                    }
                    break;
                case ';':
                    p = new NoOperation(session);
                    break;
                default:
                    throw getSyntaxError();
            }

            if (indexedParameterList != null) {
                for (int i = 0; i < indexedParameterList.size(); i++) {
                    if (indexedParameterList.get(i) == null) {
                        indexedParameterList.set(i, new Parameter(i));
                    }
                }
                parameters = indexedParameterList;
            }

            if (readIf("{")) {
                do {
                    int index = (int) readLong() - 1;
                    if (index < 0 || index >= parameters.size()) {
                        throw getSyntaxError();
                    }
                    Parameter parameter = parameters.get(index);
                    if (p == null) {
                        throw getSyntaxError();
                    }
                    read(":");
                    Expression expr = readExpression();
                    expr = expr.optimize(session);
                    parameter.setValue(expr.getValue(session));
                } while (readIf(","));
                read("}");
                for (Parameter parameter : parameters) {
                    parameter.checkSet();
                }
                parameters.clear();
            }
        }

        if (p == null) {
            throw getSyntaxError();
        }

        setSQL(p, null, start);
        return p;
    }

    private Prepared parseBackup() {
        BackupCommand command = new BackupCommand(session);
        read("TO");
        command.setFileName(readExpression());
        return command;
    }

    private Prepared parseAnalyze() {
        Analyze command = new Analyze(session);
        if (readIf("TABLE")) {
            Table table = readTableOrView();
            command.setTable(table);
        }
        if (readIf("SAMPLE_SIZE")) {
            command.setTop(readPositiveInt());
        }
        return command;
    }

    private TransactionCommand parseBegin() {
        TransactionCommand command;
        if (!readIf("WORK")) {
            readIf("TRANSACTION");
        }
        command = new TransactionCommand(session, CommandInterface.BEGIN);
        return command;
    }

    private TransactionCommand parseCommit() {
        TransactionCommand command;
        if (readIf("TRANSACTION")) {
            command = new TransactionCommand(session, CommandInterface.COMMIT_TRANSACTION);
            command.setTransactionName(readUniqueIdentifier());
            return command;
        }
        command = new TransactionCommand(session, CommandInterface.COMMIT);
        readIf("WORK");
        return command;
    }

    private TransactionCommand parseShutdown() {
        int type = CommandInterface.SHUTDOWN;
        if (readIf("IMMEDIATELY")) {
            type = CommandInterface.SHUTDOWN_IMMEDIATELY;
        } else if (readIf("COMPACT")) {
            type = CommandInterface.SHUTDOWN_COMPACT;
        } else if (readIf("DEFRAG")) {
            type = CommandInterface.SHUTDOWN_DEFRAG;
        } else {
            readIf("SCRIPT");
        }
        return new TransactionCommand(session, type);
    }

    private TransactionCommand parseRollback() {
        TransactionCommand command;
        if (readIf("TRANSACTION")) {
            command = new TransactionCommand(session, CommandInterface.ROLLBACK_TRANSACTION);
            command.setTransactionName(readUniqueIdentifier());
            return command;
        }
        if (readIf("TO")) {
            read("SAVEPOINT");
            command = new TransactionCommand(session, CommandInterface.ROLLBACK_TO_SAVEPOINT);
            command.setSavepointName(readUniqueIdentifier());
        } else {
            readIf("WORK");
            command = new TransactionCommand(session, CommandInterface.ROLLBACK);
        }
        return command;
    }

    private Prepared parsePrepare() {
        if (readIf("COMMIT")) {
            TransactionCommand command = new TransactionCommand(session, CommandInterface.PREPARE_COMMIT);
            command.setTransactionName(readUniqueIdentifier());
            return command;
        }
        String procedureName = readAliasIdentifier();
        if (readIf("(")) {
            ArrayList<Column> list = new ArrayList<>();
            for (int i = 0; ; i++) {
                Column column = parseColumnForTable("C" + i, true);
                list.add(column);
                if (readIf(")")) {
                    break;
                }
                read(",");
            }
        }
        read("AS");
        Prepared prep = parsePrepared();
        PrepareProcedure command = new PrepareProcedure(session);
        command.setProcedureName(procedureName);
        command.setPrepared(prep);
        return command;
    }

    private TransactionCommand parseSavepoint() {
        TransactionCommand command = new TransactionCommand(session, CommandInterface.SAVEPOINT);
        command.setSavepointName(readUniqueIdentifier());
        return command;
    }

    private Prepared parseReleaseSavepoint() {
        Prepared command = new NoOperation(session);
        readIf("SAVEPOINT");
        readUniqueIdentifier();
        return command;
    }

    private Update parseUpdate() {
        Update command = new Update(session);
        currentPrepared = command;
        int start = lastParseIndex;
        TableFilter filter = readSimpleTableFilter(0);
        command.setTableFilter(filter);
        read("SET");
        if (readIf("(")) {
            ArrayList<Column> columns = new ArrayList<>();
            do {
                Column column = readTableColumn(filter);
                columns.add(column);
            } while (readIf(","));
            read(")");
            read("=");
            Expression expression = readExpression();
            if (columns.size() == 1) {
                // the expression is parsed as a simple value
                command.setAssignment(columns.get(0), expression);
            } else {
                for (int i = 0, size = columns.size(); i < size; i++) {
                    Column column = columns.get(i);
                    Function f = Function.getFunction(database, "ARRAY_GET");
                    Objects.requireNonNull(f);
                    f.setParameter(0, expression);
                    f.setParameter(1, ValueExpression.get(ValueInt.get(i + 1)));
                    f.doneWithParameters();
                    command.setAssignment(column, f);
                }
            }
        } else {
            do {
                Column column = readTableColumn(filter);
                read("=");
                Expression expression;
                if (readIf("DEFAULT")) {
                    expression = ValueExpression.getDefault();
                } else {
                    expression = readExpression();
                }
                command.setAssignment(column, expression);
            } while (readIf(","));
        }

        if (readIf("WHERE")) {
            Expression condition = readExpression();
            command.setCondition(condition);
        }
        if (readIf("ORDER")) {
            // for MySQL compatibility
            // (this syntax is supported, but ignored)
            read("BY");
            parseSimpleOrderList();
        }
        if (readIf("LIMIT")) {
            Expression limit = readTerm().optimize(session);
            command.setLimit(limit);
        }
        setSQL(command, "UPDATE", start);
        return command;
    }

    private TableFilter readSimpleTableFilter(int orderInFrom) {
        Table table = readTableOrView();
        String alias = readTableAlias();
        return new TableFilter(session, table, alias, rightsChecked, currentSelect, orderInFrom, null);
    }

    private String readTableAlias() {
        String alias = null;
        if (readIf("AS")) {
            alias = readAliasIdentifier();
        } else if (currentTokenType == IDENTIFIER) {
            if (!equalsToken("SET", currentToken)) {
                // SET is not a keyword (PostgreSQL supports it as a table name)
                alias = readAliasIdentifier();
            }
        }
        return alias;
    }

    private Delete parseDelete() {
        Delete command = new Delete(session);
        Expression limit = null;
        if (readIf("TOP")) {
            limit = readTerm().optimize(session);
        }
        currentPrepared = command;
        int start = lastParseIndex;
        if (!readIf("FROM") && mode == Mode.getMySQL()) {
            readIdentifierWithSchema();
            read("FROM");
        }
        TableFilter filter = readSimpleTableFilter(0);
        command.setTableFilter(filter);
        if (readIf("WHERE")) {
            Expression condition = readExpression();
            command.setCondition(condition);
        }
        if (readIf("LIMIT") && limit == null) {
            limit = readTerm().optimize(session);
        }
        command.setLimit(limit);
        setSQL(command, "DELETE", start);
        return command;
    }

    private IndexColumn[] parseIndexColumnList() {
        ArrayList<IndexColumn> columns = new ArrayList<>();
        do {
            IndexColumn column = new IndexColumn();
            column.columnName = readColumnIdentifier();
            columns.add(column);
            if (readIf("ASC")) {
                // ignore
            } else if (readIf("DESC")) {
                column.sortType = SortOrder.DESCENDING;
            }
            if (readIf("NULLS")) {
                if (readIf("FIRST")) {
                    column.sortType |= SortOrder.NULLS_FIRST;
                } else {
                    read("LAST");
                    column.sortType |= SortOrder.NULLS_LAST;
                }
            }
        } while (readIf(","));
        read(")");
        return columns.toArray(new IndexColumn[columns.size()]);
    }

    private String[] parseColumnList() {
        ArrayList<String> columns = new ArrayList<>();
        do {
            String columnName = readColumnIdentifier();
            columns.add(columnName);
        } while (readIfMore());
        return columns.toArray(new String[columns.size()]);
    }

    private Column[] parseColumnList(Table table) {
        ArrayList<Column> columns = new ArrayList<>();
        HashSet<Column> set = New.hashSet();
        if (!readIf(")")) {
            do {
                Column column = parseColumn(table);
                if (!set.add(column)) {
                    throw DbException.get(ErrorCode.DUPLICATE_COLUMN_NAME_1,
                            column.getSQL());
                }
                columns.add(column);
            } while (readIfMore());
        }
        return columns.toArray(new Column[columns.size()]);
    }

    private Column parseColumn(Table table) {
        String id = readColumnIdentifier();
        if (database.getSettings().rowId && Column.ROWID.equals(id)) {
            return table.getRowIdColumn();
        }
        return table.getColumn(id);
    }

    private Prepared parseHelp() {
        StringBuilder buff = new StringBuilder("SELECT * FROM INFORMATION_SCHEMA.HELP");
        int i = 0;
        ArrayList<Value> paramValues = new ArrayList<>();
        while (currentTokenType != END) {
            String s = currentToken;
            readToken();
            if (i == 0) {
                buff.append(" WHERE ");
            } else {
                buff.append(" AND ");
            }
            i++;
            buff.append("UPPER(TOPIC) LIKE ?");
            paramValues.add(ValueString.get("%" + s + "%"));
        }
        return prepare(session, buff.toString(), paramValues);
    }

    private Prepared parseShow() {
        ArrayList<Value> paramValues = new ArrayList<>();
        StringBuilder buff = new StringBuilder("SELECT ");
        if (readIf("CLIENT_ENCODING")) {
            // for PostgreSQL compatibility
            buff.append("'UNICODE' AS CLIENT_ENCODING FROM DUAL");
        } else if (readIf("DEFAULT_TRANSACTION_ISOLATION")) {
            // for PostgreSQL compatibility
            buff.append("'read committed' AS DEFAULT_TRANSACTION_ISOLATION " +
                    "FROM DUAL");
        } else if (readIf("TRANSACTION")) {
            // for PostgreSQL compatibility
            read("ISOLATION");
            read("LEVEL");
            buff.append("'read committed' AS TRANSACTION_ISOLATION " +
                    "FROM DUAL");
        } else if (readIf("DATESTYLE")) {
            // for PostgreSQL compatibility
            buff.append("'ISO' AS DATESTYLE FROM DUAL");
        } else if (readIf("SERVER_VERSION")) {
            // for PostgreSQL compatibility
            buff.append("'8.1.4' AS SERVER_VERSION FROM DUAL");
        } else if (readIf("SERVER_ENCODING")) {
            // for PostgreSQL compatibility
            buff.append("'UTF8' AS SERVER_ENCODING FROM DUAL");
        } else if (readIf("TABLES")) {
            // for MySQL compatibility
            String schema = Constants.SCHEMA_MAIN;
            if (readIf("FROM")) {
                schema = readUniqueIdentifier();
            }
            buff.append("TABLE_NAME, TABLE_SCHEMA FROM "
                    + "INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_SCHEMA=? ORDER BY TABLE_NAME");
            paramValues.add(ValueString.get(schema));
        } else if (readIf("COLUMNS")) {
            // for MySQL compatibility
            read("FROM");
            String tableName = readIdentifierWithSchema();
            String schemaName = getSchema().getName();
            paramValues.add(ValueString.get(tableName));
            if (readIf("FROM")) {
                schemaName = readUniqueIdentifier();
            }
            buff.append("C.COLUMN_NAME FIELD, "
                    + "C.TYPE_NAME || '(' || C.NUMERIC_PRECISION || ')' TYPE, "
                    + "C.IS_NULLABLE \"NULL\", "
                    + "CASE (SELECT MAX(I.INDEX_TYPE_NAME) FROM "
                    + "INFORMATION_SCHEMA.INDEXES I "
                    + "WHERE I.TABLE_SCHEMA=C.TABLE_SCHEMA "
                    + "AND I.TABLE_NAME=C.TABLE_NAME "
                    + "AND I.COLUMN_NAME=C.COLUMN_NAME)"
                    + "WHEN 'PRIMARY KEY' THEN 'PRI' "
                    + "WHEN 'UNIQUE INDEX' THEN 'UNI' ELSE '' END KEY, "
                    + "IFNULL(COLUMN_DEFAULT, 'NULL') DEFAULT "
                    + "FROM INFORMATION_SCHEMA.COLUMNS C "
                    + "WHERE C.TABLE_NAME=? AND C.TABLE_SCHEMA=? "
                    + "ORDER BY C.ORDINAL_POSITION");
            paramValues.add(ValueString.get(schemaName));
        } else if (readIf("DATABASES") || readIf("SCHEMAS")) {
            // for MySQL compatibility
            buff.append("SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA");
        }
        boolean b = session.getAllowLiterals();
        try {
            // need to temporarily enable it, in case we are in
            // ALLOW_LITERALS_NUMBERS mode
            session.setAllowLiterals(true);
            return prepare(session, buff.toString(), paramValues);
        } finally {
            session.setAllowLiterals(b);
        }
    }

    private static Prepared prepare(Session s, String sql, ArrayList<Value> paramValues) {
        Prepared prep = s.prepare(sql);
        ArrayList<Parameter> params = prep.getParameters();
        if (params != null) {
            for (int i = 0, size = params.size(); i < size; i++) {
                Parameter p = params.get(i);
                p.setValue(paramValues.get(i));
            }
        }
        return prep;
    }

    private Merge parseMerge() {
        Merge command = new Merge(session);
        currentPrepared = command;
        read("INTO");
        Table table = readTableOrView();
        command.setTable(table);
        if (readIf("(")) {
            if (isSelect()) {
                command.setQuery(parseSelect());
                read(")");
                return command;
            }
            Column[] columns = parseColumnList(table);
            command.setColumns(columns);
        }
        if (readIf("KEY")) {
            read("(");
            Column[] keys = parseColumnList(table);
            command.setKeys(keys);
        }
        if (readIf("VALUES")) {
            do {
                ArrayList<Expression> values = new ArrayList<>();
                read("(");
                if (!readIf(")")) {
                    do {
                        if (readIf("DEFAULT")) {
                            values.add(null);
                        } else {
                            values.add(readExpression());
                        }
                    } while (readIfMore());
                }
                command.addRow(values.toArray(new Expression[values.size()]));
            } while (readIf(","));
        } else {
            command.setQuery(parseSelect());
        }
        return command;
    }

    private Insert parseInsert() {
        Insert command = new Insert(session);
        currentPrepared = command;
        read("INTO");
        Table table = readTableOrView();
        command.setTable(table);
        Column[] columns = null;
        if (readIf("(")) {
            if (isSelect()) {
                command.setQuery(parseSelect());
                read(")");
                return command;
            }
            columns = parseColumnList(table);
            command.setColumns(columns);
        }
        if (readIf("DIRECT")) {
            command.setInsertFromSelect(true);
        }
        if (readIf("SORTED")) {
            command.setSortedInsertMode(true);
        }
        if (readIf("DEFAULT")) {
            read("VALUES");
            Expression[] expr = {};
            command.addRow(expr);
        } else if (readIf("VALUES")) {
            read("(");
            do {
                ArrayList<Expression> values = new ArrayList<>();
                if (!readIf(")")) {
                    do {
                        if (readIf("DEFAULT")) {
                            values.add(null);
                        } else {
                            values.add(readExpression());
                        }
                    } while (readIfMore());
                }
                command.addRow(values.toArray(new Expression[values.size()]));
                // the following condition will allow (..),; and (..);
            } while (readIf(",") && readIf("("));
        } else if (readIf("SET")) {
            if (columns != null) {
                throw getSyntaxError();
            }
            ArrayList<Column> columnList = new ArrayList<>();
            ArrayList<Expression> values = new ArrayList<>();
            do {
                columnList.add(parseColumn(table));
                read("=");
                Expression expression;
                if (readIf("DEFAULT")) {
                    expression = ValueExpression.getDefault();
                } else {
                    expression = readExpression();
                }
                values.add(expression);
            } while (readIf(","));
            command.setColumns(columnList.toArray(new Column[columnList.size()]));
            command.addRow(values.toArray(new Expression[values.size()]));
        } else {
            command.setQuery(parseSelect());
        }
        if (mode.onDuplicateKeyUpdate) {
            if (readIf("ON")) {
                read("DUPLICATE");
                read("KEY");
                read("UPDATE");
                do {
                    Column column = parseColumn(table);
                    read("=");
                    Expression expression;
                    if (readIf("DEFAULT")) {
                        expression = ValueExpression.getDefault();
                    } else {
                        expression = readExpression();
                    }
                    command.addAssignmentForDuplicate(column, expression);
                } while (readIf(","));
            }
        }
        if (mode.isolationLevelInSelectOrInsertStatement) {
            parseIsolationClause();
        }
        return command;
    }

    /**
     * MySQL compatibility. REPLACE is similar to MERGE.
     */
    private Replace parseReplace() {
        Replace command = new Replace(session);
        currentPrepared = command;
        read("INTO");
        Table table = readTableOrView();
        command.setTable(table);
        if (readIf("(")) {
            if (isSelect()) {
                command.setQuery(parseSelect());
                read(")");
                return command;
            }
            Column[] columns = parseColumnList(table);
            command.setColumns(columns);
        }
        if (readIf("VALUES")) {
            do {
                ArrayList<Expression> values = new ArrayList<>();
                read("(");
                if (!readIf(")")) {
                    do {
                        if (readIf("DEFAULT")) {
                            values.add(null);
                        } else {
                            values.add(readExpression());
                        }
                    } while (readIfMore());
                }
                command.addRow(values.toArray(new Expression[values.size()]));
            } while (readIf(","));
        } else {
            command.setQuery(parseSelect());
        }
        return command;
    }

    private TableFilter readTableFilter(boolean fromOuter) {
        Table table;
        String alias = null;
        if (readIf("(")) {
            if (isSelect()) {
                Query query = parseSelectUnion();
                read(")");
                query.setParameterList(New.arrayList(parameters));
                query.init();
                Session s;
                if (createView != null) {
                    s = database.getSystemSession();
                } else {
                    s = session;
                }
                alias = session.getNextSystemIdentifier(cleanSql);
                table = TableView.createTempView(s, session.getUser(), alias, query, currentSelect);
            } else {
                TableFilter top;
                if (database.getSettings().nestedJoins) {
                    top = readTableFilter(false);
                    top = readJoin(top, currentSelect, false, false);
                    top = getNested(top);
                } else {
                    top = readTableFilter(fromOuter);
                    top = readJoin(top, currentSelect, false, fromOuter);
                }
                read(")");
                alias = readFromAlias(null);
                if (alias != null) {
                    top.setAlias(alias);
                }
                return top;
            }
        } else if (readIf("VALUES")) {
            // select * from values(1, "hello"), (2, "world")
            table = parseValuesTable(0).getTable();
        } else if (readIf("WHICH")) {
            // debug(20000);
            // Sigma-SQL support
            //
            // select a, b, c from table(a int=(1,2), b varchar=('a', 'b'), c decimal=(3.8, 4.8))
            // =>
            // from which w select w._1, w._2;
            currentIsExternal = false;
            table = readTableOrView("WHICH");
            // debug(20001);
        } else {
            String tableName = readIdentifierWithSchema(null);
            Schema schema = getSchema();
            boolean foundLeftBracket = readIf("(");
            if (foundLeftBracket && readIf("INDEX")) {
                // Sybase compatibility with
                // "select * from test (index table1_index)"
                readIdentifierWithSchema(null);
                read(")");
                foundLeftBracket = false;
            }

            if (foundLeftBracket) {
                Schema mainSchema = database.getSchema(Constants.SCHEMA_MAIN);
                if (equalsToken(tableName, RangeTable.NAME) || equalsToken(tableName, RangeTable.ALIAS)) {
                    // Range functional table
                    Expression min = readExpression();
                    read(",");
                    Expression max = readExpression();
                    if (readIf(",")) {
                        Expression step = readExpression();
                        read(")");
                        table = new RangeTable(mainSchema, min, max, step, false);
                    } else {
                        read(")");
                        table = new RangeTable(mainSchema, min, max, false);
                    }
                } else {
                    // Other function table
                    Expression expr = readFunction(schema, tableName);
                    if (!(expr instanceof FunctionCall)) {
                        throw getSyntaxError();
                    }
                    FunctionCall call = (FunctionCall) expr;
                    if (!call.isDeterministic()) {
                        recompileAlways = true;
                    }
                    // System.out.println("...call: " + call.getSQL() + "...");
                    table = new FunctionTable(mainSchema, session, expr, call);
                }
            } else if (equalsToken("DUAL", tableName)) {
                table = getDualTable(false);
            } else if (mode.sysDummy1 && equalsToken("SYSDUMMY1", tableName)) {
                table = getDualTable(false);
            } else {
                table = readTableOrView(tableName);
            }
        }

        // debug(20002);

        IndexHints indexHints = null;
        // for backward compatibility, handle case where USE is a table alias
        if (readIf("USE")) {
            if (readIf("INDEX")) {
                indexHints = parseIndexHints(table);
            } else {
                alias = "USE";
            }
        } else {
            // debug(20003);

            alias = readFromAlias(alias);

            // debug(20004);

            if (alias != null) {
                // if alias present, a second chance to parse index hints
                if (readIf("USE")) {
                    read("INDEX");
                    indexHints = parseIndexHints(table);
                }
            }
        }

        // debug(20005);

        if (currentIsExternal) {
            // debug(20006);

            if (!(table instanceof ExternalTableInterface)) {
                throw getSyntaxError();
            }
            ExternalTableInterface externalTable = (ExternalTableInterface) table;

            if (readIf("WINDOW")) {
                externalTable.setWindowSize(readExpression().optimize(session));
                out.println("WINDOW");
                externalTable.setWindowInterval(readExpression().optimize(session));
            }

            if (readIf("PERFORM")) {
                if (readIf("JAVASCRIPT")) {
                    externalTable.setAction(readExpression().optimize(session));
                } else {
                    throw DbException.get(ErrorCode.INVALID_VALUE_2, "LANGUAGE IS NOT SUPPORTED");
                }
            }

            // debug(20007);
        }

        // debug(20008);

        // inherit alias for temporary views (usually CTE's) from table name
        if(table.isView() && table.isTemporary() && alias==null) {
            alias = table.getName();
        }

        return new TableFilter(session, table, alias, rightsChecked, currentSelect, tableSequence++, indexHints);
    }

    private IndexHints parseIndexHints(Table table) {
        if (table == null) {
            throw getSyntaxError();
        }
        read("(");
        LinkedHashSet<String> indexNames = new LinkedHashSet<>();
        if (!readIf(")")) {
            do {
                String indexName = readIdentifierWithSchema();
                Index index = table.getIndex(indexName);
                indexNames.add(index.getName());
            } while (readIf(","));
            read(")");
        }
        return IndexHints.createUseIndexHints(indexNames);
    }

    private String readFromAlias(String alias) {
        if (readIf("AS")) {
            alias = readAliasIdentifier();
        } else if (currentTokenType == IDENTIFIER) {
            // left and right are not keywords (because they are functions as
            // well)
            if (!isToken("LEFT") && !isToken("RIGHT") && !isToken("FULL") && !isToken("WINDOW")) {
                alias = readAliasIdentifier();
            }
        }
        return alias;
    }

    private Prepared parseTruncate() {
        read("TABLE");
        Table table = readTableOrView();
        TruncateTable command = new TruncateTable(session);
        command.setTable(table);
        return command;
    }

    private Prepared parseComment() {
        int type = 0;
        read("ON");
        boolean column = false;
        if (readIf("TABLE") || readIf("VIEW")) {
            type = DbObject.TABLE_OR_VIEW;
        } else if (readIf("COLUMN")) {
            column = true;
            type = DbObject.TABLE_OR_VIEW;
        } else if (readIf("CONSTANT")) {
            type = DbObject.CONSTANT;
        } else if (readIf("CONSTRAINT")) {
            type = DbObject.CONSTRAINT;
        } else if (readIf("ALIAS")) {
            type = DbObject.FUNCTION_ALIAS;
        } else if (readIf("INDEX")) {
            type = DbObject.INDEX;
        } else if (readIf("ROLE")) {
            type = DbObject.ROLE;
        } else if (readIf("SCHEMA")) {
            type = DbObject.SCHEMA;
        } else if (readIf("SEQUENCE")) {
            type = DbObject.SEQUENCE;
        } else if (readIf("TRIGGER")) {
            type = DbObject.TRIGGER;
        } else if (readIf("USER")) {
            type = DbObject.USER;
        } else if (readIf("DOMAIN")) {
            type = DbObject.USER_DATATYPE;
        } else {
            throw getSyntaxError();
        }
        SetComment command = new SetComment(session);
        String objectName;
        if (column) {
            // can't use readIdentifierWithSchema() because
            // it would not read schema.table.column correctly
            // if the db name is equal to the schema name
            ArrayList<String> list = new ArrayList<>();
            do {
                list.add(readUniqueIdentifier());
            } while (readIf("."));
            schemaName = session.getCurrentSchemaName();
            if (list.size() == 4) {
                if (!equalsToken(database.getShortName(), list.get(0))) {
                    throw DbException.getSyntaxError(cleanSql, parseIndex,
                            "database name");
                }
                list.remove(0);
            }
            if (list.size() == 3) {
                schemaName = list.get(0);
                list.remove(0);
            }
            if (list.size() != 2) {
                throw DbException.getSyntaxError(cleanSql, parseIndex, "table.column");
            }
            objectName = list.get(0);
            command.setColumn(true);
            command.setColumnName(list.get(1));
        } else {
            objectName = readIdentifierWithSchema();
        }
        command.setSchemaName(schemaName);
        command.setObjectName(objectName);
        command.setObjectType(type);
        read("IS");
        command.setCommentExpression(readExpression());
        return command;
    }

    private Prepared parseDrop() {
        if (readIf("TABLE")) {
            boolean ifExists = readIfExists(false);
            String tableName = readIdentifierWithSchema();
            DropTable command = new DropTable(session, getSchema());
            command.setTableName(tableName);
            while (readIf(",")) {
                tableName = readIdentifierWithSchema();
                DropTable next = new DropTable(session, getSchema());
                next.setTableName(tableName);
                command.addNextDropTable(next);
            }
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            if (readIf("CASCADE")) {
                command.setDropAction(ConstraintReferential.CASCADE);
                readIf("CONSTRAINTS");
            } else if (readIf("RESTRICT")) {
                command.setDropAction(ConstraintReferential.RESTRICT);
            } else if (readIf("IGNORE")) {
                command.setDropAction(ConstraintReferential.SET_DEFAULT);
            }
            return command;
        } else if (readIf("INDEX")) {
            boolean ifExists = readIfExists(false);
            String indexName = readIdentifierWithSchema();
            DropIndex command = new DropIndex(session, getSchema());
            command.setIndexName(indexName);
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            return command;
        } else if (readIf("USER")) {
            boolean ifExists = readIfExists(false);
            DropUser command = new DropUser(session);
            command.setUserName(readUniqueIdentifier());
            ifExists = readIfExists(ifExists);
            readIf("CASCADE");
            command.setIfExists(ifExists);
            return command;
        } else if (readIf("SEQUENCE")) {
            boolean ifExists = readIfExists(false);
            String sequenceName = readIdentifierWithSchema();
            DropSequence command = new DropSequence(session, getSchema());
            command.setSequenceName(sequenceName);
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            return command;
        } else if (readIf("CONSTANT")) {
            boolean ifExists = readIfExists(false);
            String constantName = readIdentifierWithSchema();
            DropConstant command = new DropConstant(session, getSchema());
            command.setConstantName(constantName);
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            return command;
        } else if (readIf("TRIGGER")) {
            boolean ifExists = readIfExists(false);
            String triggerName = readIdentifierWithSchema();
            DropTrigger command = new DropTrigger(session, getSchema());
            command.setTriggerName(triggerName);
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            return command;
        } else if (readIf("VIEW")) {
            boolean ifExists = readIfExists(false);
            String viewName = readIdentifierWithSchema();
            DropView command = new DropView(session, getSchema());
            command.setViewName(viewName);
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            Integer dropAction = parseCascadeOrRestrict();
            if (dropAction != null) {
                command.setDropAction(dropAction);
            }
            return command;
        } else if (readIf("ROLE")) {
            boolean ifExists = readIfExists(false);
            DropRole command = new DropRole(session);
            command.setRoleName(readUniqueIdentifier());
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            return command;
        } else if (readIf("ALIAS")) {
            boolean ifExists = readIfExists(false);
            String aliasName = readIdentifierWithSchema();
            DropFunctionAlias command = new DropFunctionAlias(session,
                    getSchema());
            command.setAliasName(aliasName);
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            return command;
        } else if (readIf("SCHEMA")) {
            boolean ifExists = readIfExists(false);
            DropSchema command = new DropSchema(session);
            command.setSchemaName(readUniqueIdentifier());
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            return command;
        } else if (readIf("ALL")) {
            read("OBJECTS");
            DropDatabase command = new DropDatabase(session);
            command.setDropAllObjects(true);
            if (readIf("DELETE")) {
                read("FILES");
                command.setDeleteFiles(true);
            }
            return command;
        } else if (readIf("DOMAIN")) {
            return parseDropUserDataType();
        } else if (readIf("TYPE")) {
            return parseDropUserDataType();
        } else if (readIf("DATATYPE")) {
            return parseDropUserDataType();
        } else if (readIf("AGGREGATE")) {
            return parseDropAggregate();
        } else if (readIf("SYNONYM")) {
            boolean ifExists = readIfExists(false);
            String synonymName = readIdentifierWithSchema();
            DropSynonym command = new DropSynonym(session, getSchema());
            command.setSynonymName(synonymName);
            ifExists = readIfExists(ifExists);
            command.setIfExists(ifExists);
            return command;
        }
        throw getSyntaxError();
    }

    private DropUserDataType parseDropUserDataType() {
        boolean ifExists = readIfExists(false);
        DropUserDataType command = new DropUserDataType(session);
        command.setTypeName(readUniqueIdentifier());
        ifExists = readIfExists(ifExists);
        command.setIfExists(ifExists);
        return command;
    }

    private DropAggregate parseDropAggregate() {
        boolean ifExists = readIfExists(false);
        DropAggregate command = new DropAggregate(session);
        command.setName(readUniqueIdentifier());
        ifExists = readIfExists(ifExists);
        command.setIfExists(ifExists);
        return command;
    }

    private TableFilter readJoin(TableFilter top, Select command, boolean nested, boolean fromOuter) {
        boolean joined = false;
        TableFilter last = top;
        boolean nestedJoins = database.getSettings().nestedJoins;
        while (true) {
            if (readIf("RIGHT")) {
                readIf("OUTER");
                read("JOIN");
                joined = true;
                // the right hand side is the 'inner' table usually
                TableFilter newTop = readTableFilter(fromOuter);
                newTop = readJoin(newTop, command, nested, true);
                Expression on = null;
                if (readIf("ON")) {
                    on = readExpression();
                }
                if (nestedJoins) {
                    top = getNested(top);
                    newTop.addJoin(top, true, false, on);
                } else {
                    newTop.addJoin(top, true, false, on);
                }
                top = newTop;
                last = newTop;
            } else if (readIf("LEFT")) {
                readIf("OUTER");
                read("JOIN");
                joined = true;
                TableFilter join = readTableFilter(true);
                if (nestedJoins) {
                    join = readJoin(join, command, true, true);
                } else {
                    top = readJoin(top, command, false, true);
                }
                Expression on = null;
                if (readIf("ON")) {
                    on = readExpression();
                }
                top.addJoin(join, true, false, on);
                last = join;
            } else if (readIf("FULL")) {
                throw getSyntaxError();
            } else if (readIf("INNER")) {
                read("JOIN");
                joined = true;
                TableFilter join = readTableFilter(fromOuter);
                top = readJoin(top, command, false, false);
                Expression on = null;
                if (readIf("ON")) {
                    on = readExpression();
                }
                if (nestedJoins) {
                    top.addJoin(join, false, false, on);
                } else {
                    top.addJoin(join, fromOuter, false, on);
                }
                last = join;
            } else if (readIf("JOIN")) {
                joined = true;
                TableFilter join = readTableFilter(fromOuter);
                top = readJoin(top, command, false, false);
                Expression on = null;
                if (readIf("ON")) {
                    on = readExpression();
                }
                if (nestedJoins) {
                    top.addJoin(join, false, false, on);
                } else {
                    top.addJoin(join, fromOuter, false, on);
                }
                last = join;
            } else if (readIf("CROSS")) {
                read("JOIN");
                joined = true;
                TableFilter join = readTableFilter(fromOuter);
                if (nestedJoins) {
                    top.addJoin(join, false, false, null);
                } else {
                    top.addJoin(join, fromOuter, false, null);
                }
                last = join;
            } else if (readIf("NATURAL")) {
                read("JOIN");
                joined = true;
                TableFilter join = readTableFilter(fromOuter);
                Column[] tableCols = last.getTable().getColumns();
                Column[] joinCols = join.getTable().getColumns();
                String tableSchema = last.getTable().getSchema().getName();
                String joinSchema = join.getTable().getSchema().getName();
                Expression on = null;
                for (Column tc : tableCols) {
                    String tableColumnName = tc.getName();
                    for (Column c : joinCols) {
                        String joinColumnName = c.getName();
                        if (equalsToken(tableColumnName, joinColumnName)) {
                            join.addNaturalJoinColumn(c);
                            Expression tableExpr = new ExpressionColumn(database, tableSchema, last.getTableAlias(), tableColumnName);
                            Expression joinExpr = new ExpressionColumn(database, joinSchema, join.getTableAlias(), joinColumnName);
                            Expression equal = new Comparison(session,
                                    Comparison.EQUAL, tableExpr, joinExpr);
                            if (on == null) {
                                on = equal;
                            } else {
                                on = new ConditionAndOr(ConditionAndOr.AND, on,
                                        equal);
                            }
                        }
                    }
                }
                if (nestedJoins) {
                    top.addJoin(join, false, nested, on);
                } else {
                    top.addJoin(join, fromOuter, false, on);
                }
                last = join;
            } else {
                break;
            }
        } // while

        if (nested && joined) {
            top = getNested(top);
        }

        return top;
    }

    private TableFilter getNested(TableFilter n) {
        String joinTable = Constants.PREFIX_JOIN + parseIndex;
        TableFilter top = new TableFilter(session, getDualTable(true),
                joinTable, rightsChecked, currentSelect, n.getOrderInFrom(), null);
        top.addJoin(n, false, true, null);
        return top;
    }

    private Prepared parseExecute() {
        ExecuteProcedure command = new ExecuteProcedure(session);
        String procedureName = readAliasIdentifier();
        Procedure p = session.getProcedure(procedureName);
        if (p == null) {
            throw DbException.get(ErrorCode.FUNCTION_ALIAS_NOT_FOUND_1, procedureName);
        }
        command.setProcedure(p);
        if (readIf("(")) {
            for (int i = 0;; i++) {
                command.setExpression(i, readExpression());
                if (readIf(")")) {
                    break;
                }
                read(",");
            }
        }
        return command;
    }

    private DeallocateProcedure parseDeallocate() {
        readIf("PLAN");
        String procedureName = readAliasIdentifier();
        DeallocateProcedure command = new DeallocateProcedure(session);
        command.setProcedureName(procedureName);
        return command;
    }

    private Explain parseExplain() {
        Explain command = new Explain(session);
        if (readIf("ANALYZE")) {
            command.setExecuteCommand(true);
        } else {
            if (readIf("PLAN")) {
                readIf("FOR");
            }
        }
        if (isToken("SELECT") || isToken("FROM") || isToken("(") || isToken("WITH")) {
            Query query = parseSelect();
            query.setNeverLazy(true);
            command.setCommand(query);
        } else if (readIf("DELETE")) {
            command.setCommand(parseDelete());
        } else if (readIf("UPDATE")) {
            command.setCommand(parseUpdate());
        } else if (readIf("INSERT")) {
            command.setCommand(parseInsert());
        } else if (readIf("MERGE")) {
            command.setCommand(parseMerge());
        } else {
            throw getSyntaxError();
        }
        return command;
    }

    public Prepared parseExtract() {
        parseExtractColumns();
        parseExtractFrom();
        parseExtractUsing();
        return null;
    }

    private Command parseExtractColumns() {

        return null;
    }

    private Command parseExtractFrom() {
        return null;
    }

    private Command parseExtractUsing() {
        return null;
    }

    protected Query parseSelect() {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }

        int paramIndex = parameters.size();
        Query command = parseSelectUnion();
        // Query command = parseSelectPipeline();
        ArrayList<Parameter> params = new ArrayList<>();
        for (int i = paramIndex; i < parameters.size(); i++) {
            params.add(parameters.get(i));
        }
        command.setParameterList(params);
        command.init();
        return command;
    }

    private Prepared parseWithStatementOrQuery() {
        int paramIndex = parameters.size();
        Prepared command = parseWith();
        ArrayList<Parameter> params = new ArrayList<>();
        for (int i = paramIndex, size = parameters.size(); i < size; i++) {
            params.add(parameters.get(i));
        }
        command.setParameterList(params);
        if(command instanceof Query){
            Query query = (Query) command;
            query.init();
        }
        return command;
    }

    /**
     * TODO: implement pipelined selection as a single command, so it can be used as an (create table/view) AS clause
     * */
    private SelectPipeline parseSelectPipeline() {
        SelectPipeline pipeline = new SelectPipeline(session);
        pipeline.addSelect(parseSelectUnion());
        while (readIf("=>")) {
            pipeline.addSelect(parseSelectUnion());
        }

        return pipeline;
    }

    private Query parseSelectUnion() {
        int start = lastParseIndex;
        Query command = parseSelectSub();
        return parseSelectUnionExtension(command, start, false);
    }

    private Query parseSelectUnionExtension(Query command, int start, boolean unionOnly) {
        while (true) {
            if (readIf("UNION")) {
                SelectUnion union = new SelectUnion(session, command);
                if (readIf("ALL")) {
                    union.setUnionType(SelectUnion.UNION_ALL);
                } else {
                    readIf("DISTINCT");
                    union.setUnionType(SelectUnion.UNION);
                }
                union.setRight(parseSelectSub());
                command = union;
            } else if (readIf("MINUS") || readIf("EXCEPT")) {
                SelectUnion union = new SelectUnion(session, command);
                union.setUnionType(SelectUnion.EXCEPT);
                union.setRight(parseSelectSub());
                command = union;
            } else if (readIf("INTERSECT")) {
                SelectUnion union = new SelectUnion(session, command);
                union.setUnionType(SelectUnion.INTERSECT);
                union.setRight(parseSelectSub());
                command = union;
            } else {
                break;
            }
        }
        if (!unionOnly) {
            parseEndOfQuery(command);
        }
        setSQL(command, null, start);
        return command;
    }

    private void parseEndOfQuery(Query command) {
        if (readIf("ORDER")) {
            read("BY");
            Select oldSelect = currentSelect;
            if (command instanceof Select) {
                currentSelect = (Select) command;
            }

            ArrayList<SelectOrderBy> orderList = new ArrayList<>();
            do {
                boolean canBeNumber = true;
                if (readIf("=")) {
                    canBeNumber = false;
                }
                SelectOrderBy order = new SelectOrderBy();
                Expression expr = readExpression();
                if (canBeNumber && expr instanceof ValueExpression && expr.getType() == Value.INT) {
                    order.columnIndexExpr = expr;
                } else if (expr instanceof Parameter) {
                    recompileAlways = true;
                    order.columnIndexExpr = expr;
                } else {
                    order.expression = expr;
                }

                if (readIf("DESC")) {
                    order.descending = true;
                } else {
                    readIf("ASC");
                }

                if (readIf("NULLS")) {
                    if (readIf("FIRST")) {
                        order.nullsFirst = true;
                    } else {
                        read("LAST");
                        order.nullsLast = true;
                    }
                }
                orderList.add(order);
            } while (readIf(","));

            command.setOrder(orderList);
            currentSelect = oldSelect;
        }

        if (mode.supportOffsetFetch) {
            // make sure aggregate functions will not work here
            Select temp = currentSelect;
            currentSelect = null;

            // http://sqlpro.developpez.com/SQL2008/
            if (readIf("OFFSET")) {
                command.setOffset(readExpression().optimize(session));
                if (!readIf("ROW")) {
                    readIf("ROWS");
                }
            }

            if (readIf("FETCH")) {
                if (!readIf("FIRST")) {
                    read("NEXT");
                }
                if (readIf("ROW")) {
                    command.setLimit(ValueExpression.get(ValueInt.get(1)));
                } else {
                    Expression limit = readExpression().optimize(session);
                    command.setLimit(limit);
                    if (!readIf("ROW")) {
                        read("ROWS");
                    }
                }
                read("ONLY");
            }

            currentSelect = temp;
        }

        if (readIf("LIMIT")) {
            Select temp = currentSelect;
            // make sure aggregate functions will not work here
            currentSelect = null;
            Expression limit = readExpression().optimize(session);
            command.setLimit(limit);
            if (readIf("OFFSET")) {
                Expression offset = readExpression().optimize(session);
                command.setOffset(offset);
            } else if (readIf(",")) {
                // MySQL: [offset, ] rowcount
                Expression offset = limit;
                limit = readExpression().optimize(session);
                command.setOffset(offset);
                command.setLimit(limit);
            }
            if (readIf("SAMPLE_SIZE")) {
                Expression sampleSize = readExpression().optimize(session);
                command.setSampleSize(sampleSize);
            }
            currentSelect = temp;
        }

        if (readIf("FOR")) {
            if (readIf("UPDATE")) {
                if (readIf("OF")) {
                    do {
                        readIdentifierWithSchema();
                    } while (readIf(","));
                } else if (readIf("NOWAIT")) {
                    // TODO parser: select for update nowait: should not wait
                }
                command.setForUpdate(true);
            } else if (readIf("READ") || readIf("FETCH")) {
                read("ONLY");
            }
        }

        if (mode.isolationLevelInSelectOrInsertStatement) {
            parseIsolationClause();
        }

        if (readIf("EXPECT")) {
            readExpression();
        }
    }

    /**
     * DB2 isolation clause
     */
    private void parseIsolationClause() {
        if (readIf("WITH")) {
            if (readIf("RR") || readIf("RS")) {
                // concurrent-access-resolution clause
                if (readIf("USE")) {
                    read("AND");
                    read("KEEP");
                    if (readIf("SHARE") || readIf("UPDATE") ||
                            readIf("EXCLUSIVE")) {
                        // ignore
                    }
                    read("LOCKS");
                }
            } else if (readIf("CS") || readIf("UR")) {
                // ignore
            }
        }
    }

    private Query parseSelectSub() {
        if (readIf("(")) {
            Query command = parseSelectUnion();
            read(")");
            return command;
        }

        if (readIf("WITH")) {
            Query query;
            try {
                query = (Query) parseWith();
            }
            catch(ClassCastException e) {
                throw DbException.get(ErrorCode.SYNTAX_ERROR_1,
                  "WITH statement supports only SELECT (query) in this context");
            }
            // recursive can not be lazy
            query.setNeverLazy(true);
            return query;
        }

        return parseSelectSimple();
    }

    private void parseSelectSimpleFromPart(Select command) {
        do {
            // debug(2000);

            TableFilter filter = readTableFilter(false);

            // debug(2001);

            parseJoinTableFilter(filter, command);
        } while (readIf(","));

        // Parser can reorder joined table filters, need to explicitly sort them
        // to get the order as it was in the original query.
        if (session.isForceJoinOrder()) {
            sortTableFilters(command.getTopFilters());
        }
    }

    private static void sortTableFilters(ArrayList<TableFilter> filters) {
        if (filters.size() < 2) {
            return;
        }

        // Most probably we are already sorted correctly.
        boolean sorted = true;
        for (int i = 1; i < filters.size(); i++) {
            if (filters.get(i - 1).compareTo(filters.get(i)) > 0) {
                sorted = false;
                break;
            }
        }

        // If not, then sort manually.
        if (!sorted) {
            Collections.sort(filters);
        }
    }

    private void parseJoinTableFilter(TableFilter top, final Select command) {
        top = readJoin(top, command, false, top.isJoinOuter());
        command.addTableFilter(top, true);
        boolean isOuter = false;
        while (true) {
            TableFilter n = top.getNestedJoin();
            if (n != null) {
                n.visit(new TableFilterVisitor() {
                    @Override
                    public void accept(TableFilter f) {
                        command.addTableFilter(f, false);
                    }
                });
            }
            TableFilter join = top.getJoin();
            if (join == null) {
                break;
            }
            isOuter = isOuter | join.isJoinOuter();
            if (isOuter) {
                command.addTableFilter(join, false);
            } else {
                // make flat so the optimizer can work better
                Expression on = join.getJoinCondition();
                if (on != null) {
                    command.addCondition(on);
                }
                join.removeJoinCondition();
                top.removeJoin();
                command.addTableFilter(join, true);
            }
            top = join;
        }
    }

    private void parseSelectSimpleSelectPart(Select command) {
        Objects.requireNonNull(currentSelect);

        // System.out.println("==Parser.parseSelectSimpleSelectPart==" + getUnparsedSql());

        currentIsExternal = readIf("EXTERNAL");

        Select temp = currentSelect;
        // make sure aggregate functions will not work in TOP and LIMIT
        currentSelect = null;

        if (readIf("TOP")) {
            // can't read more complex expressions here because
            // SELECT TOP 1 +? A FROM TEST could mean
            // SELECT TOP (1+?) A FROM TEST or
            // SELECT TOP 1 (+?) AS A FROM TEST
            Expression limit = readTerm().optimize(session);
            command.setLimit(limit);
        } else if (readIf("LIMIT")) {
            Expression offset = readTerm().optimize(session);
            command.setOffset(offset);
            Expression limit = readTerm().optimize(session);
            command.setLimit(limit);
        }

        currentSelect = temp;
        if (readIf("DISTINCT")) {
            command.setDistinct(true);
        } else {
            readIf("ALL");
        }

        ArrayList<Expression> expressions = new ArrayList<>();
        do {
            if ("FROM".equalsIgnoreCase(currentToken)) {
                throw getSyntaxError();
            }

            if (readIf("*")) {
                Wildcard wildcard = new Wildcard(null, null);
                expressions.add(wildcard);
            } else {
                Expression expr = readExpression();

                if (readIf("AS") || currentTokenType == IDENTIFIER) {
                    String alias = readAliasIdentifier();
                    boolean aliasColumnName = database.getSettings().aliasColumnName;
                    aliasColumnName |= mode.aliasColumnName;
                    expr = new Alias(expr, alias, aliasColumnName);
                }

                expressions.add(expr);
            }
        } while (readIf(","));

        for (int i = 0; i < expressions.size(); i++) {
            expressions.get(i).setSelectItemSequence(1 + i);
        }

        command.setExpressions(expressions);
    }

    private Select parseSelectSimple() {
        boolean fromFirst;
        if (readIf("SELECT")) {
            fromFirst = false;
        } else if (readIf("FROM")) {
            fromFirst = true;
        } else {
            throw getSyntaxError();
        }

        Select command = new Select(selectSequence++, session);
        int start = lastParseIndex;
        Select oldSelect = currentSelect;
        currentSelect = command;
        currentPrepared = command;
        currentIsExternal = false;
        // TODO: there is might be bugs in complex select
        currentSelectIndex++;
        if (indexedSelectList == null) {
            indexedSelectList = new ArrayList<>();
        }
        indexedSelectList.add(command);

        if (fromFirst) {
            // debug(200);
            parseSelectSimpleFromPart(command);
            // debug(201);
            read("SELECT");
            parseSelectSimpleSelectPart(command);
        } else {
            parseSelectSimpleSelectPart(command);
            if (!readIf("FROM")) {
                // select without FROM: convert to SELECT ... FROM
                // SYSTEM_RANGE(1,1)
                Table dual = getDualTable(false);
                TableFilter filter = new TableFilter(
                  session, dual, null, rightsChecked, currentSelect, 0, null);
                command.addTableFilter(filter, true);
            } else {
                parseSelectSimpleFromPart(command);
            }
        }

        if (readIf("WHERE")) {
            // debug(210);
            Expression condition = readExpression();
            command.addCondition(condition);
        }

        // the group by is read for the outer select (or not a select)
        // so that columns that are not grouped can be used
        currentSelect = oldSelect;
        if (readIf("GROUP")) {
            // debug(220);
            read("BY");
            command.setGroupQuery();
            ArrayList<Expression> list = new ArrayList<>();
            do {
                Expression expr = readExpression();
                list.add(expr);
            } while (readIf(","));
            command.setGroupBy(list);
        }

        currentSelect = command;
        if (readIf("HAVING")) {
            // debug(230);
            command.setGroupQuery();
            Expression condition = readExpression();
            command.setHaving(condition);
        }

        command.setParameterList(parameters);
        currentSelect = oldSelect;

        setSQL(command, "SELECT", start);
        return command;
    }

    private Table getDualTable(boolean noColumns) {
        Schema main = database.findSchema(Constants.SCHEMA_MAIN);
        Expression one = ValueExpression.get(ValueLong.get(1));
        return new RangeTable(main, one, one, noColumns);
    }

    private void setSQL(Prepared command, String start, int startIndex) {
        String sql = originalSQL.substring(startIndex, lastParseIndex).trim();
        if (start != null) {
            sql = start + " " + sql;
        }
        command.setSQL(sql);
    }

    private Column parseColumnForTable(String columnName, boolean defaultNullable) {
        Column column;
        boolean isIdentity = readIf("IDENTITY");
        if (isIdentity || readIf("BIGSERIAL")) {
            // Check if any of them are disallowed in the current Mode
            if (isIdentity && mode.
                    disallowedTypes.contains("IDENTITY")) {
                throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1,
                        currentToken);
            }
            column = new Column(columnName, Value.LONG);
            column.setOriginalSQL("IDENTITY");
            parseAutoIncrement(column);
            // PostgreSQL compatibility
            if (!mode.serialColumnIsNotPK) {
                column.setPrimaryKey(true);
            }
        } else if (readIf("SERIAL")) {
            column = new Column(columnName, Value.INT);
            column.setOriginalSQL("SERIAL");
            parseAutoIncrement(column);
            // PostgreSQL compatibility
            if (!mode.serialColumnIsNotPK) {
                column.setPrimaryKey(true);
            }
        } else {
            column = parseColumnWithType(columnName);
        }
        if (readIf("INVISIBLE")) {
            column.setVisible(false);
        } else if (readIf("VISIBLE")) {
            column.setVisible(true);
        }
        if (readIf("NOT")) {
            read("NULL");
            column.setNullable(false);
        } else if (readIf("NULL")) {
            column.setNullable(true);
        } else {
            // domains may be defined as not nullable
            column.setNullable(defaultNullable & column.isNullable());
        }
        if (readIf("AS")) {
            if (isIdentity) {
                getSyntaxError();
            }
            Expression expr = readExpression();
            column.setComputedExpression(expr);
        } else if (readIf("DEFAULT")) {
            Expression defaultExpression = readExpression();
            column.setDefaultExpression(session, defaultExpression);
        } else if (readIf("GENERATED")) {
            if (!readIf("ALWAYS")) {
                read("BY");
                read("DEFAULT");
            }
            read("AS");
            read("IDENTITY");
            long start = 1, increment = 1;
            if (readIf("(")) {
                read("START");
                readIf("WITH");
                start = readLong();
                readIf(",");
                if (readIf("INCREMENT")) {
                    readIf("BY");
                    increment = readLong();
                }
                read(")");
            }
            column.setPrimaryKey(true);
            column.setAutoIncrement(true, start, increment);
        }
        if (readIf("NOT")) {
            read("NULL");
            column.setNullable(false);
        } else {
            readIf("NULL");
        }
        if (readIf("AUTO_INCREMENT") || readIf("BIGSERIAL") || readIf("SERIAL")) {
            parseAutoIncrement(column);
            if (readIf("NOT")) {
                read("NULL");
            }
        } else if (readIf("IDENTITY")) {
            parseAutoIncrement(column);
            column.setPrimaryKey(true);
            if (readIf("NOT")) {
                read("NULL");
            }
        }
        if (readIf("NULL_TO_DEFAULT")) {
            column.setConvertNullToDefault(true);
        }
        if (readIf("SEQUENCE")) {
            Sequence sequence = readSequence();
            column.setSequence(sequence);
        }
        if (readIf("SELECTIVITY")) {
            int value = readPositiveInt();
            column.setSelectivity(value);
        }
        String comment = readCommentIf();
        if (comment != null) {
            column.setComment(comment);
        }
        return column;
    }

    private void parseAutoIncrement(Column column) {
        long start = 1, increment = 1;
        if (readIf("(")) {
            start = readLong();
            if (readIf(",")) {
                increment = readLong();
            }
            read(")");
        }
        column.setAutoIncrement(true, start, increment);
    }

    private Prepared parseCreate() {
        boolean orReplace = false;
        if (readIf("OR")) {
            read("REPLACE");
            orReplace = true;
        }
        boolean force = readIf("FORCE");
        if (readIf("VIEW")) {
            return parseCreateView(force, orReplace);
        } else if (readIf("ALIAS")) {
            return parseCreateFunctionAlias(force);
        } else if (readIf("SEQUENCE")) {
            return parseCreateSequence();
        } else if (readIf("USER")) {
            return parseCreateUser();
        } else if (readIf("TRIGGER")) {
            return parseCreateTrigger(force);
        } else if (readIf("ROLE")) {
            return parseCreateRole();
        } else if (readIf("SCHEMA")) {
            return parseCreateSchema();
        } else if (readIf("CONSTANT")) {
            return parseCreateConstant();
        } else if (readIf("DOMAIN")) {
            return parseCreateUserDataType();
        } else if (readIf("TYPE")) {
            return parseCreateUserDataType();
        } else if (readIf("DATATYPE")) {
            return parseCreateUserDataType();
        } else if (readIf("AGGREGATE")) {
            return parseCreateAggregate(force);
        } else if (readIf("LINKED")) {
            return parseCreateLinkedTable(false, false, force);
        }
        // tables or linked tables
        boolean memory = false, cached = false;
        if (readIf("MEMORY")) {
            memory = true;
        } else if (readIf("CACHED")) {
            cached = true;
        }
        if (readIf("LOCAL")) {
            read("TEMPORARY");
            if (readIf("LINKED")) {
                return parseCreateLinkedTable(true, false, force);
            }
            read("TABLE");
            return parseCreateTable(true, false, cached);
        } else if (readIf("GLOBAL")) {
            read("TEMPORARY");
            if (readIf("LINKED")) {
                return parseCreateLinkedTable(true, true, force);
            }
            read("TABLE");
            return parseCreateTable(true, true, cached);
        } else if (readIf("TEMP") || readIf("TEMPORARY")) {
            if (readIf("LINKED")) {
                return parseCreateLinkedTable(true, true, force);
            }
            read("TABLE");
            return parseCreateTable(true, true, cached);
        } else if (readIf("TABLE")) {
            if (!cached && !memory) {
                cached = database.getDefaultTableType() == Table.TYPE_CACHED;
            }
            return parseCreateTable(false, false, cached);
        } else if (readIf("SYNONYM")) {
            return parseCreateSynonym(orReplace);
        } else {
            boolean hash = false, primaryKey = false;
            boolean unique = false, spatial = false;
            String indexName = null;
            Schema oldSchema = null;
            boolean ifNotExists = false;
            if (readIf("PRIMARY")) {
                read("KEY");
                if (readIf("HASH")) {
                    hash = true;
                }
                primaryKey = true;
                if (!isToken("ON")) {
                    ifNotExists = readIfNotExists();
                    indexName = readIdentifierWithSchema(null);
                    oldSchema = getSchema();
                }
            } else {
                if (readIf("UNIQUE")) {
                    unique = true;
                }
                if (readIf("HASH")) {
                    hash = true;
                }
                if (readIf("SPATIAL")) {
                    spatial = true;
                }
                if (readIf("INDEX")) {
                    if (!isToken("ON")) {
                        ifNotExists = readIfNotExists();
                        indexName = readIdentifierWithSchema(null);
                        oldSchema = getSchema();
                    }
                } else {
                    throw getSyntaxError();
                }
            }
            read("ON");
            String tableName = readIdentifierWithSchema();
            checkSchema(oldSchema);
            CreateIndex command = new CreateIndex(session, getSchema());
            command.setIfNotExists(ifNotExists);
            command.setPrimaryKey(primaryKey);
            command.setTableName(tableName);
            command.setUnique(unique);
            command.setIndexName(indexName);
            command.setComment(readCommentIf());
            read("(");
            command.setIndexColumns(parseIndexColumnList());

            if (readIf("USING")) {
                if (hash) {
                    throw getSyntaxError();
                }
                if (spatial) {
                    throw getSyntaxError();
                }
                if (readIf("BTREE")) {
                    // default
                } else if (readIf("RTREE")) {
                    spatial = true;
                } else if (readIf("HASH")) {
                    hash = true;
                } else {
                    throw getSyntaxError();
                }

            }
            command.setHash(hash);
            command.setSpatial(spatial);
            return command;
        }
    }

    /**
     * @return true if we expect to see a TABLE clause
     */
    private boolean addRoleOrRight(GrantRevoke command) {
        if (readIf("SELECT")) {
            command.addRight(Right.SELECT);
            return true;
        } else if (readIf("DELETE")) {
            command.addRight(Right.DELETE);
            return true;
        } else if (readIf("INSERT")) {
            command.addRight(Right.INSERT);
            return true;
        } else if (readIf("UPDATE")) {
            command.addRight(Right.UPDATE);
            return true;
        } else if (readIf("ALL")) {
            command.addRight(Right.ALL);
            return true;
        } else if (readIf("ALTER")) {
            read("ANY");
            read("SCHEMA");
            command.addRight(Right.ALTER_ANY_SCHEMA);
            command.addTable(null);
            return false;
        } else if (readIf("CONNECT")) {
            // ignore this right
            return true;
        } else if (readIf("RESOURCE")) {
            // ignore this right
            return true;
        } else {
            command.addRoleName(readUniqueIdentifier());
            return false;
        }
    }

    private GrantRevoke parseGrantRevoke(int operationType) {
        GrantRevoke command = new GrantRevoke(session);
        command.setOperationType(operationType);
        boolean tableClauseExpected = addRoleOrRight(command);
        while (readIf(",")) {
            addRoleOrRight(command);
            if (command.isRightMode() && command.isRoleMode()) {
                throw DbException.get(ErrorCode.ROLES_AND_RIGHT_CANNOT_BE_MIXED);
            }
        }
        if (tableClauseExpected) {
            if (readIf("ON")) {
                if (readIf("SCHEMA")) {
                    Schema schema = database.getSchema(readAliasIdentifier());
                    command.setSchema(schema);
                } else {
                    do {
                        Table table = readTableOrView();
                        command.addTable(table);
                    } while (readIf(","));
                }
            }
        }
        if (operationType == CommandInterface.GRANT) {
            read("TO");
        } else {
            read("FROM");
        }
        command.setGranteeName(readUniqueIdentifier());
        return command;
    }

    /**
     * For just one situation:
     * values(1, 'hello'), (2, 'world');
     * */
    private Select parseValues() {
        Select command = new Select(session);
        currentSelect = command;
        TableFilter filter = parseValuesTable(0);
        ArrayList<Expression> list = new ArrayList<>();
        list.add(new Wildcard(null, null));
        command.setExpressions(list);
        command.addTableFilter(filter, true);
        command.init();
        return command;
    }

    private TableFilter parseValuesTable(int orderInFrom) {
        Schema mainSchema = database.getSchema(Constants.SCHEMA_MAIN);
        TableFunction tf = (TableFunction) Function.getFunction(database, "TABLE");
        Objects.requireNonNull(tf);
        ArrayList<Column> columns = new ArrayList<>();
        ArrayList<ArrayList<Expression>> rows = new ArrayList<>();
        do {
            int i = 0;
            ArrayList<Expression> row = new ArrayList<>();
            boolean multiColumn = readIf("(");
            do {
                Expression expr = readExpression();
                expr = expr.optimize(session);
                int type = expr.getType();
                long prec;
                int scale, displaySize;
                Column column;
                String columnName = "C" + (i + 1);
                if (rows.size() == 0) {
                    if (type == Value.UNKNOWN) {
                        type = Value.STRING;
                    }
                    DataType dt = DataType.getDataType(type);
                    prec = dt.defaultPrecision;
                    scale = dt.defaultScale;
                    displaySize = dt.defaultDisplaySize;
                    column = new Column(columnName, type, prec, scale, displaySize);
                    columns.add(column);
                }
                prec = expr.getPrecision();
                scale = expr.getScale();
                displaySize = expr.getDisplaySize();
                if (i >= columns.size()) {
                    throw DbException.get(ErrorCode.COLUMN_COUNT_DOES_NOT_MATCH);
                }
                Column c = columns.get(i);
                type = Value.getHigherOrder(c.getType(), type);
                prec = Math.max(c.getPrecision(), prec);
                scale = Math.max(c.getScale(), scale);
                displaySize = Math.max(c.getDisplaySize(), displaySize);
                column = new Column(columnName, type, prec, scale, displaySize);
                columns.set(i, column);
                row.add(expr);
                i++;
            } while (multiColumn && readIf(","));
            if (multiColumn) {
                read(")");
            }
            rows.add(row);
        } while (readIf(","));

        int columnCount = columns.size();
        int rowCount = rows.size();
        for (ArrayList<Expression> row : rows) {
            if (row.size() != columnCount) {
                throw DbException.get(ErrorCode.COLUMN_COUNT_DOES_NOT_MATCH);
            }
        }
        for (int i = 0; i < columnCount; i++) {
            Column c = columns.get(i);
            if (c.getType() == Value.UNKNOWN) {
                c = new Column(c.getName(), Value.STRING, 0, 0, 0);
                columns.set(i, c);
            }
            Expression[] array = new Expression[rowCount];
            for (int j = 0; j < rowCount; j++) {
                array[j] = rows.get(j).get(i);
            }
            ExpressionList list = new ExpressionList(array);
            tf.setParameter(i, list);
        }
        tf.setColumns(columns);
        tf.doneWithParameters();
        Table table = new FunctionTable(mainSchema, session, tf, tf);
        TableFilter filter = new TableFilter(session, table, null,
                rightsChecked, currentSelect, orderInFrom,
                null);
        return filter;
    }

    private Call parseCall() {
        Call command = new Call(session);
        currentPrepared = command;
        command.setExpression(readExpression());
        return command;
    }

    private CreateRole parseCreateRole() {
        CreateRole command = new CreateRole(session);
        command.setIfNotExists(readIfNotExists());
        command.setRoleName(readUniqueIdentifier());
        return command;
    }

    private CreateSchema parseCreateSchema() {
        CreateSchema command = new CreateSchema(session);
        command.setIfNotExists(readIfNotExists());
        command.setSchemaName(readUniqueIdentifier());
        if (readIf("AUTHORIZATION")) {
            command.setAuthorization(readUniqueIdentifier());
        } else {
            command.setAuthorization(session.getUser().getName());
        }
        if (readIf("WITH")) {
            command.setTableEngineParams(readTableEngineParams());
        }
        return command;
    }

    private ArrayList<String> readTableEngineParams() {
        ArrayList<String> tableEngineParams = new ArrayList<>();
        do {
            tableEngineParams.add(readUniqueIdentifier());
        } while (readIf(","));
        return tableEngineParams;
    }

    private CreateSequence parseCreateSequence() {
        boolean ifNotExists = readIfNotExists();
        String sequenceName = readIdentifierWithSchema();
        CreateSequence command = new CreateSequence(session, getSchema());
        command.setIfNotExists(ifNotExists);
        command.setSequenceName(sequenceName);
        while (true) {
            if (readIf("START")) {
                readIf("WITH");
                command.setStartWith(readExpression());
            } else if (readIf("INCREMENT")) {
                readIf("BY");
                command.setIncrement(readExpression());
            } else if (readIf("MINVALUE")) {
                command.setMinValue(readExpression());
            } else if (readIf("NOMINVALUE")) {
                command.setMinValue(null);
            } else if (readIf("MAXVALUE")) {
                command.setMaxValue(readExpression());
            } else if (readIf("NOMAXVALUE")) {
                command.setMaxValue(null);
            } else if (readIf("CYCLE")) {
                command.setCycle(true);
            } else if (readIf("NOCYCLE")) {
                command.setCycle(false);
            } else if (readIf("NO")) {
                if (readIf("MINVALUE")) {
                    command.setMinValue(null);
                } else if (readIf("MAXVALUE")) {
                    command.setMaxValue(null);
                } else if (readIf("CYCLE")) {
                    command.setCycle(false);
                } else if (readIf("CACHE")) {
                    command.setCacheSize(ValueExpression.get(ValueLong.get(1)));
                } else {
                    break;
                }
            } else if (readIf("CACHE")) {
                command.setCacheSize(readExpression());
            } else if (readIf("NOCACHE")) {
                command.setCacheSize(ValueExpression.get(ValueLong.get(1)));
            } else if (readIf("BELONGS_TO_TABLE")) {
                command.setBelongsToTable(true);
            } else if (readIf("ORDER")) {
                // Oracle compatibility
            } else {
                break;
            }
        }
        return command;
    }

    private boolean readIfNotExists() {
        if (readIf("IF")) {
            read("NOT");
            read("EXISTS");
            return true;
        }
        return false;
    }

    private boolean readIfAffinity() {
        return readIf("AFFINITY") || readIf("SHARD");
    }

    private CreateConstant parseCreateConstant() {
        boolean ifNotExists = readIfNotExists();
        String constantName = readIdentifierWithSchema();
        Schema schema = getSchema();
        if (isKeyword(constantName)) {
            throw DbException.get(ErrorCode.CONSTANT_ALREADY_EXISTS_1,
                    constantName);
        }
        read("VALUE");
        Expression expr = readExpression();
        CreateConstant command = new CreateConstant(session, schema);
        command.setConstantName(constantName);
        command.setExpression(expr);
        command.setIfNotExists(ifNotExists);
        return command;
    }

    private CreateAggregate parseCreateAggregate(boolean force) {
        boolean ifNotExists = readIfNotExists();
        CreateAggregate command = new CreateAggregate(session);
        command.setForce(force);
        String name = readIdentifierWithSchema();
        if (isKeyword(name) || Function.getFunction(database, name) != null ||
                getAggregateType(name) >= 0) {
            throw DbException.get(ErrorCode.FUNCTION_ALIAS_ALREADY_EXISTS_1,
                    name);
        }
        command.setName(name);
        command.setSchema(getSchema());
        command.setIfNotExists(ifNotExists);
        read("FOR");
        command.setJavaClassMethod(readUniqueIdentifier());
        return command;
    }

    private CreateUserDataType parseCreateUserDataType() {
        boolean ifNotExists = readIfNotExists();
        CreateUserDataType command = new CreateUserDataType(session);
        command.setTypeName(readUniqueIdentifier());
        read("AS");
        Column col = parseColumnForTable("VALUE", true);
        if (readIf("CHECK")) {
            Expression expr = readExpression();
            col.addCheckConstraint(session, expr);
        }
        col.rename(null);
        command.setColumn(col);
        command.setIfNotExists(ifNotExists);
        return command;
    }

    private CreateTrigger parseCreateTrigger(boolean force) {
        boolean ifNotExists = readIfNotExists();
        String triggerName = readIdentifierWithSchema(null);
        Schema schema = getSchema();
        boolean insteadOf, isBefore;
        if (readIf("INSTEAD")) {
            read("OF");
            isBefore = true;
            insteadOf = true;
        } else if (readIf("BEFORE")) {
            insteadOf = false;
            isBefore = true;
        } else {
            read("AFTER");
            insteadOf = false;
            isBefore = false;
        }
        int typeMask = 0;
        boolean onRollback = false;
        do {
            if (readIf("INSERT")) {
                typeMask |= Trigger.INSERT;
            } else if (readIf("UPDATE")) {
                typeMask |= Trigger.UPDATE;
            } else if (readIf("DELETE")) {
                typeMask |= Trigger.DELETE;
            } else if (readIf("SELECT")) {
                typeMask |= Trigger.SELECT;
            } else if (readIf("ROLLBACK")) {
                onRollback = true;
            } else {
                throw getSyntaxError();
            }
        } while (readIf(","));
        read("ON");
        String tableName = readIdentifierWithSchema();
        checkSchema(schema);
        CreateTrigger command = new CreateTrigger(session, getSchema());
        command.setForce(force);
        command.setTriggerName(triggerName);
        command.setIfNotExists(ifNotExists);
        command.setInsteadOf(insteadOf);
        command.setBefore(isBefore);
        command.setOnRollback(onRollback);
        command.setTypeMask(typeMask);
        command.setTableName(tableName);
        if (readIf("FOR")) {
            read("EACH");
            read("ROW");
            command.setRowBased(true);
        } else {
            command.setRowBased(false);
        }
        if (readIf("QUEUE")) {
            command.setQueueSize(readPositiveInt());
        }
        command.setNoWait(readIf("NOWAIT"));
        if (readIf("AS")) {
            command.setTriggerSource(readString());
        } else {
            read("CALL");
            command.setTriggerClassName(readUniqueIdentifier());
        }
        return command;
    }

    private CreateUser parseCreateUser() {
        CreateUser command = new CreateUser(session);
        command.setIfNotExists(readIfNotExists());
        command.setUserName(readUniqueIdentifier());
        command.setComment(readCommentIf());
        if (readIf("PASSWORD")) {
            command.setPassword(readExpression());
        } else if (readIf("SALT")) {
            command.setSalt(readExpression());
            read("HASH");
            command.setHash(readExpression());
        } else if (readIf("IDENTIFIED")) {
            read("BY");
            // uppercase if not quoted
            command.setPassword(ValueExpression.get(ValueString.get(readColumnIdentifier())));
        } else {
            throw getSyntaxError();
        }
        if (readIf("ADMIN")) {
            command.setAdmin(true);
        }
        return command;
    }

    private CreateFunctionAlias parseCreateFunctionAlias(boolean force) {
        boolean ifNotExists = readIfNotExists();
        final boolean newAliasSameNameAsBuiltin = Function.getFunction(database, currentToken) != null;
        String aliasName;
        if (database.isAllowBuiltinAliasOverride() && newAliasSameNameAsBuiltin) {
            aliasName = currentToken;
            schemaName = session.getCurrentSchemaName();
            readToken();
        } else {
            aliasName = readIdentifierWithSchema();
        }
        if (database.isAllowBuiltinAliasOverride() && newAliasSameNameAsBuiltin) {
            // fine
        } else if (isKeyword(aliasName) || Function.getFunction(database, aliasName) != null
          || getAggregateType(aliasName) >= 0) {
            throw DbException.get(ErrorCode.FUNCTION_ALIAS_ALREADY_EXISTS_1, aliasName);
        }
        CreateFunctionAlias command = new CreateFunctionAlias(session, getSchema());
        command.setForce(force);
        command.setAliasName(aliasName);
        command.setIfNotExists(ifNotExists);
        command.setDeterministic(readIf("DETERMINISTIC"));
        command.setBufferResultSetToLocalTemp(!readIf("NOBUFFER"));
        if (readIf("AS")) {
            command.setSource(readString());
        } else {
            read("FOR");
            command.setJavaClassMethod(readUniqueIdentifier());
        }
        return command;
    }

    private Prepared parseWith() {
        List<TableView> viewsCreated = new ArrayList<>();
        readIf("RECURSIVE");
        do {
            viewsCreated.add(parseSingleCommonTableExpression());
        } while (readIf(","));

        Prepared p = null;

        if(isToken("SELECT")) {
            Query query = parseSelectUnion();
            query.setPrepareAlways(true);
            query.setNeverLazy(true);
            p = query;
        }
        else if(readIf("INSERT")) {
            p = parseInsert();
            p.setPrepareAlways(true);
        }
        else if(readIf("UPDATE")) {
            p = parseUpdate();
            p.setPrepareAlways(true);
        }
        else if(readIf("MERGE")) {
            p = parseMerge();
            p.setPrepareAlways(true);
        }
        else if(readIf("DELETE")) {
            p = parseDelete();
            p.setPrepareAlways(true);
        }
        else if(readIf("CREATE")) {
            if (!isToken("TABLE")){
                throw DbException.get(ErrorCode.SYNTAX_ERROR_1,
                        WITH_STATEMENT_SUPPORTS_LIMITED_STATEMENTS);

            }
            p = parseCreate();
            p.setPrepareAlways(true);
        }
        else {
            throw DbException.get(ErrorCode.SYNTAX_ERROR_1,
                    WITH_STATEMENT_SUPPORTS_LIMITED_STATEMENTS);
        }

        // clean up temp views starting with last to first (in case of
        // dependencies)
        Collections.reverse(viewsCreated);
        p.setCteCleanups(viewsCreated);
        return p;
    }

    private TableView parseSingleCommonTableExpression() {
        String tempViewName = readIdentifierWithSchema();
        Schema schema = getSchema();
        Table recursiveTable;
        ArrayList<Column> columns = new ArrayList<>();
        String[] cols = null;

        // column names are now optional - they can be inferred from the named
        // query if not supplied
        if (readIf("(")) {
            cols = parseColumnList();
            for (String c : cols) {
                // we dont really know the type of the column, so string will
                // have to do

                // if is WebRegularTable, add a function: extract() as a new column
                // and for all columns, add an index and create a schema automatically
                columns.add(new Column(c, Value.STRING));
            }
        }
        Table old = session.findLocalTempTable(tempViewName);
        if (old != null) {
            if (!(old instanceof TableView)) {
                throw DbException.get(ErrorCode.TABLE_OR_VIEW_ALREADY_EXISTS_1, tempViewName);
            }
            TableView tv = (TableView) old;
            if (!tv.isTableExpression()) {
                throw DbException.get(ErrorCode.TABLE_OR_VIEW_ALREADY_EXISTS_1, tempViewName);
            }
            session.removeLocalTempTable(old);
        }
        // this table is created as a work around because recursive
        // table expressions need to reference something that look like
        // themselves to work (its removed after creation in this method)
        CreateTableData data = new CreateTableData();
        data.id = database.allocateObjectId();
        data.columns = columns;
        data.tableName = tempViewName;
        data.temporary = true;
        data.persistData = true;
        data.persistIndexes = false;
        data.create = true;
        data.session = session;
        recursiveTable = schema.createTable(data);
        session.addLocalTempTable(recursiveTable);
        String querySQL;
        List<Column> columnTemplateList = new ArrayList<Column>();
        try {
            read("AS");
            read("(");
            Query withQuery = parseSelect();
            read(")");
            withQuery.prepare();
            querySQL = StringUtils.cache(withQuery.getPlanSQL());
            ArrayList<Expression> withExpressions = withQuery.getExpressions();
            for (int i = 0; i < withExpressions.size(); ++i) {
                String columnName = cols != null ? cols[i] : withExpressions.get(i).getColumnName();
                columnTemplateList.add(new Column(columnName, withExpressions.get(i).getType()));
            }
        } finally {
            session.removeLocalTempTable(recursiveTable);
        }
        int id = database.allocateObjectId();
        // No easy way to determine if this is a recursive query up front, so we just compile
        // it twice - once without the flag set, and if we didn't see a recursive term,
        // then we just compile it again.
        TableView view = new TableView(schema, id, tempViewName, querySQL,
                parameters, columnTemplateList.toArray(new Column[0]), session,
                true/* recursive */, false);
        if (!view.isRecursiveQueryDetected()) {
            view = new TableView(schema, id, tempViewName, querySQL, parameters,
                    columnTemplateList.toArray(new Column[0]), session,
                    false/* recursive */, false);
        }
        view.setTableExpression(true);
        view.setTemporary(true);
        session.addLocalTempTable(view);
        view.setOnCommitDrop(true);

        return view;
    }

    private CreateView parseCreateView(boolean force, boolean orReplace) {
        boolean ifNotExists = readIfNotExists();
        String viewName = readIdentifierWithSchema();
        CreateView command = new CreateView(session, getSchema());
        this.createView = command;
        command.setViewName(viewName);
        command.setIfNotExists(ifNotExists);
        command.setComment(readCommentIf());
        command.setOrReplace(orReplace);
        command.setForce(force);
        if (readIf("(")) {
            String[] cols = parseColumnList();
            command.setColumnNames(cols);
        }
        String select = StringUtils.cache(cleanSql
                .substring(parseIndex));
        read("AS");
        try {
            Query query;
            session.setParsingView(true);
            try {
                query = parseSelect();
                query.prepare();
            } finally {
                session.setParsingView(false);
            }
            command.setSelect(query);
        } catch (DbException e) {
            if (force) {
                command.setSelectSQL(select);
                while (currentTokenType != END) {
                    readToken();
                }
            } else {
                throw e;
            }
        }
        return command;
    }

    private TransactionCommand parseCheckpoint() {
        TransactionCommand command;
        if (readIf("SYNC")) {
            command = new TransactionCommand(session,
                    CommandInterface.CHECKPOINT_SYNC);
        } else {
            command = new TransactionCommand(session,
                    CommandInterface.CHECKPOINT);
        }
        return command;
    }

    private Prepared parseAlter() {
        if (readIf("TABLE")) {
            return parseAlterTable();
        } else if (readIf("USER")) {
            return parseAlterUser();
        } else if (readIf("INDEX")) {
            return parseAlterIndex();
        } else if (readIf("SCHEMA")) {
            return parseAlterSchema();
        } else if (readIf("SEQUENCE")) {
            return parseAlterSequence();
        } else if (readIf("VIEW")) {
            return parseAlterView();
        }
        throw getSyntaxError();
    }

    private void checkSchema(Schema old) {
        if (old != null && getSchema() != old) {
            throw DbException.get(ErrorCode.SCHEMA_NAME_MUST_MATCH);
        }
    }

    private AlterIndexRename parseAlterIndex() {
        boolean ifExists = readIfExists(false);
        String indexName = readIdentifierWithSchema();
        Schema old = getSchema();
        AlterIndexRename command = new AlterIndexRename(session);
        command.setOldSchema(old);
        command.setOldName(indexName);
        command.setIfExists(ifExists);
        read("RENAME");
        read("TO");
        String newName = readIdentifierWithSchema(old.getName());
        checkSchema(old);
        command.setNewName(newName);
        return command;
    }

    private AlterView parseAlterView() {
        AlterView command = new AlterView(session);
        boolean ifExists = readIfExists(false);
        command.setIfExists(ifExists);
        String viewName = readIdentifierWithSchema();
        Table tableView = getSchema().findTableOrView(session, viewName);
        if (!(tableView instanceof TableView) && !ifExists) {
            throw DbException.get(ErrorCode.VIEW_NOT_FOUND_1, viewName);
        }
        TableView view = (TableView) tableView;
        command.setView(view);
        read("RECOMPILE");
        return command;
    }

    private Prepared parseAlterSchema() {
        boolean ifExists = readIfExists(false);
        String schemaName = readIdentifierWithSchema();
        Schema old = getSchema();
        read("RENAME");
        read("TO");
        String newName = readIdentifierWithSchema(old.getName());
        Schema schema = findSchema(schemaName);
        if (schema == null) {
            if (ifExists) {
                return new NoOperation(session);
            }
            throw DbException.get(ErrorCode.SCHEMA_NOT_FOUND_1, schemaName);
        }
        AlterSchemaRename command = new AlterSchemaRename(session);
        command.setOldSchema(schema);
        checkSchema(old);
        command.setNewName(newName);
        return command;
    }

    private AlterSequence parseAlterSequence() {
        boolean ifExists = readIfExists(false);
        String sequenceName = readIdentifierWithSchema();
        AlterSequence command = new AlterSequence(session, getSchema());
        command.setSequenceName(sequenceName);
        command.setIfExists(ifExists);
        while (true) {
            if (readIf("RESTART")) {
                read("WITH");
                command.setStartWith(readExpression());
            } else if (readIf("INCREMENT")) {
                read("BY");
                command.setIncrement(readExpression());
            } else if (readIf("MINVALUE")) {
                command.setMinValue(readExpression());
            } else if (readIf("NOMINVALUE")) {
                command.setMinValue(null);
            } else if (readIf("MAXVALUE")) {
                command.setMaxValue(readExpression());
            } else if (readIf("NOMAXVALUE")) {
                command.setMaxValue(null);
            } else if (readIf("CYCLE")) {
                command.setCycle(true);
            } else if (readIf("NOCYCLE")) {
                command.setCycle(false);
            } else if (readIf("NO")) {
                if (readIf("MINVALUE")) {
                    command.setMinValue(null);
                } else if (readIf("MAXVALUE")) {
                    command.setMaxValue(null);
                } else if (readIf("CYCLE")) {
                    command.setCycle(false);
                } else if (readIf("CACHE")) {
                    command.setCacheSize(ValueExpression.get(ValueLong.get(1)));
                } else {
                    break;
                }
            } else if (readIf("CACHE")) {
                command.setCacheSize(readExpression());
            } else if (readIf("NOCACHE")) {
                command.setCacheSize(ValueExpression.get(ValueLong.get(1)));
            } else {
                break;
            }
        }
        return command;
    }

    private AlterUser parseAlterUser() {
        String userName = readUniqueIdentifier();
        if (readIf("SET")) {
            AlterUser command = new AlterUser(session);
            command.setType(CommandInterface.ALTER_USER_SET_PASSWORD);
            command.setUser(database.getUser(userName));
            if (readIf("PASSWORD")) {
                command.setPassword(readExpression());
            } else if (readIf("SALT")) {
                command.setSalt(readExpression());
                read("HASH");
                command.setHash(readExpression());
            } else {
                throw getSyntaxError();
            }
            return command;
        } else if (readIf("RENAME")) {
            read("TO");
            AlterUser command = new AlterUser(session);
            command.setType(CommandInterface.ALTER_USER_RENAME);
            command.setUser(database.getUser(userName));
            String newName = readUniqueIdentifier();
            command.setNewName(newName);
            return command;
        } else if (readIf("ADMIN")) {
            AlterUser command = new AlterUser(session);
            command.setType(CommandInterface.ALTER_USER_ADMIN);
            User user = database.getUser(userName);
            command.setUser(user);
            if (readIf("TRUE")) {
                command.setAdmin(true);
            } else if (readIf("FALSE")) {
                command.setAdmin(false);
            } else {
                throw getSyntaxError();
            }
            return command;
        }
        throw getSyntaxError();
    }

    private Prepared parseSet() {
        if (readIf("@")) {
            Set command = new Set(session, SetTypes.VARIABLE);
            command.setString(readAliasIdentifier());
            readIfEqualOrTo();
            command.setExpression(readExpression());
            return command;
        } else if (readIf("AUTOCOMMIT")) {
            readIfEqualOrTo();
            boolean value = readBooleanSetting();
            int setting = value ? CommandInterface.SET_AUTOCOMMIT_TRUE
                    : CommandInterface.SET_AUTOCOMMIT_FALSE;
            return new TransactionCommand(session, setting);
        } else if (readIf("MVCC")) {
            readIfEqualOrTo();
            boolean value = readBooleanSetting();
            Set command = new Set(session, SetTypes.MVCC);
            command.setInt(value ? 1 : 0);
            return command;
        } else if (readIf("EXCLUSIVE")) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.EXCLUSIVE);
            command.setExpression(readExpression());
            return command;
        } else if (readIf("IGNORECASE")) {
            readIfEqualOrTo();
            boolean value = readBooleanSetting();
            Set command = new Set(session, SetTypes.IGNORECASE);
            command.setInt(value ? 1 : 0);
            return command;
        } else if (readIf("PASSWORD")) {
            readIfEqualOrTo();
            AlterUser command = new AlterUser(session);
            command.setType(CommandInterface.ALTER_USER_SET_PASSWORD);
            command.setUser(session.getUser());
            command.setPassword(readExpression());
            return command;
        } else if (readIf("SALT")) {
            readIfEqualOrTo();
            AlterUser command = new AlterUser(session);
            command.setType(CommandInterface.ALTER_USER_SET_PASSWORD);
            command.setUser(session.getUser());
            command.setSalt(readExpression());
            read("HASH");
            command.setHash(readExpression());
            return command;
        } else if (readIf("MODE")) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.MODE);
            command.setString(readAliasIdentifier());
            return command;
        } else if (readIf("COMPRESS_LOB")) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.COMPRESS_LOB);
            if (currentTokenType == VALUE) {
                command.setString(readString());
            } else {
                command.setString(readUniqueIdentifier());
            }
            return command;
        } else if (readIf("DATABASE")) {
            readIfEqualOrTo();
            read("COLLATION");
            return parseSetCollation();
        } else if (readIf("COLLATION")) {
            readIfEqualOrTo();
            return parseSetCollation();
        } else if (readIf("BINARY_COLLATION")) {
            readIfEqualOrTo();
            return parseSetBinaryCollation();
        } else if (readIf("CLUSTER")) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.CLUSTER);
            command.setString(readString());
            return command;
        } else if (readIf("DATABASE_EVENT_LISTENER")) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.DATABASE_EVENT_LISTENER);
            command.setString(readString());
            return command;
        } else if (readIf("ALLOW_LITERALS")) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.ALLOW_LITERALS);
            if (readIf("NONE")) {
                command.setInt(Constants.ALLOW_LITERALS_NONE);
            } else if (readIf("ALL")) {
                command.setInt(Constants.ALLOW_LITERALS_ALL);
            } else if (readIf("NUMBERS")) {
                command.setInt(Constants.ALLOW_LITERALS_NUMBERS);
            } else {
                command.setInt(readPositiveInt());
            }
            return command;
        } else if (readIf("DEFAULT_TABLE_TYPE")) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.DEFAULT_TABLE_TYPE);
            if (readIf("MEMORY")) {
                command.setInt(Table.TYPE_MEMORY);
            } else if (readIf("CACHED")) {
                command.setInt(Table.TYPE_CACHED);
            } else {
                command.setInt(readPositiveInt());
            }
            return command;
        } else if (readIf("CREATE")) {
            readIfEqualOrTo();
            // Derby compatibility (CREATE=TRUE in the database URL)
            readToken();
            return new NoOperation(session);
        } else if (readIf("HSQLDB.DEFAULT_TABLE_TYPE")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("PAGE_STORE")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("CACHE_TYPE")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("FILE_LOCK")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("DB_CLOSE_ON_EXIT")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("AUTO_SERVER")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("AUTO_SERVER_PORT")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("AUTO_RECONNECT")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("ASSERT")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("ACCESS_MODE_DATA")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("OPEN_NEW")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("JMX")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("PAGE_SIZE")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("RECOVER")) {
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("NAMES")) {
            // Quercus PHP MySQL driver compatibility
            readIfEqualOrTo();
            readToken();
            return new NoOperation(session);
        } else if (readIf("SCHEMA")) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.SCHEMA);
            command.setString(readAliasIdentifier());
            return command;
        } else if (readIf("DATESTYLE")) {
            // PostgreSQL compatibility
            readIfEqualOrTo();
            if (!readIf("ISO")) {
                String s = readString();
                if (!equalsToken(s, "ISO")) {
                    throw getSyntaxError();
                }
            }
            return new NoOperation(session);
        } else if (readIf("SEARCH_PATH") || readIf(SetTypes.getTypeName(SetTypes.SCHEMA_SEARCH_PATH))) {
            readIfEqualOrTo();
            Set command = new Set(session, SetTypes.SCHEMA_SEARCH_PATH);
            ArrayList<String> list = new ArrayList<>();
            list.add(readAliasIdentifier());
            while (readIf(",")) {
                list.add(readAliasIdentifier());
            }
            String[] schemaNames = new String[list.size()];
            list.toArray(schemaNames);
            command.setStringArray(schemaNames);
            return command;
        } else if (readIf("JAVA_OBJECT_SERIALIZER")) {
            readIfEqualOrTo();
            return parseSetJavaObjectSerializer();
        } else {
            if (isToken("LOGSIZE")) {
                // HSQLDB compatibility
                currentToken = SetTypes.getTypeName(SetTypes.MAX_LOG_SIZE);
            }
            if (isToken("FOREIGN_KEY_CHECKS")) {
                // MySQL compatibility
                currentToken = SetTypes
                        .getTypeName(SetTypes.REFERENTIAL_INTEGRITY);
            }
            int type = SetTypes.getType(currentToken);
            if (type < 0) {
                throw getSyntaxError();
            }
            readToken();
            readIfEqualOrTo();
            Set command = new Set(session, type);
            command.setExpression(readExpression());
            return command;
        }
    }

    private void readIfEqualOrTo() {
        if (!readIf("=")) {
            readIf("TO");
        }
    }

    private Prepared parseUse() {
        readIfEqualOrTo();
        Set command = new Set(session, SetTypes.SCHEMA);
        command.setString(readAliasIdentifier());
        return command;
    }

    private Set parseSetCollation() {
        Set command = new Set(session, SetTypes.COLLATION);
        String name = readAliasIdentifier();
        command.setString(name);
        if (equalsToken(name, CompareMode.OFF)) {
            return command;
        }
        Collator coll = CompareMode.getCollator(name);
        if (coll == null) {
            throw DbException.getInvalidValueException("collation", name);
        }
        if (readIf("STRENGTH")) {
            if (readIf("PRIMARY")) {
                command.setInt(Collator.PRIMARY);
            } else if (readIf("SECONDARY")) {
                command.setInt(Collator.SECONDARY);
            } else if (readIf("TERTIARY")) {
                command.setInt(Collator.TERTIARY);
            } else if (readIf("IDENTICAL")) {
                command.setInt(Collator.IDENTICAL);
            }
        } else {
            command.setInt(coll.getStrength());
        }
        return command;
    }

    private Set parseSetBinaryCollation() {
        Set command = new Set(session, SetTypes.BINARY_COLLATION);
        String name = readAliasIdentifier();
        command.setString(name);
        if (equalsToken(name, CompareMode.UNSIGNED) ||
                equalsToken(name, CompareMode.SIGNED)) {
            return command;
        }
        throw DbException.getInvalidValueException("BINARY_COLLATION", name);
    }

    private Set parseSetJavaObjectSerializer() {
        Set command = new Set(session, SetTypes.JAVA_OBJECT_SERIALIZER);
        String name = readString();
        command.setString(name);
        return command;
    }

    private RunScriptCommand parseRunScript() {
        RunScriptCommand command = new RunScriptCommand(session);
        read("FROM");
        command.setFileNameExpr(readExpression());
        if (readIf("COMPRESSION")) {
            command.setCompressionAlgorithm(readUniqueIdentifier());
        }
        if (readIf("CIPHER")) {
            command.setCipher(readUniqueIdentifier());
            if (readIf("PASSWORD")) {
                command.setPassword(readExpression());
            }
        }
        if (readIf("CHARSET")) {
            command.setCharset(Charset.forName(readString()));
        }
        return command;
    }

    private ScriptCommand parseScript() {
        ScriptCommand command = new ScriptCommand(session);
        boolean data = true, passwords = true, settings = true;
        boolean dropTables = false, simple = false;
        if (readIf("SIMPLE")) {
            simple = true;
        }
        if (readIf("NODATA")) {
            data = false;
        }
        if (readIf("NOPASSWORDS")) {
            passwords = false;
        }
        if (readIf("NOSETTINGS")) {
            settings = false;
        }
        if (readIf("DROP")) {
            dropTables = true;
        }
        if (readIf("BLOCKSIZE")) {
            long blockSize = readLong();
            command.setLobBlockSize(blockSize);
        }
        command.setData(data);
        command.setPasswords(passwords);
        command.setSettings(settings);
        command.setDrop(dropTables);
        command.setSimple(simple);
        if (readIf("TO")) {
            command.setFileNameExpr(readExpression());
            if (readIf("COMPRESSION")) {
                command.setCompressionAlgorithm(readUniqueIdentifier());
            }
            if (readIf("CIPHER")) {
                command.setCipher(readUniqueIdentifier());
                if (readIf("PASSWORD")) {
                    command.setPassword(readExpression());
                }
            }
            if (readIf("CHARSET")) {
                command.setCharset(Charset.forName(readString()));
            }
        }
        if (readIf("SCHEMA")) {
            HashSet<String> schemaNames = New.hashSet();
            do {
                schemaNames.add(readUniqueIdentifier());
            } while (readIf(","));
            command.setSchemaNames(schemaNames);
        } else if (readIf("TABLE")) {
            ArrayList<Table> tables = new ArrayList<>();
            do {
                tables.add(readTableOrView());
            } while (readIf(","));
            command.setTables(tables);
        }
        return command;
    }

    private Table readTableOrView() {
        return readTableOrView(readIdentifierWithSchema(null));
    }

    private Table readTableOrView(String tableName) {
        // same algorithm than readSequence
        if (schemaName != null) {
            return getSchema().getTableOrView(session, tableName);
        }

        Schema schema = database.getSchema(session.getCurrentSchemaName());
        Table table = schema.resolveTableOrView(session, tableName);
        if (table != null) {
            return table;
        }

        String[] schemaNames = session.getSchemaSearchPath();
        if (schemaNames != null) {
            for (String name : schemaNames) {
                schema = database.getSchema(name);
                table = schema.resolveTableOrView(session, tableName);
                if (table != null) {
                    return table;
                }
            }
        }

        throw DbException.get(TABLE_OR_VIEW_NOT_FOUND_1, tableName);
    }

    private Prepared parseAlterTable() {
        boolean ifTableExists = readIfExists(false);
        String tableName = readIdentifierWithSchema();
        Schema schema = getSchema();
        if (readIf("ADD")) {
            Prepared command = parseAlterTableAddConstraintIf(tableName,
                    schema, ifTableExists);
            if (command != null) {
                return command;
            }
            return parseAlterTableAddColumn(tableName, schema, ifTableExists);
        } else if (readIf("SET")) {
            read("REFERENTIAL_INTEGRITY");
            int type = CommandInterface.ALTER_TABLE_SET_REFERENTIAL_INTEGRITY;
            boolean value = readBooleanSetting();
            AlterTableSet command = new AlterTableSet(session,
                    schema, type, value);
            command.setTableName(tableName);
            command.setIfTableExists(ifTableExists);
            if (readIf("CHECK")) {
                command.setCheckExisting(true);
            } else if (readIf("NOCHECK")) {
                command.setCheckExisting(false);
            }
            return command;
        } else if (readIf("RENAME")) {
            if (readIf("COLUMN")) {
                // PostgreSQL syntax
                String columnName = readColumnIdentifier();
                read("TO");
                AlterTableRenameColumn command = new AlterTableRenameColumn(
                        session, schema);
                command.setTableName(tableName);
                command.setIfTableExists(ifTableExists);
                command.setOldColumnName(columnName);
                String newName = readColumnIdentifier();
                command.setNewColumnName(newName);
                return command;
            } else if (readIf("CONSTRAINT")) {
                String constraintName = readIdentifierWithSchema(schema.getName());
                checkSchema(schema);
                read("TO");
                AlterTableRenameConstraint command = new AlterTableRenameConstraint(
                        session, schema);
                command.setConstraintName(constraintName);
                String newName = readColumnIdentifier();
                command.setNewConstraintName(newName);
                return commandIfTableExists(schema, tableName, ifTableExists, command);
            } else {
                read("TO");
                String newName = readIdentifierWithSchema(schema.getName());
                checkSchema(schema);
                AlterTableRename command = new AlterTableRename(session,
                        getSchema());
                command.setOldTableName(tableName);
                command.setNewTableName(newName);
                command.setIfTableExists(ifTableExists);
                command.setHidden(readIf("HIDDEN"));
                return command;
            }
        } else if (readIf("DROP")) {
            if (readIf("CONSTRAINT")) {
                boolean ifExists = readIfExists(false);
                String constraintName = readIdentifierWithSchema(schema.getName());
                ifExists = readIfExists(ifExists);
                checkSchema(schema);
                AlterTableDropConstraint command = new AlterTableDropConstraint(
                        session, getSchema(), ifExists);
                command.setConstraintName(constraintName);
                return commandIfTableExists(schema, tableName, ifTableExists, command);
            } else if (readIf("FOREIGN")) {
                // MySQL compatibility
                read("KEY");
                String constraintName = readIdentifierWithSchema(schema.getName());
                checkSchema(schema);
                AlterTableDropConstraint command = new AlterTableDropConstraint(
                        session, getSchema(), false);
                command.setConstraintName(constraintName);
                return commandIfTableExists(schema, tableName, ifTableExists, command);
            } else if (readIf("INDEX")) {
                // MySQL compatibility
                String indexName = readIdentifierWithSchema();
                DropIndex command = new DropIndex(session, getSchema());
                command.setIndexName(indexName);
                return commandIfTableExists(schema, tableName, ifTableExists, command);
            } else if (readIf("PRIMARY")) {
                read("KEY");
                Table table = tableIfTableExists(schema, tableName, ifTableExists);
                if (table == null) {
                    return new NoOperation(session);
                }
                Index idx = table.getPrimaryKey();
                DropIndex command = new DropIndex(session, schema);
                command.setIndexName(idx.getName());
                return command;
            } else {
                readIf("COLUMN");
                boolean ifExists = readIfExists(false);
                AlterTableAlterColumn command = new AlterTableAlterColumn(session, schema);
                command.setType(CommandInterface.ALTER_TABLE_DROP_COLUMN);
                ArrayList<Column> columnsToRemove = new ArrayList<>();
                Table table = tableIfTableExists(schema, tableName, ifTableExists);
                do {
                    String columnName = readColumnIdentifier();
                    if (table == null) {
                        return new NoOperation(session);
                    }
                    if (ifExists && !table.doesColumnExist(columnName)) {
                        return new NoOperation(session);
                    }
                    Column column = table.getColumn(columnName);
                    columnsToRemove.add(column);
                } while (readIf(","));
                command.setTableName(tableName);
                command.setIfTableExists(ifTableExists);
                command.setColumnsToRemove(columnsToRemove);
                return command;
            }
        } else if (readIf("CHANGE")) {
            // MySQL compatibility
            readIf("COLUMN");
            String columnName = readColumnIdentifier();
            String newColumnName = readColumnIdentifier();
            Column column = columnIfTableExists(schema, tableName, columnName, ifTableExists);
            boolean nullable = column == null ? true : column.isNullable();
            // new column type ignored. RENAME and MODIFY are
            // a single command in MySQL but two different commands in H2.
            parseColumnForTable(newColumnName, nullable);
            AlterTableRenameColumn command = new AlterTableRenameColumn(session, schema);
            command.setTableName(tableName);
            command.setIfTableExists(ifTableExists);
            command.setOldColumnName(columnName);
            command.setNewColumnName(newColumnName);
            return command;
        } else if (readIf("MODIFY")) {
            // MySQL compatibility
            readIf("COLUMN");
            String columnName = readColumnIdentifier();
            return parseAlterTableAlterColumnType(schema, tableName, columnName, ifTableExists);
        } else if (readIf("ALTER")) {
            readIf("COLUMN");
            String columnName = readColumnIdentifier();
            Column column = columnIfTableExists(schema, tableName, columnName, ifTableExists);
            if (readIf("RENAME")) {
                read("TO");
                AlterTableRenameColumn command = new AlterTableRenameColumn(
                        session, schema);
                command.setTableName(tableName);
                command.setIfTableExists(ifTableExists);
                command.setOldColumnName(columnName);
                String newName = readColumnIdentifier();
                command.setNewColumnName(newName);
                return command;
            } else if (readIf("DROP")) {
                // PostgreSQL compatibility
                if (readIf("DEFAULT")) {
                    AlterTableAlterColumn command = new AlterTableAlterColumn(
                            session, schema);
                    command.setTableName(tableName);
                    command.setIfTableExists(ifTableExists);
                    command.setOldColumn(column);
                    command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_DEFAULT);
                    command.setDefaultExpression(null);
                    return command;
                }
                read("NOT");
                read("NULL");
                AlterTableAlterColumn command = new AlterTableAlterColumn(
                        session, schema);
                command.setTableName(tableName);
                command.setIfTableExists(ifTableExists);
                command.setOldColumn(column);
                command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_NULL);
                return command;
            } else if (readIf("TYPE")) {
                // PostgreSQL compatibility
                return parseAlterTableAlterColumnType(schema, tableName, columnName, ifTableExists);
            } else if (readIf("SET")) {
                if (readIf("DATA")) {
                    // Derby compatibility
                    read("TYPE");
                    return parseAlterTableAlterColumnType(schema, tableName, columnName, ifTableExists);
                }
                AlterTableAlterColumn command = new AlterTableAlterColumn(session, schema);
                command.setTableName(tableName);
                command.setIfTableExists(ifTableExists);
                command.setOldColumn(column);
                if (readIf("NULL")) {
                    command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_NULL);
                    return command;
                } else if (readIf("NOT")) {
                    read("NULL");
                    command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_NOT_NULL);
                    return command;
                } else if (readIf("DEFAULT")) {
                    Expression defaultExpression = readExpression();
                    command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_DEFAULT);
                    command.setDefaultExpression(defaultExpression);
                    return command;
                } else if (readIf("INVISIBLE")) {
                    command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_VISIBILITY);
                    command.setVisible(false);
                    return command;
                } else if (readIf("VISIBLE")) {
                    command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_VISIBILITY);
                    command.setVisible(true);
                    return command;
                }
            } else if (readIf("RESTART")) {
                readIf("WITH");
                Expression start = readExpression();
                AlterSequence command = new AlterSequence(session, schema);
                command.setColumn(column);
                command.setStartWith(start);
                return commandIfTableExists(schema, tableName, ifTableExists, command);
            } else if (readIf("SELECTIVITY")) {
                AlterTableAlterColumn command = new AlterTableAlterColumn(session, schema);
                command.setTableName(tableName);
                command.setIfTableExists(ifTableExists);
                command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_SELECTIVITY);
                command.setOldColumn(column);
                command.setSelectivity(readExpression());
                return command;
            } else {
                return parseAlterTableAlterColumnType(schema, tableName, columnName, ifTableExists);
            }
        }
        throw getSyntaxError();
    }

    private Table tableIfTableExists(Schema schema, String tableName, boolean ifTableExists) {
        Table table = schema.resolveTableOrView(session, tableName);
        if (table == null && !ifTableExists) {
            throw DbException.get(TABLE_OR_VIEW_NOT_FOUND_1, tableName);
        }
        return table;
    }

    private Column columnIfTableExists(Schema schema, String tableName,
            String columnName, boolean ifTableExists) {
        Table table = tableIfTableExists(schema, tableName, ifTableExists);
        return table == null ? null : table.getColumn(columnName);
    }

    private Prepared commandIfTableExists(Schema schema, String tableName,
            boolean ifTableExists, Prepared commandIfTableExists) {
        return tableIfTableExists(schema, tableName, ifTableExists) == null
            ? new NoOperation(session)
            : commandIfTableExists;
    }

    private AlterTableAlterColumn parseAlterTableAlterColumnType(Schema schema,
            String tableName, String columnName, boolean ifTableExists) {
        Column oldColumn = columnIfTableExists(schema, tableName, columnName, ifTableExists);
        Column newColumn = parseColumnForTable(columnName, (oldColumn == null) || oldColumn.isNullable());
        AlterTableAlterColumn command = new AlterTableAlterColumn(session, schema);
        command.setTableName(tableName);
        command.setIfTableExists(ifTableExists);
        command.setType(CommandInterface.ALTER_TABLE_ALTER_COLUMN_CHANGE_TYPE);
        command.setOldColumn(oldColumn);
        command.setNewColumn(newColumn);
        return command;
    }

    private AlterTableAlterColumn parseAlterTableAddColumn(String tableName, Schema schema, boolean ifTableExists) {
        readIf("COLUMN");
        AlterTableAlterColumn command = new AlterTableAlterColumn(session, schema);
        command.setType(CommandInterface.ALTER_TABLE_ADD_COLUMN);
        command.setTableName(tableName);
        command.setIfTableExists(ifTableExists);
        ArrayList<Column> columnsToAdd = new ArrayList<>();
        if (readIf("(")) {
            command.setIfNotExists(false);
            do {
                String columnName = readColumnIdentifier();
                Column column = parseColumnForTable(columnName, true);
                columnsToAdd.add(column);
            } while (readIf(","));
            read(")");
            if (readIf("BEFORE")) {
                command.setAddBefore(readColumnIdentifier());
            } else if (readIf("AFTER")) {
                command.setAddAfter(readColumnIdentifier());
            }
        } else {
            boolean ifNotExists = readIfNotExists();
            command.setIfNotExists(ifNotExists);
            String columnName = readColumnIdentifier();
            Column column = parseColumnForTable(columnName, true);
            columnsToAdd.add(column);
            if (readIf("BEFORE")) {
                command.setAddBefore(readColumnIdentifier());
            } else if (readIf("AFTER")) {
                command.setAddAfter(readColumnIdentifier());
            }
        }
        command.setNewColumns(columnsToAdd);
        return command;
    }

    private int parseAction() {
        Integer result = parseCascadeOrRestrict();
        if (result != null) {
            return result;
        }
        if (readIf("NO")) {
            read("ACTION");
            return ConstraintReferential.RESTRICT;
        }
        read("SET");
        if (readIf("NULL")) {
            return ConstraintReferential.SET_NULL;
        }
        read("DEFAULT");
        return ConstraintReferential.SET_DEFAULT;
    }

    private Integer parseCascadeOrRestrict() {
        if (readIf("CASCADE")) {
            return ConstraintReferential.CASCADE;
        } else if (readIf("RESTRICT")) {
            return ConstraintReferential.RESTRICT;
        } else {
            return null;
        }
    }

    private DefineCommand parseAlterTableAddConstraintIf(String tableName, Schema schema, boolean ifTableExists) {
        String constraintName = null, comment = null;
        boolean ifNotExists = false;
        boolean allowIndexDefinition = mode.indexDefinitionInCreateTable;
        boolean allowAffinityKey = mode.allowAffinityKey;
        if (readIf("CONSTRAINT")) {
            ifNotExists = readIfNotExists();
            constraintName = readIdentifierWithSchema(schema.getName());
            checkSchema(schema);
            comment = readCommentIf();
            allowIndexDefinition = true;
        }
        if (readIf("PRIMARY")) {
            read("KEY");
            AlterTableAddConstraint command = new AlterTableAddConstraint(session, schema, ifNotExists);
            command.setType(CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY);
            command.setComment(comment);
            command.setConstraintName(constraintName);
            command.setTableName(tableName);
            command.setIfTableExists(ifTableExists);
            if (readIf("HASH")) {
                command.setPrimaryKeyHash(true);
            }
            read("(");
            command.setIndexColumns(parseIndexColumnList());
            if (readIf("INDEX")) {
                String indexName = readIdentifierWithSchema();
                command.setIndex(getSchema().findIndex(session, indexName));
            }
            return command;
        } else if (allowIndexDefinition && (isToken("INDEX") || isToken("KEY"))) {
            // MySQL
            // need to read ahead, as it could be a column name
            int start = lastParseIndex;
            readToken();
            if (DataType.getTypeByName(currentToken) != null) {
                // known data type
                parseIndex = start;
                readToken();
                return null;
            }
            CreateIndex command = new CreateIndex(session, schema);
            command.setComment(comment);
            command.setTableName(tableName);
            command.setIfTableExists(ifTableExists);
            if (!readIf("(")) {
                command.setIndexName(readUniqueIdentifier());
                read("(");
            }
            command.setIndexColumns(parseIndexColumnList());
            // MySQL compatibility
            if (readIf("USING")) {
                read("BTREE");
            }
            return command;
        } else if (allowAffinityKey && readIfAffinity()) {
            read("KEY");
            read("(");
            CreateIndex command = createAffinityIndex(schema, tableName, parseIndexColumnList());
            command.setIfTableExists(ifTableExists);
            return command;
        }
        AlterTableAddConstraint command;
        if (readIf("CHECK")) {
            command = new AlterTableAddConstraint(session, schema, ifNotExists);
            command.setType(CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_CHECK);
            command.setCheckExpression(readExpression());
        } else if (readIf("UNIQUE")) {
            readIf("KEY");
            readIf("INDEX");
            command = new AlterTableAddConstraint(session, schema, ifNotExists);
            command.setType(CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_UNIQUE);
            if (!readIf("(")) {
                constraintName = readUniqueIdentifier();
                read("(");
            }
            command.setIndexColumns(parseIndexColumnList());
            if (readIf("INDEX")) {
                String indexName = readIdentifierWithSchema();
                command.setIndex(getSchema().findIndex(session, indexName));
            }
            // MySQL compatibility
            if (readIf("USING")) {
                read("BTREE");
            }
        } else if (readIf("FOREIGN")) {
            command = new AlterTableAddConstraint(session, schema, ifNotExists);
            command.setType(CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_REFERENTIAL);
            read("KEY");
            read("(");
            command.setIndexColumns(parseIndexColumnList());
            if (readIf("INDEX")) {
                String indexName = readIdentifierWithSchema();
                command.setIndex(schema.findIndex(session, indexName));
            }
            read("REFERENCES");
            parseReferences(command, schema, tableName);
        } else {
            if (constraintName != null) {
                throw getSyntaxError();
            }
            return null;
        }
        if (readIf("NOCHECK")) {
            command.setCheckExisting(false);
        } else {
            readIf("CHECK");
            command.setCheckExisting(true);
        }
        command.setTableName(tableName);
        command.setIfTableExists(ifTableExists);
        command.setConstraintName(constraintName);
        command.setComment(comment);
        return command;
    }

    private void parseReferences(AlterTableAddConstraint command,
            Schema schema, String tableName) {
        if (readIf("(")) {
            command.setRefTableName(schema, tableName);
            command.setRefIndexColumns(parseIndexColumnList());
        } else {
            String refTableName = readIdentifierWithSchema(schema.getName());
            command.setRefTableName(getSchema(), refTableName);
            if (readIf("(")) {
                command.setRefIndexColumns(parseIndexColumnList());
            }
        }
        if (readIf("INDEX")) {
            String indexName = readIdentifierWithSchema();
            command.setRefIndex(getSchema().findIndex(session, indexName));
        }
        while (readIf("ON")) {
            if (readIf("DELETE")) {
                command.setDeleteAction(parseAction());
            } else {
                read("UPDATE");
                command.setUpdateAction(parseAction());
            }
        }
        if (readIf("NOT")) {
            read("DEFERRABLE");
        } else {
            readIf("DEFERRABLE");
        }
    }

    private CreateLinkedTable parseCreateLinkedTable(boolean temp,
            boolean globalTemp, boolean force) {
        read("TABLE");
        boolean ifNotExists = readIfNotExists();
        String tableName = readIdentifierWithSchema();
        CreateLinkedTable command = new CreateLinkedTable(session, getSchema());
        command.setTemporary(temp);
        command.setGlobalTemporary(globalTemp);
        command.setForce(force);
        command.setIfNotExists(ifNotExists);
        command.setTableName(tableName);
        command.setComment(readCommentIf());
        read("(");
        command.setDriver(readString());
        read(",");
        command.setUrl(readString());
        read(",");
        command.setUser(readString());
        read(",");
        command.setPassword(readString());
        read(",");
        String originalTable = readString();
        if (readIf(",")) {
            command.setOriginalSchema(originalTable);
            originalTable = readString();
        }
        command.setOriginalTable(originalTable);
        read(")");
        if (readIf("EMIT")) {
            read("UPDATES");
            command.setEmitUpdates(true);
        } else if (readIf("READONLY")) {
            command.setReadOnly(true);
        }
        return command;
    }

    private CreateTable parseCreateTable(boolean temp, boolean globalTemp, boolean persistIndexes) {
        boolean ifNotExists = readIfNotExists();
        String tableName = readIdentifierWithSchema();
        if (temp && globalTemp && equalsToken("SESSION", schemaName)) {
            // support weird syntax: declare global temporary table session.xy
            // (...) not logged
            schemaName = session.getCurrentSchemaName();
            globalTemp = false;
        }

        Schema schema = getSchema();
        CreateTable command = new CreateTable(session, schema);
        command.setPersistIndexes(persistIndexes);
        command.setTemporary(temp);
        command.setGlobalTemporary(globalTemp);
        command.setIfNotExists(ifNotExists);
        command.setTableName(tableName);
        command.setComment(readCommentIf());
        if (readIf("(")) {
            if (!readIf(")")) {
                do {
                    DefineCommand c = parseAlterTableAddConstraintIf(tableName, schema, false);
                    if (c != null) {
                        command.addConstraintCommand(c);
                    } else {
                        String columnName = readColumnIdentifier();
                        Column column = parseColumnForTable(columnName, true);
                        if (column.isAutoIncrement() && column.isPrimaryKey()) {
                            column.setPrimaryKey(false);
                            IndexColumn[] cols = { new IndexColumn() };
                            cols[0].columnName = column.getName();
                            AlterTableAddConstraint pk = new AlterTableAddConstraint(session, schema, false);
                            pk.setType(CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY);
                            pk.setTableName(tableName);
                            pk.setIndexColumns(cols);
                            command.addConstraintCommand(pk);
                        }
                        command.addColumn(column);
                        String constraintName = null;
                        if (readIf("CONSTRAINT")) {
                            constraintName = readColumnIdentifier();
                        }
                        // For compatibility with Apache Ignite.
                        boolean allowAffinityKey = mode.allowAffinityKey;
                        boolean affinity = allowAffinityKey && readIfAffinity();
                        if (readIf("PRIMARY")) {
                            read("KEY");
                            boolean hash = readIf("HASH");
                            IndexColumn[] cols = { new IndexColumn() };
                            cols[0].columnName = column.getName();
                            AlterTableAddConstraint pk = new AlterTableAddConstraint(session, schema, false);
                            pk.setPrimaryKeyHash(hash);
                            pk.setType(CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY);
                            pk.setTableName(tableName);
                            pk.setIndexColumns(cols);
                            command.addConstraintCommand(pk);
                            if (readIf("AUTO_INCREMENT")) {
                                parseAutoIncrement(column);
                            }
                            if (affinity) {
                                CreateIndex idx = createAffinityIndex(schema, tableName, cols);
                                command.addConstraintCommand(idx);
                            }
                        } else if (affinity) {
                            read("KEY");
                            IndexColumn[] cols = { new IndexColumn() };
                            cols[0].columnName = column.getName();
                            CreateIndex idx = createAffinityIndex(schema, tableName, cols);
                            command.addConstraintCommand(idx);
                        } else if (readIf("UNIQUE")) {
                            AlterTableAddConstraint unique =
                              new AlterTableAddConstraint(session, schema, false);
                            unique.setConstraintName(constraintName);
                            unique.setType(CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_UNIQUE);
                            IndexColumn[] cols = { new IndexColumn() };
                            cols[0].columnName = columnName;
                            unique.setIndexColumns(cols);
                            unique.setTableName(tableName);
                            command.addConstraintCommand(unique);
                        }
                        if (readIf("NOT")) {
                            read("NULL");
                            column.setNullable(false);
                        } else {
                            readIf("NULL");
                        }
                        if (readIf("CHECK")) {
                            Expression expr = readExpression();
                            column.addCheckConstraint(session, expr);
                        }
                        if (readIf("REFERENCES")) {
                            AlterTableAddConstraint ref = new AlterTableAddConstraint(session, schema, false);
                            ref.setConstraintName(constraintName);
                            ref.setType(CommandInterface.ALTER_TABLE_ADD_CONSTRAINT_REFERENTIAL);
                            IndexColumn[] cols = { new IndexColumn() };
                            cols[0].columnName = columnName;
                            ref.setIndexColumns(cols);
                            ref.setTableName(tableName);
                            parseReferences(ref, schema, tableName);
                            command.addConstraintCommand(ref);
                        }
                    }
                } while (readIfMore());
            }
        }

        // Allows "COMMENT='comment'" in DDL statements (MySQL syntax)
        if (readIf("COMMENT")) {
            if (readIf("=")) {
                // read the complete string comment, but nothing with it for now
                readString();
            }
        }

        if (readIf("ENGINE")) {
            if (readIf("=")) {
                // map MySQL engine types onto H2 behavior
                String tableEngine = readUniqueIdentifier();
                if ("InnoDb".equalsIgnoreCase(tableEngine)) {
                    // ok
                } else if (!"MyISAM".equalsIgnoreCase(tableEngine)) {
                    throw DbException.getUnsupportedException(tableEngine);
                }
            } else {
                command.setTableEngine(readUniqueIdentifier());
            }
        }
        if (readIf("WITH")) {
            command.setTableEngineParams(readTableEngineParams());
        }

        // MySQL compatibility
        if (readIf("AUTO_INCREMENT")) {
            read("=");
            if (currentTokenType != VALUE || currentValue.getType() != Value.INT) {
                throw DbException.getSyntaxError(cleanSql, parseIndex, "integer");
            }
            readToken();
        }

        readIf("DEFAULT");
        if (readIf("CHARSET")) {
            read("=");
            if (!readIf("UTF8")) {
                read("UTF8MB4");
            }
        }

        if (temp) {
            if (readIf("ON")) {
                read("COMMIT");
                if (readIf("DROP")) {
                    command.setOnCommitDrop();
                } else if (readIf("DELETE")) {
                    read("ROWS");
                    command.setOnCommitTruncate();
                }
            } else if (readIf("NOT")) {
                if (readIf("PERSISTENT")) {
                    command.setPersistData(false);
                } else {
                    read("LOGGED");
                }
            }
            if (readIf("TRANSACTIONAL")) {
                command.setTransactional(true);
            }
        } else if (!persistIndexes && readIf("NOT")) {
            read("PERSISTENT");
            command.setPersistData(false);
        }

        if (readIf("HIDDEN")) {
            command.setHidden(true);
        }

        if (readIf("AS")) {
            if (readIf("SORTED")) {
                command.setSortedInsertMode(true);
            }
            command.setQuery(parseSelect());
        }

        // for MySQL compatibility
        if (readIf("ROW_FORMAT")) {
            if (readIf("=")) {
                readColumnIdentifier();
            }
        }

        return command;
    }

    private CreateSynonym parseCreateSynonym(boolean orReplace) {
        boolean ifNotExists = readIfNotExists();
        String name = readIdentifierWithSchema();
        Schema synonymSchema = getSchema();
        read("FOR");
        String tableName = readIdentifierWithSchema();

        Schema targetSchema = getSchema();
        CreateSynonym command = new CreateSynonym(session, synonymSchema);
        command.setName(name);
        command.setSynonymFor(tableName);
        command.setSynonymForSchema(targetSchema);
        command.setComment(readCommentIf());
        command.setIfNotExists(ifNotExists);
        command.setOrReplace(orReplace);
        return command;
    }

    private CreateIndex createAffinityIndex(Schema schema, String tableName, IndexColumn[] indexColumns) {
        CreateIndex idx = new CreateIndex(session, schema);
        idx.setTableName(tableName);
        idx.setIndexColumns(indexColumns);
        idx.setAffinity(true);
        return idx;
    }

    /**
     * Add double quotes around an identifier if required.
     *
     * @param s the identifier
     * @return the quoted identifier
     */
    public static String quoteIdentifier(String s) {
        if (s == null || s.length() == 0) {
            return "\"\"";
        }
        char c = s.charAt(0);
        // lowercase a-z is quoted as well
        if ((!Character.isLetter(c) && c != '_') || Character.isLowerCase(c)) {
            return StringUtils.quoteIdentifier(s);
        }
        for (int i = 1, length = s.length(); i < length; i++) {
            c = s.charAt(i);
            if ((!Character.isLetterOrDigit(c) && c != '_') ||
                    Character.isLowerCase(c)) {
                return StringUtils.quoteIdentifier(s);
            }
        }
        if (isKeyword(s, true)) {
            return StringUtils.quoteIdentifier(s);
        }
        return s;
    }

    public void setLiteralsChecked(boolean literalsChecked) {
        this.literalsChecked = literalsChecked;
    }

    public void setRightsChecked(boolean rightsChecked) {
        this.rightsChecked = rightsChecked;
    }

    public void setSuppliedParameterList(ArrayList<Parameter> suppliedParameterList) {
        this.suppliedParameterList = suppliedParameterList;
    }

    protected void checkLiterals(boolean isText) {
        if (!literalsChecked && !session.getAllowLiterals()) {
            int allowed = database.getAllowLiterals();
            if (allowed == Constants.ALLOW_LITERALS_NONE || (isText && allowed != Constants.ALLOW_LITERALS_ALL)) {
                throw DbException.get(ErrorCode.LITERALS_ARE_NOT_ALLOWED);
            }
        }
    }

    private Schema getSchema(String schemaName) {
        if (schemaName == null) {
            return null;
        }
        Schema schema = findSchema(schemaName);
        if (schema == null) {
            throw DbException.get(ErrorCode.SCHEMA_NOT_FOUND_1, schemaName);
        }
        return schema;
    }

    protected Schema getSchema() {
        return getSchema(schemaName);
    }

    private Schema findSchema(String schemaName) {
        if (schemaName == null) {
            return null;
        }
        Schema schema = database.findSchema(schemaName);
        if (schema == null) {
            if (equalsToken("SESSION", schemaName)) {
                // for local temporary tables
                schema = database.getSchema(session.getCurrentSchemaName());
            } else if (mode.sysDummy1 &&
              "SYSIBM".equals(schemaName)) {
                // IBM DB2 and Apache Derby compatibility: SYSIBM.SYSDUMMY1
                schema = database.getSchema(session.getCurrentSchemaName());
            }
        }
        return schema;
    }

    /**
     * Parse a SQL code snippet that represents an expression.
     *
     * @param sql the code snippet
     * @return the expression object
     */
    public static Expression parseExpression(Session session, String sql) {
        Parser parser = new Parser(session, sql);
        parser.annotate();
        parser.readToken();
        return parser.readExpression();
    }

    /**
     * Parse a SQL code snippet that represents a table name.
     *
     * @param sql the code snippet
     * @return the table object
     */
    public static Table parseTableName(Session session, String sql) {
        Parser parser = new Parser(session, sql);
        parser.annotate();
        parser.readToken();
        return parser.readTableOrView();
    }
}
