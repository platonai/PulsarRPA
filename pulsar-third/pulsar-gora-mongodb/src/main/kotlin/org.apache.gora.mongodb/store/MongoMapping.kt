/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gora.mongodb.store

import com.google.common.collect.ImmutableList
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

/**
 * @author Fabien Poulard fpoulard@dictanova.com
 */
class KMongoMapping {
    /**
     * Regex corresponding to a valid Mongo document field name
     */
    private val validMongoDocumentField = Pattern.compile(
        "[a-z0-9\\-]+",
        Pattern.CASE_INSENSITIVE
    )
    /**
     * Getter for the name of the collection
     */// FIXME check name is ok
    /**
     * Setter for the name of the collection
     */
    /**
     * Name of the collection this mapping is linked to
     */
    var collectionName: String? = null

    /**
     * Mapping between the class fields and the Mongo document fields
     */
    private val classToDocument = HashMap<String, String>()

    /**
     * Mapping between the Mongo document fields and the class fields
     */
    private val documentToClass = HashMap<String, String>()

    /**
     * Mongo document description
     */
    private val documentFields = HashMap<String, DocumentFieldType>()

    /**
     * Change the name of the collection.
     *
     * @param oldName
     * previous name
     * @param newName
     * new name to be used
     */
    fun renameCollection(oldName: String?, newName: String?) {
        // FIXME
        collectionName = newName
    }

    /**
     * Handle the registering of a new Mongo document field. This method must
     * check that, in case the field is several document levels deep, the
     * intermediate fields exists and are of a type compatible with this new type.
     * If the parent fields do not exist, they are created.
     *
     * @param name
     * name of the field, the various levels are separated from each
     * other by dots
     * @param type
     * type of the field
     */
    private fun newDocumentField(name: String, type: DocumentFieldType) {
        // First of all, split the field to identify the various levels
        val breadcrumb = name.split("\\.".toRegex()).toTypedArray()
        // Process each intermediate field to check they are of Map type
        val partialFieldName = StringBuilder()
        for (i in 0 until breadcrumb.size - 1) {
            // Build intermediate field name
            val f = breadcrumb[i]
            require(isValidFieldName(f)) {
                ("'" + f
                        + "' is an invalid field name for a Mongo document")
            }
            partialFieldName.append(f)
            // Check field exists or not and is of valid type
            val intermediateFieldName = partialFieldName.toString()
            if (documentFields.containsKey(intermediateFieldName)) {
                check(
                    ImmutableList.of(
                        DocumentFieldType.DOCUMENT,
                        DocumentFieldType.LIST
                    ).contains(
                        documentFields[intermediateFieldName]
                    )
                ) {
                    ("The field '" + intermediateFieldName
                            + "' is already registered in "
                            + "a type not compatible with the new definition of " + "field '"
                            + name + "'.")
                }
            } else {
                documentFields[intermediateFieldName] = DocumentFieldType.DOCUMENT
            }
            partialFieldName.append(".")
        }
        // Check the field does not already exist, insert the complete field
        check(!(documentFields.containsKey(name) && documentFields[name] != type)) {
            ("The field '" + name + "' is already "
                    + "registered with a different type.")
        }
        documentFields[name] = type
    }

    /**
     * Check that the field matches the valid field definition for a Mongo
     * document.
     *
     * @param f
     * name of the field to be checked
     * @return true if the parameter is a valid field name for a Mongo document
     */
    private fun isValidFieldName(f: String): Boolean {
        return validMongoDocumentField.matcher(f).matches()
    }

    /**
     * Register a new mapping between a field from the persisted class to a
     * MongoDocument field.
     *
     * @param classFieldName
     * name of the field in the persisted class
     * @param docFieldName
     * name of the field in the mondo document
     * @param fieldType
     * type of the field
     */
    fun addClassField(
        classFieldName: String,
        docFieldName: String, fieldType: String
    ) {
        try {
            // Register a new field for the mongo document
            newDocumentField(docFieldName, DocumentFieldType.valueOf(fieldType.toUpperCase(Locale.getDefault())))
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(
                "Declared '" + fieldType
                        + "' for class field '" + classFieldName
                        + "' is not supported by MongoMapping"
            )
        }
        // Register the mapping
        if (classToDocument.containsKey(classFieldName)) {
            check(classToDocument[classFieldName] == docFieldName) {
                ("The class field '" + classFieldName
                        + "' is already registered in the mapping"
                        + " with the document field '"
                        + classToDocument[classFieldName]
                        + " which differs from the new one '" + docFieldName + "'.")
            }
        } else {
            classToDocument[classFieldName] = docFieldName
            documentToClass[docFieldName] = classFieldName
        }
    }

    /**
     * Given a field from the persistence class, return the corresponding field in
     * the document.
     *
     * @param field
     * @return
     */
    fun getDocumentField(field: String): String? {
        return classToDocument[field]
    }

    /**
     * Package private method to retrieve the type of a document field.
     */
    fun getDocumentFieldType(field: String): DocumentFieldType? {
        return documentFields[field]
    }

    /**
     * Accepted types of data to be mapped in mongo based on BSON types
     */
    enum class DocumentFieldType {
        BINARY,  // bytes
        BOOLEAN,  // bytes
        INT32,  // 4 bytes, signed integer
        INT64,  // 8 bytes, signed integer
        DOUBLE,  // 8 bytes, float
        STRING,  // string
        DATE,  // date
        LIST,  // a list
        DOCUMENT,  // a subdocument
        OBJECTID // ObjectId is a 12-byte BSON type
    }

    companion object {
        /**
         * Helper to write useful information into the logs
         */
        val LOG = LoggerFactory.getLogger(MongoMapping::class.java)
    }
}