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

import static com.mongodb.AuthenticationMechanism.*;
import static org.apache.gora.mongodb.store.MongoMapping.DocumentFieldType;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.print.Doc;
import javax.xml.bind.DatatypeConverter;

import com.google.common.collect.Iterables;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.connection.ServerSettings;
import com.mongodb.internal.bulk.DeleteRequest;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.util.Utf8;
import org.apache.gora.mongodb.filters.MongoFilterUtil;
import org.apache.gora.mongodb.query.MongoDBQuery;
import org.apache.gora.mongodb.query.MongoDBResult;
import org.apache.gora.mongodb.utils.BSONDecorator;
import org.apache.gora.mongodb.utils.GoraCodecRegistry;
import org.apache.gora.persistency.impl.BeanFactoryImpl;
import org.apache.gora.persistency.impl.DirtyListWrapper;
import org.apache.gora.persistency.impl.DirtyMapWrapper;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.query.PartitionQuery;
import org.apache.gora.query.Query;
import org.apache.gora.query.Result;
import org.apache.gora.query.impl.PartitionQueryImpl;
import org.apache.gora.store.impl.DataStoreBase;
import org.apache.gora.util.AvroUtils;
import org.apache.gora.util.ClassLoadingUtils;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.mongodb.*;

/**
 * Implementation of a MongoDB data store to be used by gora.
 * 
 * @param <K>
 *          class to be used for the key
 * @param <T>
 *          class to be persisted within the store
 * @author Fabien Poulard fpoulard@dictanova.com
 * @author Damien Raude-Morvan draudemorvan@dictanova.com
 */
public class MongoStore<K, T extends PersistentBase> extends DataStoreBase<K, T> {

  public static final Logger LOG = LoggerFactory.getLogger(MongoStore.class);

  /**
   * Default value for mapping file
   */
  public static final String DEFAULT_MAPPING_FILE = "/gora-mongodb-mapping.xml";

  /**
   * MongoDB client
   */
  private static ConcurrentHashMap<String, MongoClient> mapsOfClients = new ConcurrentHashMap<>();

  private MongoDatabase mongoClientDB;

  private MongoCollection<Document> mongoClientColl;

  /**
   * Mapping definition for MongoDB
   */
  private MongoMapping mapping;

  private MongoFilterUtil<K, T> filterUtil;

  public MongoStore() {
    // Create a default mapping that will be overriden in initialize method
    this.mapping = new MongoMapping();
  }

  /**
   * Initialize the data store by reading the credentials, setting the client's
   * properties up and reading the mapping file.
   */
  public void initialize(Class<K> keyClass, Class<T> pPersistentClass, Properties properties) {
    try {
      LOG.debug("Initializing MongoDB store");
      MongoStoreParameters parameters = MongoStoreParameters.load(properties, getConf());
      
      super.initialize(keyClass, pPersistentClass, properties);

      LOG.debug("properties {} {} {} {} {}", parameters.getServers(), parameters.getDbname(),
              parameters.getLogin(), parameters.getSecret(),
              parameters.getAuthenticationType()
      );

      filterUtil = new MongoFilterUtil<>(getConf());

      // Load the mapping
      MongoMappingBuilder<K, T> builder = new MongoMappingBuilder<>(this);
      LOG.debug("Initializing Mongo store with mapping {}.", parameters.getMappingFile());
      builder.fromFile(parameters.getMappingFile());
      mapping = builder.build();

      // Prepare MongoDB connection
      mongoClientDB = getDB(parameters);
      mongoClientColl = mongoClientDB
          .getCollection(mapping.getCollectionName());

      LOG.info("Initialized Mongo store for database {} of {}.", parameters.getDbname(), parameters.getServers());
    } catch (IOException e) {
      LOG.error("Error while initializing MongoDB store: {}",
              e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieve a client connected to the MongoDB server to be used.
   *
   * @param params This value should specify the host:port (at least one) for
   *               connecting to remote MongoDB.
   * @return a {@link MongoClient} instance connected to the server
   * @throws UnknownHostException
   */
  private MongoClient getClient(MongoStoreParameters params)
      throws UnknownHostException {
    // Configure options
//    MongoClientOptions.Builder optBuilder = new MongoClientOptions.Builder()
//        .dbEncoderFactory(GoraDBEncoder.FACTORY); // Utf8 serialization!
    MongoClientSettings.Builder optBuilder = MongoClientSettings.builder();

    optBuilder.codecRegistry(GoraCodecRegistry.REGISTRY);
    if (params.getReadPreference() != null) {
      optBuilder.readPreference(ReadPreference.valueOf(params.getReadPreference()));
    }
    if (params.getWriteConcern() != null) {
      optBuilder.writeConcern(WriteConcern.valueOf(params.getWriteConcern()));
    }

    // If configuration contains a login + secret, try to authenticated with DB
    if (params.getLogin() != null && params.getSecret() != null) {
      MongoCredential credential = createCredential(params.getAuthenticationType(),
              params.getLogin(), params.getDbname(), params.getSecret());
      optBuilder.credential(credential);
    }

    // optBuilder.applyConnectionString(new ConnectionString(params.getServers()));

    System.out.println(optBuilder.build().getServerSettings());
    return MongoClients.create(optBuilder.build());
//    // Connect to the Mongo server
//    return new MongoClient(addrs, credentials, optBuilder.build());
  }

  private List<ServerAddress> buildAddresses(String servers) {
    List<ServerAddress> addrs = new ArrayList<>();

    Iterable<String> serversArray = Splitter.on(",").split(servers);
    for (String server : serversArray) {
      Iterator<String> paramsIterator = Splitter.on(":").trimResults().split(server).iterator();
      if (!paramsIterator.hasNext()) {
        // No server, use default
        addrs.add(new ServerAddress());
      } else {
        String host = paramsIterator.next();
        if (paramsIterator.hasNext()) {
          String port = paramsIterator.next();
          addrs.add(new ServerAddress(host, Integer.parseInt(port)));
        } else {
          addrs.add(new ServerAddress(host));
        }
      }
    }

    return addrs;
  }

  /**
   * This method creates credentials according to the Authentication type.
   *
   * @param authenticationType authentication Type (Authentication Mechanism)
   * @param username           username
   * @param database           database
   * @param password           password
   * @return Mongo Crendential
   * @see <a href="http://api.mongodb.com/java/current/com/mongodb/AuthenticationMechanism.html">AuthenticationMechanism in MongoDB Java Driver</a>
   */
  private MongoCredential createCredential(String authenticationType, String username, String database, String password) {
    MongoCredential credential = null;
    if (PLAIN.getMechanismName().equals(authenticationType)) {
      credential = MongoCredential.createPlainCredential(username, database, password.toCharArray());
    } else if (SCRAM_SHA_1.getMechanismName().equals(authenticationType)) {
      credential = MongoCredential.createScramSha1Credential(username, database, password.toCharArray());
//    } else if (MONGODB_CR.getMechanismName().equals(authenticationType)) {
//      credential = MongoCredential.createMongoCRCredential(username, database, password.toCharArray());
    } else if (GSSAPI.getMechanismName().equals(authenticationType)) {
      credential = MongoCredential.createGSSAPICredential(username);
    } else if (MONGODB_X509.getMechanismName().equals(authenticationType)) {
      credential = MongoCredential.createMongoX509Credential(username);
    } else {
      credential = MongoCredential.createCredential(username, database, password.toCharArray());
    }
    return credential;
  }

  /**
   * Get reference to Mongo DB, using credentials if not null.
   */
  private MongoDatabase getDB(MongoStoreParameters parameters) throws UnknownHostException {
    // Get reference to Mongo DB
    if (!mapsOfClients.containsKey(parameters.getServers())) {
      mapsOfClients.put(parameters.getServers(), getClient(parameters));
    }

    return mapsOfClients.get(parameters.getServers()).getDatabase(parameters.getDbname());
  }

  public MongoMapping getMapping() {
    return mapping;
  }

  /**
   * Accessor to the name of the collection used.
   */
  @Override
  public String getSchemaName() {
    return mapping.getCollectionName();
  }

  @Override
  public String getSchemaName(final String mappingSchemaName,
      final Class<?> persistentClass) {
    return super.getSchemaName(mappingSchemaName, persistentClass);
  }

  /**
   * Create a new collection in MongoDB if necessary.
   */
  @Override
  public void createSchema() {
    if (mongoClientDB == null)
      throw new IllegalStateException(
          "Impossible to create the schema as no database has been selected.");
    if (schemaExists()) {
      return;
    }

    // If initialized create the collection
    mongoClientColl = mongoClientDB.getCollection(
        mapping.getCollectionName(), Document.class); // send a DBObject to
    // force creation
    // otherwise creation is deferred
    // mongoClientColl.setDBEncoderFactory(GoraDBEncoder.FACTORY);

    LOG.debug("Collection {} has been created for Mongo instance {}.",
            mapping.getCollectionName(), mongoClientDB.getName());
  }

  /**
   * Drop the collection.
   */
  @Override
  public void deleteSchema() {
    if (mongoClientColl == null)
      throw new IllegalStateException(
          "Impossible to delete the schema as no schema is selected.");
    // If initialized, simply drop the collection
    mongoClientColl.drop();

    LOG.debug("Collection {} has been dropped for Mongo instance {}.",
            mongoClientColl.getNamespace(), mongoClientDB.getName());
  }

  /**
   * Check if the collection already exists or should be created.
   */
  @Override
  public boolean schemaExists() {
    String name = mapping.getCollectionName();
    for (String s : mongoClientDB.listCollectionNames()) {
      if (s.equals(name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Ensure the data is synced to disk.
   */
  @Override
  public void flush() {
    for (MongoClient client : mapsOfClients.values()) {
      // NOT SUPPORTED
      // client.fsync(false);
      LOG.debug("Forced synced of database for Mongo instance {}.", client);
    }
  }

  /**
   * Release the resources linked to this collection
   */
  @Override
  public void close() {
  }

  /**
   * Retrieve an entry from the store with only selected fields.
   * 
   * @param key
   *          identifier of the document in the database
   * @param fields
   *          list of fields to be loaded from the database
   */
  @Override
  public T get(final K key, final String[] fields) {
    String[] dbFields = getFieldsToQuery(fields);
    // Prepare the MongoDB query
    BasicDBObject q = new BasicDBObject("_id", key);
    BasicDBObject proj = new BasicDBObject();
    for (String field : dbFields) {
      String docf = mapping.getDocumentField(field);
      if (docf != null) {
        proj.put(docf, true);
      }
    }

    // Execute the query
//    DBObject res = mongoClientColl.findOne(q, proj);
    Document doc = mongoClientColl.find(q).projection(proj).first();
    // Build the corresponding persistent
    return newInstance(doc, dbFields);
  }

  /**
   * Persist an object into the store.
   * 
   * @param key
   *          identifier of the object in the store
   * @param obj
   *          the object to be inserted
   */
  @Override
  public void put(final K key, final T obj) {
    // Save the object in the database
    if (obj.isDirty()) {
      performPut(key, obj);
    } else {
      LOG.info("Ignored putting object {} in the store as it is neither "
          + "new, neither dirty.", obj);
    }
  }

  /**
   * Update a object that already exists in the store. The object must exist
   * already or the update may fail.
   * 
   * @param key
   *          identifier of the object in the store
   * @param obj
   *          the object to be inserted
   */
  private void performPut(final K key, final T obj) {
    // Build the query to select the object to be updated
    Document qSel = new Document("_id", key);

    // Build the update query
    Document qUpdate = new Document();

    Document qUpdateSet = newUpdateSetInstance(obj);
    if (qUpdateSet.size() > 0) {
      qUpdate.put("$set", qUpdateSet);
    }

    Document qUpdateUnset = newUpdateUnsetInstance(obj);
    if (qUpdateUnset.size() > 0) {
      qUpdate.put("$unset", qUpdateUnset);
    }

    // Execute the update (if there is at least one $set ot $unset
    if (!qUpdate.isEmpty()) {
      UpdateOptions options = new UpdateOptions();
      options.upsert(true);
      mongoClientColl.updateOne(qSel, qUpdate, options);
//      mongoClientColl.update(qSel, qUpdate, true, false);
      obj.clearDirty();
    } else {
      LOG.debug("No update to perform, skip {}", key);
    }
  }

  @Override
  public boolean delete(final K key) {
    Document removeKey = new Document("_id", key);
    DeleteResult result = mongoClientColl.deleteOne(removeKey);
    return result.getDeletedCount() > 0;
//    WriteResult writeResult = mongoClientColl.remove(removeKey);
//    return writeResult != null && writeResult.getN() > 0;
  }

  @Override
  public long deleteByQuery(final Query<K, T> query) {
    // Build the actual MongoDB query
    Document q = MongoDBQuery.toDBQuery(query);
    DeleteResult result = mongoClientColl.deleteMany(q);
    return result.getDeletedCount();
//    WriteResult writeResult = mongoClientColl.remove(q);
//    if (writeResult != null) {
//      return writeResult.getN();
//    }
//    return 0;
  }

  /**
   * Execute the query and return the result.
   */
  @Override
  public Result<K, T> execute(final Query<K, T> query) {

    String[] fields = getFieldsToQuery(query.getFields());
    // Build the actual MongoDB query
    Document q = MongoDBQuery.toDBQuery(query);
    Document p = MongoDBQuery.toProjection(fields, mapping);

    if (query.getFilter() != null) {
      boolean succeeded = filterUtil.setFilter(q, query.getFilter(), this);
      if (succeeded) {
        // don't need local filter
        query.setLocalFilterEnabled(false);
      }
    }

    // Execute the query on the collection
//    DBCursor cursor = mongoClientColl.find(q, p);
//    if (query.getLimit() > 0)
//      cursor = cursor.limit((int) query.getLimit());
//    cursor.batchSize(100);
//    cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);

    int count = (int)mongoClientColl.estimatedDocumentCount();
    FindIterable<Document> it = mongoClientColl.find(q).projection(p);

    MongoCursor<Document> cursor = it.cursor();
    if (query.getLimit() > 0) {
      it.limit((int)query.getLimit());
    }
    it.batchSize(100);
    it.maxAwaitTime(1, TimeUnit.HOURS);

    // Build the result
    MongoDBResult<K, T> mongoResult = new MongoDBResult<>(this, query);
    mongoResult.setCursor(cursor, count);

    return mongoResult;
  }

  /**
   * Create a new {@link Query} to query the datastore.
   */
  @Override
  public Query<K, T> newQuery() {
    MongoDBQuery<K, T> query = new MongoDBQuery<>(this);
    query.setFields(getFieldsToQuery(null));
    return query;
  }

  /**
   * Partitions the given query and returns a list of PartitionQuerys, which
   * will execute on local data.
   */
  @Override
  public List<PartitionQuery<K, T>> getPartitions(final Query<K, T> query)
      throws IOException {
    // FIXME: for now, there is only one partition as we do not handle
    // MongoDB sharding configuration
    List<PartitionQuery<K, T>> partitions = new ArrayList<>();
    PartitionQueryImpl<K, T> partitionQuery = new PartitionQueryImpl<>(
        query);
    partitionQuery.setConf(getConf());
    partitions.add(partitionQuery);
    return partitions;
  }

  // //////////////////////////////////////////////////////// DESERIALIZATION

  /**
   * Build a new instance of the persisted class from the {@link DBObject}
   * retrieved from the database.
   * 
   * @param obj
   *          the {@link DBObject} that results from the query to the database
   * @param fields
   *          the list of fields to be mapped to the persistence class instance
   * @return a persistence class instance which content was deserialized from
   *         the {@link DBObject}
   */
  public T newInstance(final Document obj, final String[] fields) {
    if (obj == null)
      return null;
    BSONDecorator easybson = new BSONDecorator(obj);
    // Create new empty persistent bean instance
    T persistent = newPersistent();
    String[] dbFields = getFieldsToQuery(fields);

    // Populate each field
    for (String f : dbFields) {
      // Check the field exists in the mapping and in the db
      String docf = mapping.getDocumentField(f);
      if (docf == null || !easybson.containsField(docf))
        continue;

      DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
      Field field = fieldMap.get(f);
      Schema fieldSchema = field.schema();

      LOG.debug(
          "Load from DBObject (MAIN), field:{}, schemaType:{}, docField:{}, storeType:{}",
              field.name(), fieldSchema.getType(), docf, storeType);
      Object result = fromDBObject(fieldSchema, storeType, field, docf,
          easybson);
      persistent.put(field.pos(), result);
    }
    persistent.clearDirty();
    return persistent;
  }

  private Object fromDBObject(final Schema fieldSchema,
      final DocumentFieldType storeType, final Field field, final String docf,
      final BSONDecorator easybson) {
    Object result = null;
    switch (fieldSchema.getType()) {
    case MAP:
      result = fromMongoMap(docf, fieldSchema, easybson, field);
      break;
    case ARRAY:
      result = fromMongoList(docf, fieldSchema, easybson, field);
      break;
    case RECORD:
      Document rec = easybson.getDBObject(docf);
      if (rec == null) {
        result = null;
        break;
      }
      result = fromMongoRecord(fieldSchema, docf, rec);
      break;
    case BOOLEAN:
      result = easybson.getBoolean(docf);
      break;
    case DOUBLE:
      result = easybson.getDouble(docf);
      break;
    case FLOAT:
      result = easybson.getDouble(docf).floatValue();
      break;
    case INT:
      result = easybson.getInt(docf);
      break;
    case LONG:
      result = easybson.getLong(docf);
      break;
    case STRING:
      result = fromMongoString(storeType, docf, easybson);
      break;
    case ENUM:
      result = AvroUtils.getEnumValue(fieldSchema, easybson.getUtf8String(docf)
          .toString());
      break;
    case BYTES:
    case FIXED:
      result = easybson.getBytes(docf);
      break;
    case NULL:
      result = null;
      break;
    case UNION:
      result = fromMongoUnion(fieldSchema, storeType, field, docf, easybson);
      break;
    default:
      LOG.warn("Unable to read {}", docf);
      break;
    }
    return result;
  }

  private Object fromMongoUnion(final Schema fieldSchema,
      final DocumentFieldType storeType, final Field field, final String docf,
      final BSONDecorator easybson) {
    Object result;// schema [type0, type1]
    Type type0 = fieldSchema.getTypes().get(0).getType();
    Type type1 = fieldSchema.getTypes().get(1).getType();

    // Check if types are different and there's a "null", like ["null","type"]
    // or ["type","null"]
    if (!type0.equals(type1)
        && (type0.equals(Type.NULL) || type1.equals(Type.NULL))) {
      Schema innerSchema = fieldSchema.getTypes().get(1);
      LOG.debug(
          "Load from DBObject (UNION), schemaType:{}, docField:{}, storeType:{}",
          new Object[] { innerSchema.getType(), docf, storeType });
      // Deserialize as if schema was ["type"]
      result = fromDBObject(innerSchema, storeType, field, docf, easybson);
    } else {
      throw new IllegalStateException(
          "MongoStore doesn't support 3 types union field yet. Please update your mapping");
    }
    return result;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object fromMongoRecord(final Schema fieldSchema, final String docf, final Document rec) {
    Object result;
    BSONDecorator innerBson = new BSONDecorator(rec);
    Class<?> clazz = null;
    try {
      clazz = ClassLoadingUtils.loadClass(fieldSchema.getFullName());
    } catch (ClassNotFoundException e) {
    }
    PersistentBase record = (PersistentBase) new BeanFactoryImpl(keyClass, clazz).newPersistent();
    for (Field recField : fieldSchema.getFields()) {
      Schema innerSchema = recField.schema();
      DocumentFieldType innerStoreType = mapping
          .getDocumentFieldType(innerSchema.getName());
      String innerDocField = mapping.getDocumentField(recField.name()) != null ? mapping
          .getDocumentField(recField.name()) : recField.name();
          String fieldPath = docf + "." + innerDocField;
          LOG.debug(
              "Load from DBObject (RECORD), field:{}, schemaType:{}, docField:{}, storeType:{}",
                  recField.name(), innerSchema.getType(), fieldPath,
                  innerStoreType);
          record.put(
              recField.pos(),
              fromDBObject(innerSchema, innerStoreType, recField, innerDocField,
                  innerBson));
    }
    result = record;
    return result;
  }

  /* pp */ Object fromMongoList(final String docf, final Schema fieldSchema,
      final BSONDecorator easybson, final Field f) {
    List<Document> list = easybson.getDBList(docf);
    List<Object> rlist = new ArrayList<>();
    if (list == null) {
      return new DirtyListWrapper(rlist);
    }

    for (Object item : list) {
      DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
      Object o = fromDBObject(fieldSchema.getElementType(), storeType, f, "item", new BSONDecorator(new Document("item", item)));
      rlist.add(o);
    }

    return new DirtyListWrapper<>(rlist);
  }

  /* pp */ Object fromMongoMap(final String docf, final Schema fieldSchema,
      final BSONDecorator easybson, final Field f) {
    Document map = easybson.getDBObject(docf);
    Map<Utf8, Object> rmap = new HashMap<>();
    if (map == null) {
      return new DirtyMapWrapper(rmap);
    }
    for (Entry<String, Object> e : map.entrySet()) {
      String mapKey = e.getKey();
      String decodedMapKey = decodeFieldKey(mapKey);

      DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
      Object o = fromDBObject(fieldSchema.getValueType(), storeType, f, mapKey,
          new BSONDecorator(map));
      rmap.put(new Utf8(decodedMapKey), o);
    }
    return new DirtyMapWrapper<>(rmap);
  }

  private Object fromMongoString(final DocumentFieldType storeType,
      final String docf, final BSONDecorator easybson) {
    Object result;
    if (storeType == DocumentFieldType.OBJECTID) {
      // Try auto-conversion of BSON data to ObjectId
      // It will work if data is stored as String or as ObjectId
      Object bin = easybson.get(docf);
      if (bin instanceof String) {
        ObjectId id = new ObjectId((String) bin);
        result = new Utf8(id.toString());
      } else {
        result = new Utf8(bin.toString());
      }
    } else if (storeType == DocumentFieldType.DATE) {
      Object bin = easybson.get(docf);
      if (bin instanceof Date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault());
        calendar.setTime((Date) bin);
        result = new Utf8(DatatypeConverter.printDateTime(calendar));
      } else {
        result = new Utf8(bin.toString());
      }
    } else {
      result = easybson.getUtf8String(docf);
    }
    return result;
  }

  // ////////////////////////////////////////////////////////// SERIALIZATION

  /**
   * Build a new instance of {@link DBObject} from the persistence class
   * instance in parameter. Limit the {@link DBObject} to the fields that are
   * dirty and not null, that is the fields that will need to be updated in the
   * store.
   * 
   * @param persistent
   *          a persistence class instance which content is to be serialized as
   *          a {@link DBObject} for use as parameter of a $set operator
   * @return a {@link DBObject} which content corresponds to the fields that
   *         have to be updated... and formatted to be passed in parameter of a
   *         $set operator
   */
  private Document newUpdateSetInstance(final T persistent) {
    Document result = new Document();
    for (Field f : persistent.getSchema().getFields()) {
      if (persistent.isDirty(f.pos()) && (persistent.get(f.pos()) != null)) {
        String docf = mapping.getDocumentField(f.name());
        Object value = persistent.get(f.pos());
        DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
        LOG.debug(
            "Transform value to DBObject (MAIN), docField:{}, schemaType:{}, storeType:{}",
                docf, f.schema().getType(), storeType);
        Object o = toDBObject(docf, f.schema(), f.schema().getType(),
            storeType, value);
        result.put(docf, o);
      }
    }
    return result;
  }

  /**
   * Build a new instance of {@link DBObject} from the persistence class
   * instance in parameter. Limit the {@link DBObject} to the fields that are
   * dirty and null, that is the fields that will need to be updated in the
   * store by being removed.
   * 
   * @param persistent
   *          a persistence class instance which content is to be serialized as
   *          a {@link DBObject} for use as parameter of a $set operator
   * @return a {@link DBObject} which content corresponds to the fields that
   *         have to be updated... and formated to be passed in parameter of a
   *         $unset operator
   */
  private Document newUpdateUnsetInstance(final T persistent) {
    Document result = new Document();
    for (Field f : persistent.getSchema().getFields()) {
      if (persistent.isDirty(f.pos()) && (persistent.get(f.pos()) == null)) {
        String docf = mapping.getDocumentField(f.name());
        Object value = persistent.get(f.pos());
        DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
        LOG.debug(
            "Transform value to DBObject (MAIN), docField:{}, schemaType:{}, storeType:{}",
                docf, f.schema().getType(), storeType);
        Object o = toDBObject(docf, f.schema(), f.schema().getType(),
            storeType, value);
        result.put(docf, o);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private Object toDBObject(final String docf, final Schema fieldSchema,
      final Type fieldType, final DocumentFieldType storeType,
      final Object value) {
    Object result = null;
    switch (fieldType) {
    case MAP:
      if (storeType != null && storeType != DocumentFieldType.DOCUMENT) {
        throw new IllegalStateException(
            "Field "
                + fieldSchema.getType()
                + ": to store a Gora 'map', target Mongo mapping have to be of 'document' type");
      }
      Schema valueSchema = fieldSchema.getValueType();
      result = mapToMongo(docf, (Map<CharSequence, ?>) value, valueSchema,
          valueSchema.getType());
      break;
    case ARRAY:
      if (storeType != null && storeType != DocumentFieldType.LIST) {
        throw new IllegalStateException(
            "Field "
                + fieldSchema.getType()
                + ": To store a Gora 'array', target Mongo mapping have to be of 'list' type");
      }
      Schema elementSchema = fieldSchema.getElementType();
      result = listToMongo(docf, (List<?>) value, elementSchema,
          elementSchema.getType());
      break;
    case BYTES:
      if (value != null) {
        result = ((ByteBuffer) value).array();
      }
      break;
    case INT:
    case LONG:
    case FLOAT:
    case DOUBLE:
    case BOOLEAN:
      result = value;
      break;
    case STRING:
      result = stringToMongo(fieldSchema, storeType, value);
      break;
    case ENUM:
      if (value != null)
        result = value.toString();
      break;
    case RECORD:
      if (value == null)
        break;
      result = recordToMongo(docf, fieldSchema, value);
      break;
    case UNION:
      result = unionToMongo(docf, fieldSchema, storeType, value);
      break;
    case FIXED:
      result = value;
      break;

    default:
      LOG.error("Unknown field type: {}", fieldSchema.getType());
      break;
    }

    return result;
  }

  private Object unionToMongo(final String docf, final Schema fieldSchema,
      final DocumentFieldType storeType, final Object value) {
    Object result;// schema [type0, type1]
    Type type0 = fieldSchema.getTypes().get(0).getType();
    Type type1 = fieldSchema.getTypes().get(1).getType();

    // Check if types are different and there's a "null", like ["null","type"]
    // or ["type","null"]
    if (!type0.equals(type1)
        && (type0.equals(Type.NULL) || type1.equals(Type.NULL))) {
      Schema innerSchema = fieldSchema.getTypes().get(1);
      LOG.debug(
          "Transform value to DBObject (UNION), schemaType:{}, type1:{}, storeType:{}",
          new Object[] { innerSchema.getType(), type1, storeType });
      // Deserialize as if schema was ["type"]
      result = toDBObject(docf, innerSchema, type1, storeType, value);
    } else {
      throw new IllegalStateException(
          "MongoStore doesn't support 3 types union field yet. Please update your mapping");
    }
    return result;
  }

  private BasicDBObject recordToMongo(final String docf,
      final Schema fieldSchema, final Object value) {
    BasicDBObject record = new BasicDBObject();
    for (Field member : fieldSchema.getFields()) {
      Object innerValue = ((PersistentBase) value).get(member.pos());
      String innerDoc = mapping.getDocumentField(member.name());
      Type innerType = member.schema().getType();
      DocumentFieldType innerStoreType = mapping.getDocumentFieldType(innerDoc);
      LOG.debug(
          "Transform value to DBObject (RECORD), docField:{}, schemaType:{}, storeType:{}",
          new Object[] { member.name(), member.schema().getType(),
              innerStoreType });
      record.put(
          member.name(),
          toDBObject(docf, member.schema(), innerType, innerStoreType,
              innerValue));
    }
    return record;
  }

  private Object stringToMongo(final Schema fieldSchema,
      final DocumentFieldType storeType, final Object value) {
    Object result = null;
    if (storeType == DocumentFieldType.OBJECTID) {
      if (value != null) {
        ObjectId id;
        try {
          id = new ObjectId(value.toString());
        } catch (IllegalArgumentException e1) {
          // Unable to parse anything from Utf8 value, throw error
          throw new IllegalStateException("Field " + fieldSchema.getType()
          + ": Invalid string: unable to convert to ObjectId");
        }
        result = id;
      }
    } else if (storeType == DocumentFieldType.DATE) {
      if (value != null) {
        // Try to parse date from Utf8 value
        Calendar calendar = null;
        try {
          // Parse as date + time
          calendar = DatatypeConverter.parseDateTime(value.toString());
        } catch (IllegalArgumentException e1) {
          try {
            // Parse as date only
            calendar = DatatypeConverter.parseDate(value.toString());
          } catch (IllegalArgumentException e2) {
            // No-op
          }
        }
        if (calendar == null) {
          // Unable to parse anything from Utf8 value, throw error
          throw new IllegalStateException("Field " + fieldSchema.getType()
          + ": Invalid date format '" + value + "'");
        }
        result = calendar.getTime();
      }
    } else {
      if (value != null) {
        result = value.toString();
      }
    }
    return result;
  }

  /**
   * Convert a Java Map as used in Gora generated classes to a Map that can
   * safely be serialized into MongoDB.
   * 
   * @param value
   *          the Java Map that must be serialized into a MongoDB object
   * @param fieldType
   *          type of the values within the map
   * @return a {@link BasicDBObject} version of the {@link Map} that can be
   *         safely serialized into MongoDB.
   */
  private BasicDBObject mapToMongo(final String docf,
      final Map<CharSequence, ?> value, final Schema fieldSchema,
      final Type fieldType) {
    BasicDBObject map = new BasicDBObject();
    // Handle null case
    if (value == null)
      return map;

    // Handle regular cases
    for (Entry<CharSequence, ?> e : value.entrySet()) {
      String mapKey = e.getKey().toString();
      String encodedMapKey = encodeFieldKey(mapKey);
      Object mapValue = e.getValue();

      DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
      Object result = toDBObject(docf, fieldSchema, fieldType, storeType,
          mapValue);
      map.put(encodedMapKey, result);
    }

    return map;
  }

  /**
   * Convert a Java {@link GenericArray} as used in Gora generated classes to a
   * List that can safely be serialized into MongoDB.
   * 
   * @param array
   *          the {@link GenericArray} to be serialized
   * @param fieldType
   *          type of the elements within the array
   * @return a {@link BasicDBList} version of the {@link GenericArray} that can
   *         be safely serialized into MongoDB.
   */
  private BasicDBList listToMongo(final String docf, final Collection<?> array,
      final Schema fieldSchema, final Type fieldType) {
    BasicDBList list = new BasicDBList();
    // Handle null case
    if (array == null)
      return list;

    // Handle regular cases
    for (Object item : array) {
      DocumentFieldType storeType = mapping.getDocumentFieldType(docf);
      Object result = toDBObject(docf, fieldSchema, fieldType, storeType, item);
      list.add(result);
    }

    return list;
  }

  // //////////////////////////////////////////////////////// CLEANUP

  /**
   * Ensure Key encoding -&gt; dots replaced with middle dots
   * 
   * @param key
   *          char with only dots.
   * @return encoded string with "\u00B7" chars..
   */
  public String encodeFieldKey(final String key) {
    if (key == null) {
      return null;
    }
    return key.replace(".", "\u00B7");
  }

  /**
   * Ensure Key decoding -&gt; middle dots replaced with dots
   * 
   * @param key
   *          encoded string with "\u00B7" chars.
   * @return Cleanup up char with only dots.
   */
  public String decodeFieldKey(final String key) {
    if (key == null) {
      return null;
    }
    return key.replace("\u00B7", ".");
  }

}
