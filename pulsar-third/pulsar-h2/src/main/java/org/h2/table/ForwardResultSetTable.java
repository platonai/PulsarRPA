package org.h2.table;

import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.schema.Schema;

import java.sql.ResultSet;

/**
 * Created by vincent on 17-10-24.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * TODO: use ResultTempTable or something
 */
public class ForwardResultSetTable extends FunctionTable {

  private ResultInterface resultInterface;

  public ForwardResultSetTable(Schema schema, Session session, String name, ResultInterface resultInterface) {
    super(schema, session, name);
    this.resultInterface = resultInterface;

    setRowCount(resultInterface.getRowCount());

    int columnCount = resultInterface.getVisibleColumnCount();
    Column[] cols = new Column[columnCount];
    for (int i = 0; i < columnCount; i++) {
      cols[i] = new Column(resultInterface.getColumnName(i),
        resultInterface.getColumnType(i),
        resultInterface.getColumnPrecision(i),
        resultInterface.getColumnScale(i), resultInterface.getDisplaySize(i));
    }
    setColumns(cols);
  }

  /**
   * Forward the result directly.
   *
   * @param session the session
   * @return the result
   */
  @Override
  public ResultInterface getResult(Session session) {
    return resultInterface;
  }

  @Override
  public ResultSet getResultSet(Session session) {
    throw DbException.getUnsupportedException("ALIAS");
  }

  @Override
  public boolean isBufferResultSetToLocalTemp() {
    return true;
  }

  @Override
  public boolean isDeterministic() {
    return true;
  }
}
