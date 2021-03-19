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

import org.apache.hadoop.conf.Configuration
import java.lang.Boolean
import java.util.*

/**
 * Configuration properties
 *
 * @author Damien Raude-Morvan
 */
class KMongoStoreParameters
/**
 * @param mappingFile
 * @param servers
 * @param dbname         Name of database to connect to.
 * @param authenticationType Authentication type to login
 * @param login          Optionnal login for remote database.
 * @param secret         Optional secret for remote database.
 * @param readPreference
 * @param writeConcern   @return a DB instance from <tt>mongoClient</tt> or null if
 */ private constructor(
    val mappingFile: String,
    val servers: String,
    val dbname: String,
    val authenticationType: String,
    val login: String,
    val secret: String,
    val readPreference: String,
    val writeConcern: String
) {

    companion object {
        /**
         * Property indicating if the hadoop configuration has priority or not
         */
        const val PROP_OVERRIDING = "gora.mongodb.override_hadoop_configuration"

        /**
         * Property pointing to the file for the mapping
         */
        const val PROP_MAPPING_FILE = "gora.mongodb.mapping.file"

        /**
         * Property pointing to the host where the server is running
         */
        const val PROP_MONGO_SERVERS = "gora.mongodb.servers"

        /**
         * Property pointing to the username to connect to the server
         */
        const val PROP_MONGO_LOGIN = "gora.mongodb.login"

        /**
         * Property pointing to the authentication type to connect to the server
         * @see [AuthenticationMechanism in MongoDB Java Driver](http://api.mongodb.com/java/current/com/mongodb/AuthenticationMechanism.html)
         */
        const val PROP_MONGO_AUTHENTICATION_TYPE = "gora.mongodb.authentication.type"

        /**
         * Property pointing to the secret to connect to the server
         */
        const val PROP_MONGO_SECRET = "gora.mongodb.secret"

        /**
         * Property pointing to MongoDB Read Preference value.
         *
         * @see [Read Preference in MongoDB Documentation](http://docs.mongodb.org/manual/core/read-preference/)
         *
         * @see [ReadPreference in MongoDB Java Driver](http://api.mongodb.org/java/current/com/mongodb/ReadPreference.html)
         */
        const val PROP_MONGO_READPREFERENCE = "gora.mongodb.readpreference"

        /**
         * Property pointing to MongoDB Write Concern value.
         *
         * @see [Write Concern in MongoDB Documentation](http://docs.mongodb.org/manual/core/write-concern/)
         *
         * @see [WriteConcern in MongoDB Java Driver](http://api.mongodb.org/java/current/com/mongodb/WriteConcern.html)
         */
        const val PROP_MONGO_WRITECONCERN = "gora.mongodb.writeconcern"

        /**
         * Property to select the database
         */
        const val PROP_MONGO_DB = "gora.mongodb.db"
        fun load(properties: Properties, conf: Configuration): KMongoStoreParameters {
            // Prepare the configuration
            var vPropMappingFile = properties.getProperty(PROP_MAPPING_FILE, MongoStore.DEFAULT_MAPPING_FILE)
            var vPropMongoServers = properties.getProperty(PROP_MONGO_SERVERS)
            var vPropMongoAuthenticationType = properties.getProperty(PROP_MONGO_AUTHENTICATION_TYPE)
            var vPropMongoLogin = properties.getProperty(PROP_MONGO_LOGIN)
            var vPropMongoSecret = properties.getProperty(PROP_MONGO_SECRET)
            var vPropMongoDb = properties.getProperty(PROP_MONGO_DB)
            var vPropMongoRead = properties.getProperty(PROP_MONGO_READPREFERENCE)
            var vPropMongoWrite = properties.getProperty(PROP_MONGO_WRITECONCERN)
            val overrideHadoop = properties.getProperty(PROP_OVERRIDING)
            if (!Boolean.parseBoolean(overrideHadoop)) {
                MongoStore.LOG.debug("Hadoop configuration has priority.")
                vPropMappingFile = conf[PROP_MAPPING_FILE, vPropMappingFile]
                vPropMongoServers = conf[PROP_MONGO_SERVERS, vPropMongoServers]
                vPropMongoAuthenticationType = conf[PROP_MONGO_AUTHENTICATION_TYPE, vPropMongoAuthenticationType]
                vPropMongoLogin = conf[PROP_MONGO_LOGIN, vPropMongoLogin]
                vPropMongoSecret = conf[PROP_MONGO_SECRET, vPropMongoSecret]
                vPropMongoDb = conf[PROP_MONGO_DB, vPropMongoDb]
                vPropMongoRead = conf[PROP_MONGO_READPREFERENCE, vPropMongoRead]
                vPropMongoWrite = conf[PROP_MONGO_WRITECONCERN, vPropMongoWrite]
            }
            return KMongoStoreParameters(
                vPropMappingFile,
                vPropMongoServers,
                vPropMongoDb,
                vPropMongoAuthenticationType,
                vPropMongoLogin,
                vPropMongoSecret,
                vPropMongoRead,
                vPropMongoWrite
            )
        }
    }
}
