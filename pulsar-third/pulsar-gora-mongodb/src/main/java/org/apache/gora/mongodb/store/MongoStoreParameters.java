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

import org.apache.hadoop.conf.Configuration;

import java.util.Properties;

/**
 * Configuration properties
 *
 * @author Damien Raude-Morvan
 */
public class MongoStoreParameters {

  /**
   * Property indicating if the hadoop configuration has priority or not
   */
  public static final String PROP_OVERRIDING = "gora.mongodb.override_hadoop_configuration";

  /**
   * Property pointing to the file for the mapping
   */
  public static final String PROP_MAPPING_FILE = "gora.mongodb.mapping.file";

  /**
   * Property pointing to the host where the server is running
   */
  public static final String PROP_MONGO_SERVERS = "gora.mongodb.servers";

  /**
   * Property pointing to the username to connect to the server
   */
  public static final String PROP_MONGO_LOGIN = "gora.mongodb.login";


  /**
   * Property pointing to the authentication type to connect to the server
   * @see <a href="http://api.mongodb.com/java/current/com/mongodb/AuthenticationMechanism.html">AuthenticationMechanism in MongoDB Java Driver</a>
   */
  public static final String PROP_MONGO_AUTHENTICATION_TYPE = "gora.mongodb.authentication.type";

  /**
   * Property pointing to the secret to connect to the server
   */
  public static final String PROP_MONGO_SECRET = "gora.mongodb.secret";

  /**
   * Property pointing to MongoDB Read Preference value.
   *
   * @see <a href="http://docs.mongodb.org/manual/core/read-preference/">Read Preference in MongoDB Documentation</a>
   * @see <a href="http://api.mongodb.org/java/current/com/mongodb/ReadPreference.html">ReadPreference in MongoDB Java Driver</a>
   */
  public static final String PROP_MONGO_READPREFERENCE = "gora.mongodb.readpreference";

  /**
   * Property pointing to MongoDB Write Concern value.
   *
   * @see <a href="http://docs.mongodb.org/manual/core/write-concern/">Write Concern in MongoDB Documentation</a>
   * @see <a href="http://api.mongodb.org/java/current/com/mongodb/WriteConcern.html">WriteConcern in MongoDB Java Driver</a>
   */
  public static final String PROP_MONGO_WRITECONCERN = "gora.mongodb.writeconcern";

  /**
   * Property to select the database
   */
  public static final String PROP_MONGO_DB = "gora.mongodb.db";

  private final String mappingFile;
  private final String servers;
  private final String authenticationType;
  private final String dbname;
  private final String login;
  private final String secret;
  private final String readPreference;
  private final String writeConcern;

  /**
   * @param mappingFile
   * @param servers
   * @param dbname         Name of database to connect to.
   * @param authenticationType Authentication type to login
   * @param login          Optionnal login for remote database.
   * @param secret         Optional secret for remote database.
   * @param readPreference
   * @param writeConcern   @return a DB instance from <tt>mongoClient</tt> or null if
   */
  private MongoStoreParameters(String mappingFile, String servers, String dbname, String authenticationType, String login, String secret, String readPreference, String writeConcern) {
    this.mappingFile = mappingFile;
    this.servers = servers;
    this.dbname = dbname;
    this.authenticationType = authenticationType;
    this.login = login;
    this.secret = secret;
    this.readPreference = readPreference;
    this.writeConcern = writeConcern;
  }

  public String getMappingFile() {
    return mappingFile;
  }

  public String getServers() {
    return servers;
  }

  public String getDbname() {
    return dbname;
  }

  public String getLogin() {
    return login;
  }

  public String getAuthenticationType() {
    return authenticationType;
  }

  public String getSecret() {
    return secret;
  }

  public String getReadPreference() {
    return readPreference;
  }

  public String getWriteConcern() {
    return writeConcern;
  }

  public static MongoStoreParameters load(Properties properties, Configuration conf) {
    // Prepare the configuration
    String vPropMappingFile = properties.getProperty(PROP_MAPPING_FILE, MongoStore.DEFAULT_MAPPING_FILE);
    String vPropMongoServers = properties.getProperty(PROP_MONGO_SERVERS);
    String vPropMongoAuthenticationType = properties.getProperty(PROP_MONGO_AUTHENTICATION_TYPE);
    String vPropMongoLogin = properties.getProperty(PROP_MONGO_LOGIN);
    String vPropMongoSecret = properties.getProperty(PROP_MONGO_SECRET);
    String vPropMongoDb = properties.getProperty(PROP_MONGO_DB);
    String vPropMongoRead = properties.getProperty(PROP_MONGO_READPREFERENCE);
    String vPropMongoWrite = properties.getProperty(PROP_MONGO_WRITECONCERN);
    String overrideHadoop = properties.getProperty(PROP_OVERRIDING);
    if (!Boolean.parseBoolean(overrideHadoop)) {
      MongoStore.LOG.debug("Hadoop configuration has priority.");
      vPropMappingFile = conf.get(PROP_MAPPING_FILE, vPropMappingFile);
      vPropMongoServers = conf.get(PROP_MONGO_SERVERS, vPropMongoServers);
      vPropMongoAuthenticationType = conf.get(PROP_MONGO_AUTHENTICATION_TYPE, vPropMongoAuthenticationType);
      vPropMongoLogin = conf.get(PROP_MONGO_LOGIN, vPropMongoLogin);
      vPropMongoSecret = conf.get(PROP_MONGO_SECRET, vPropMongoSecret);
      vPropMongoDb = conf.get(PROP_MONGO_DB, vPropMongoDb);
      vPropMongoRead = conf.get(PROP_MONGO_READPREFERENCE, vPropMongoRead);
      vPropMongoWrite = conf.get(PROP_MONGO_WRITECONCERN, vPropMongoWrite);
    }
    return new MongoStoreParameters(vPropMappingFile, vPropMongoServers, vPropMongoDb, vPropMongoAuthenticationType, vPropMongoLogin, vPropMongoSecret, vPropMongoRead, vPropMongoWrite);
  }
}
