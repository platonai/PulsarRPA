/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.mongodb.utils;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import org.apache.avro.util.Utf8;
import org.bson.BSONObject;
import org.bson.Document;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

/**
 * Utility class to build {@link DBObject} used by MongoDB in an easy way by
 * directly specifying the fully qualified names of fields.
 *
 * @author Fabien Poulard fpoulard@dictanova.com
 */
public class BSONDecorator {

  final private Document myBson;

  public BSONDecorator(final Document obj) {
    myBson = obj;
  }

  /**
   * Check if the field passed in parameter exists or not. The field is passed
   * as a fully qualified name that is the path to the field from the root of
   * the document (for example: "field1" or "parent.child.field2").
   *
   * @param fieldName fully qualified name of the field
   * @return true if the field and all its parents exists in the decorated
   * {@link DBObject}, false otherwise
   */
  public boolean containsField(String fieldName) {
    // Prepare for in depth setting
    String[] fields = fieldName.split("\\.");
    int i = 0;
    Document intermediate = myBson;

    // Set intermediate parents
    while (i < (fields.length - 1)) {
      if (!intermediate.containsKey(fields[i]))
        return false;
      intermediate = (Document) intermediate.get(fields[i]);
      i++;
    }

    // Check final field
    return intermediate.containsKey(fields[fields.length - 1]);
  }

  /**
   * Access field as a {@link Document}.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field as a {@link Document}
   */
  public Document getDBObject(String fieldName) {
    return (Document) getFieldParent(fieldName)
            .get(getLeafName(fieldName));
  }

  /**
   * Access field as a {@link BasicDBList}.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field as a {@link BasicDBList}
   */
  public ArrayList<Document> getDBList(String fieldName) {
    return (ArrayList<Document>) getFieldParent(fieldName).get(getLeafName(fieldName));
  }

  /**
   * Access field as a boolean.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field as a boolean
   */
  public Boolean getBoolean(String fieldName) {
    Document parent = getFieldParent(fieldName);
    String lf = getLeafName(fieldName);
    return parent.containsKey(lf) ? parent.getBoolean(lf) : null;
  }

  /**
   * Access field as a double.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field as a double
   */
  public Double getDouble(String fieldName) {
    Document parent = getFieldParent(fieldName);
    String lf = getLeafName(fieldName);
    return parent.containsKey(lf) ? parent.getDouble(lf) : null;
  }

  /**
   * Access field as a int.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field as a double
   */
  public Integer getInt(String fieldName) {
    Document parent = getFieldParent(fieldName);
    String lf = getLeafName(fieldName);
    return parent.containsKey(lf) && parent.get(lf) != null ? parent.getInteger(lf) : null;
  }

  /**
   * Access field as a long.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field as a double
   */
  public Long getLong(String fieldName) {
    Document parent = getFieldParent(fieldName);
    String lf = getLeafName(fieldName);
    return parent.containsKey(lf) ? parent.getLong(lf) : null;
  }

  /**
   * Access field as a date.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field as a date
   */
  public Date getDate(String fieldName) {
    Document parent = getFieldParent(fieldName);
    String lf = getLeafName(fieldName);
    return parent.getDate(lf);
  }

  /**
   * Access field as a Utf8 string.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field as a {@link Utf8} string
   */
  public Utf8 getUtf8String(String fieldName) {
    Document parent = getFieldParent(fieldName);
    String value = parent.getString(getLeafName(fieldName));
    return (value != null) ? new Utf8(value) : null;
  }

  /**
   * Access field as bytes.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field
   */
  public ByteBuffer getBytes(String fieldName) {
    Object o = get(fieldName);
    if (o == null)
      return null;
    else if (o instanceof byte[])
      return ByteBuffer.wrap((byte[]) o);
    else
      return (ByteBuffer) o;
  }

  /**
   * Access field as an object, no casting.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @return value of the field
   */
  public Object get(String fieldName) {
    Document parent = getFieldParent(fieldName);
    return parent.get(getLeafName(fieldName));
  }

  /**
   * Set field. Create the intermediate levels if necessary as
   * {@link Document} fields.
   *
   * @param fieldName fully qualified name of the field to be accessed
   * @param value     value of the field
   */
  public void put(String fieldName, Object value) {
    Document parent = getFieldParent(fieldName, true);
    parent.put(getLeafName(fieldName), value);
  }

  // ////////////////////////////////////////////////////////////// UTILITIES

  /**
   * Retrieve the parent of a field.
   *
   * @param fieldName       fully qualified name of the field
   * @param createIfMissing create the intermediate fields if necessary
   * @return the parent of the field
   * @throws IllegalAccessError if the field does not exist
   */
  private Document getFieldParent(String fieldName, boolean createIfMissing) {
    String[] fields = fieldName.split("\\.");
    int i = 0;
    Document intermediate = (Document) myBson;

    // Set intermediate parents
    while (i < (fields.length - 1)) {
      if (!intermediate.containsKey(fields[i]))
        if (createIfMissing)
          intermediate.put(fields[i], new Document());
        else
          throw new IllegalAccessError("The field '" + fieldName
                  + "' does not exist: '" + fields[i] + "' is missing.");
      intermediate = (Document) intermediate.get(fields[i]);
      i++;
    }

    return intermediate;
  }

  private Document getFieldParent(String fieldName) {
    return getFieldParent(fieldName, false);
  }

  /**
   * Compute the name of the leaf field.
   *
   * @param fieldName fully qualified name of the target field
   * @return name of the field at the end of the tree (leaf)
   */
  private String getLeafName(String fieldName) {
    int i = fieldName.lastIndexOf(".");
    if (i < 0)
      return fieldName;
    else
      return fieldName.substring(i + 1);
  }

}
