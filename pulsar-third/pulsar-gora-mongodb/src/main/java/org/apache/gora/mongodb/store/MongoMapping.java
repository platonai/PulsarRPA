/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.mongodb.store;

import static org.apache.gora.mongodb.store.MongoMapping.DocumentFieldType.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * @author Fabien Poulard fpoulard@dictanova.com
 */
public class MongoMapping {

  /**
   * Helper to write useful information into the logs
   */
  public static final Logger LOG = LoggerFactory.getLogger(MongoMapping.class);

  /**
   * Regex corresponding to a valid Mongo document field name
   */
  private Pattern validMongoDocumentField = Pattern.compile("[a-z0-9\\-]+",
      Pattern.CASE_INSENSITIVE);

  /**
   * Name of the collection this mapping is linked to
   */
  private String collectionName;

  /**
   * Mapping between the class fields and the Mongo document fields
   */
  private HashMap<String, String> classToDocument = new HashMap<>();

  /**
   * Mapping between the Mongo document fields and the class fields
   */
  private HashMap<String, String> documentToClass = new HashMap<>();

  /**
   * Mongo document description
   */
  private HashMap<String, DocumentFieldType> documentFields = new HashMap<>();

  /**
   * Change the name of the collection.
   * 
   * @param oldName
   *          previous name
   * @param newName
   *          new name to be used
   */
  public void renameCollection(String oldName, String newName) {
    // FIXME
    collectionName = newName;
  }

  /**
   * Getter for the name of the collection
   */
  public String getCollectionName() {
    return collectionName;
  }

  /**
   * Setter for the name of the collection
   */
  public void setCollectionName(String collName) {
    // FIXME check name is ok
    collectionName = collName;
  }

  /**
   * Handle the registering of a new Mongo document field. This method must
   * check that, in case the field is several document levels deep, the
   * intermediate fields exists and are of a type compatible with this new type.
   * If the parent fields do not exist, they are created.
   * 
   * @param name
   *          name of the field, the various levels are separated from each
   *          other by dots
   * @param type
   *          type of the field
   */
  private void newDocumentField(String name, DocumentFieldType type) {
    // First of all, split the field to identify the various levels
    String[] breadcrumb = name.split("\\.");
    // Process each intermediate field to check they are of Map type
    StringBuilder partialFieldName = new StringBuilder();
    for (int i = 0; i < breadcrumb.length - 1; i++) {
      // Build intermediate field name
      String f = breadcrumb[i];
      if (!isValidFieldName(f))
        throw new IllegalArgumentException("'" + f
            + "' is an invalid field name for a Mongo document");
      partialFieldName.append(f);
      // Check field exists or not and is of valid type
      String intermediateFieldName = partialFieldName.toString();
      if (documentFields.containsKey(intermediateFieldName)) {
        if (!ImmutableList.of(DOCUMENT, LIST).contains(
            documentFields.get(intermediateFieldName)))
          throw new IllegalStateException("The field '" + intermediateFieldName
              + "' is already registered in "
              + "a type not compatible with the new definition of " + "field '"
              + name + "'.");
      } else {
        documentFields.put(intermediateFieldName, DocumentFieldType.DOCUMENT);
      }
      partialFieldName.append(".");
    }
    // Check the field does not already exist, insert the complete field
    if (documentFields.containsKey(name) && (documentFields.get(name) != type))
      throw new IllegalStateException("The field '" + name + "' is already "
          + "registered with a different type.");
    documentFields.put(name, type);
  }

  /**
   * Check that the field matches the valid field definition for a Mongo
   * document.
   * 
   * @param f
   *          name of the field to be checked
   * @return true if the parameter is a valid field name for a Mongo document
   */
  private boolean isValidFieldName(String f) {
    return validMongoDocumentField.matcher(f).matches();
  }

  /**
   * Register a new mapping between a field from the persisted class to a
   * MongoDocument field.
   * 
   * @param classFieldName
   *          name of the field in the persisted class
   * @param docFieldName
   *          name of the field in the mondo document
   * @param fieldType
   *          type of the field
   */
  public void addClassField(String classFieldName,
      String docFieldName, String fieldType) {
    try {
      // Register a new field for the mongo document
      newDocumentField(docFieldName, valueOf(fieldType.toUpperCase(Locale.getDefault())));
    } catch (final IllegalArgumentException e) {
      throw new IllegalStateException("Declared '" + fieldType
          + "' for class field '" + classFieldName
          + "' is not supported by MongoMapping");
    }
    // Register the mapping
    if (classToDocument.containsKey(classFieldName)) {
      if (!classToDocument.get(classFieldName).equals(docFieldName)) {
        throw new IllegalStateException("The class field '" + classFieldName
            + "' is already registered in the mapping"
            + " with the document field '"
            + classToDocument.get(classFieldName)
            + " which differs from the new one '" + docFieldName + "'.");
      }
    } else {
      classToDocument.put(classFieldName, docFieldName);
      documentToClass.put(docFieldName, classFieldName);
    }
  }

  /**
   * Given a field from the persistence class, return the corresponding field in
   * the document.
   * 
   * @param field
   * @return
   */
  public String getDocumentField(String field) {
    return classToDocument.get(field);
  }

  /**
   * Package private method to retrieve the type of a document field.
   */
  protected DocumentFieldType getDocumentFieldType(String field) {
    return documentFields.get(field);
  }

  /**
   * Accepted types of data to be mapped in mongo based on BSON types
   */
  public static enum DocumentFieldType {
    BINARY, // bytes
    BOOLEAN, // bytes
    INT32, // 4 bytes, signed integer
    INT64, // 8 bytes, signed integer
    DOUBLE, // 8 bytes, float
    STRING, // string
    DATE, // date
    LIST, // a list
    DOCUMENT, // a subdocument
    OBJECTID // ObjectId is a 12-byte BSON type
  }
}
