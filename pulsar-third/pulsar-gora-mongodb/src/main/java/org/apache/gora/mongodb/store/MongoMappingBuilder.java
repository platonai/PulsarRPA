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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.gora.persistency.impl.PersistentBase;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * A builder for creating the mapper. This will allow building a thread safe
 * {@link MongoMapping} using simple immutabilty.
 */
public class MongoMappingBuilder<K, T extends PersistentBase> {

  // Document description
  static final String TAG_DOCUMENT = "document";

  static final String ATT_COLLECTION = "collection";

  static final String STAG_DOCUMENT_FIELD = "field";

  static final String ATT_NAME = "name";

  static final String ATT_TYPE = "type";

  static final String STAG_SUBDOCUMENT = "subdocument";

  // Class description
  static final String TAG_CLASS = "class";

  static final String ATT_KEYCLASS = "keyClass";

  static final String ATT_DOCUMENT = "document";

  static final String TAG_FIELD = "field";

  static final String ATT_FIELD = "docfield";

  private final MongoStore<K, T> dataStore;

  /**
   * Mapping instance being built
   */
  private MongoMapping mapping;

  public MongoMappingBuilder(final MongoStore<K, T> store) {
    this.dataStore = store;
    this.mapping = new MongoMapping();
  }

  /**
   * Return the built mapping if it is in a legal state
   */
  public MongoMapping build() {
    if (mapping.getCollectionName() == null)
      throw new IllegalStateException("A collection is not specified");
    return mapping;
  }

  /**
   * Load the {@link MongoMapping} from a file
   * passed in parameter.
   * 
   * @param uri
   *          path to the file holding the mapping
   * @throws IOException
   */
  protected void fromFile(String uri) throws IOException {

    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      InputStream is = getClass().getResourceAsStream(uri);
      if (is == null) {
        String msg = "Unable to load the mapping from resource '" + uri
            + "' as it does not appear to exist! " + "Trying local file.";
        MongoStore.LOG.warn(msg);
        is = new FileInputStream(uri);
      }
      Document doc = saxBuilder.build(is);
      Element root = doc.getRootElement();
      // No need to use documents descriptions for now...
      // Extract class descriptions
      @SuppressWarnings("unchecked")
      List<Element> classElements = root.getChildren(TAG_CLASS);
      for (Element classElement : classElements) {
        final Class<T> persistentClass = dataStore.getPersistentClass();
        final Class<K> keyClass = dataStore.getKeyClass();
        if (haveKeyClass(keyClass, classElement)
            && havePersistentClass(persistentClass, classElement)) {
          loadPersistentClass(classElement, persistentClass);
          break; // only need that
        }
      }
    } catch (IOException ex) {
      MongoStore.LOG.error(ex.getMessage());
      MongoStore.LOG.error(ex.getStackTrace().toString());
      throw ex;
    } catch (Exception ex) {
      MongoStore.LOG.error(ex.getMessage());
      MongoStore.LOG.error(ex.getStackTrace().toString());
      throw new IOException(ex);
    }
  }

  private boolean havePersistentClass(final Class<T> persistentClass,
      final Element classElement) {
    return classElement.getAttributeValue(ATT_NAME).equals(
        persistentClass.getName());
  }

  private boolean haveKeyClass(final Class<K> keyClass,
      final Element classElement) {
    return classElement.getAttributeValue(ATT_KEYCLASS).equals(
        keyClass.getName());
  }

  /**
   * Handle the XML parsing of the class definition.
   * 
   * @param classElement
   *          the XML node containing the class definition
   */
  protected void loadPersistentClass(Element classElement,
      Class<T> pPersistentClass) {

    String docNameFromMapping = classElement.getAttributeValue(ATT_DOCUMENT);
    String collName = dataStore.getSchemaName(docNameFromMapping,
        pPersistentClass);

    mapping.setCollectionName(collName);
    // docNameFromMapping could be null here
    if (!collName.equals(docNameFromMapping)) {
      MongoStore.LOG
      .info("Keyclass and nameclass match but mismatching table names "
          + " mappingfile schema is '" + docNameFromMapping
          + "' vs actual schema '" + collName
          + "' , assuming they are the same.");
      if (docNameFromMapping != null) {
        mapping.renameCollection(docNameFromMapping, collName);
      }
    }

    // Process fields declaration
    @SuppressWarnings("unchecked")
    List<Element> fields = classElement.getChildren(TAG_FIELD);
    for (Element field : fields) {
      mapping
      .addClassField(field.getAttributeValue(ATT_NAME),
          field.getAttributeValue(ATT_FIELD),
          field.getAttributeValue(ATT_TYPE));
    }
  }

}
