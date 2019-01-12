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

package fun.platonic.plusar.gora.hbase.store;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.gora.util.AvroUtils;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains utility methods for byte[] {@literal <->} field conversions.
 */
public class HBaseByteInterface {
  /**
   * Threadlocals maintaining reusable binary decoders and encoders.
   */
  private static ThreadLocal<ByteArrayOutputStream> outputStream =
      new ThreadLocal<>();
  
  public static final ThreadLocal<BinaryDecoder> decoders =
      new ThreadLocal<>();
  public static final ThreadLocal<BinaryEncoder> encoders =
      new ThreadLocal<>();
  /*
   * Create a threadlocal map for the datum readers and writers, because
   * they are not thread safe, at least not before Avro 1.4.0 (See AVRO-650).
   * When they are thread safe, it is possible to maintain a single reader and
   * writer pair for every schema, instead of one for every thread.
   */
  
  public static final ConcurrentHashMap<String, SpecificDatumReader<?>> readerMap = 
      new ConcurrentHashMap<>();
     
  public static final ConcurrentHashMap<String, SpecificDatumWriter<?>> writerMap = 
      new ConcurrentHashMap<>();

  /**
   * Deserializes an array of bytes matching the given schema to the proper basic 
   * (enum, Utf8,...) or complex type (Persistent/Record).
   * 
   * Does not handle <code>arrays/maps</code> if not inside a <code>record</code> type.
   * 
   * @param schema Avro schema describing the expected data
   * @param val array of bytes with the data serialized
   * @return Enum|Utf8|ByteBuffer|Integer|Long|Float|Double|Boolean|Persistent|Null
   * @throws IOException
   */
  @SuppressWarnings({ "rawtypes" })
  public static Object fromBytes(Schema schema, byte[] val) throws IOException {
    Type type = schema.getType();
    switch (type) {
    case ENUM:    return AvroUtils.getEnumValue(schema, val[0]);
    case STRING:  return new Utf8(Bytes.toString(val));
    case BYTES:   return ByteBuffer.wrap(val);
    case INT:     return Bytes.toInt(val);
    case LONG:    return Bytes.toLong(val);
    case FLOAT:   return Bytes.toFloat(val);
    case DOUBLE:  return Bytes.toDouble(val);
    case BOOLEAN: return val[0] != 0;
    case UNION:
      // XXX Special case: When reading the top-level field of a record we must handle the
      // special case ["null","type"] definitions: this will be written as if it was ["type"]
      // if not in a special case, will execute "case RECORD".
      
      // if 'val' is empty we ignore the special case (will match Null in "case RECORD")  
      if (schema.getTypes().size() == 2) {
        
        // schema [type0, type1]
        Type type0 = schema.getTypes().get(0).getType() ;
        Type type1 = schema.getTypes().get(1).getType() ;
        
        // Check if types are different and there's a "null", like ["null","type"] or ["type","null"]
        if (!type0.equals(type1)
            && (   type0.equals(Type.NULL)
                || type1.equals(Type.NULL))) {

          if (type0.equals(Type.NULL))
            schema = schema.getTypes().get(1) ;
          else
            schema = schema.getTypes().get(0) ;

          return fromBytes(schema, val) ; // Deserialize as if schema was ["type"]
        }

      }
      // else
      //   type = [type0,type1] where type0=type1
      //   or val == null
      // => deserialize like "case RECORD"

    case RECORD:
      // For UNION schemas, must use a specific SpecificDatumReader
      // from the readerMap since unions don't have own name
      // (key name in map will be "UNION-type-type-...")
      String schemaId = schema.getType().equals(Type.UNION) ? String.valueOf(schema.hashCode()) : schema.getFullName();
      
      SpecificDatumReader<?> reader = readerMap.get(schemaId);
      if (reader == null) {
        reader = new SpecificDatumReader(schema);// ignore dirty bits
        SpecificDatumReader localReader=null;
        if((localReader=readerMap.putIfAbsent(schemaId, reader))!=null) {
          reader = localReader;
        }
      }
      
      // initialize a decoder, possibly reusing previous one
      BinaryDecoder decoderFromCache = decoders.get();
      BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(val, null);
      // put in threadlocal cache if the initial get was empty
      if (decoderFromCache==null) {
        decoders.set(decoder);
      }
      return reader.read(null, decoder);
    default: throw new RuntimeException("Unknown type: "+type);
    }
  }

  /**
   * Converts an array of bytes to the target <em>basic class</em>.
   * @param clazz (Byte|Boolean|Short|Integer|Long|Float|Double|String|Utf8).class
   * @param val array of bytes with the value
   * @return an instance of <code>clazz</code> with the bytes in <code>val</code>
   *         deserialized with org.apache.hadoop.hbase.util.Bytes
   */
  @SuppressWarnings("unchecked")
  public static <K> K fromBytes(Class<K> clazz, byte[] val) {
    if (clazz.equals(Byte.TYPE) || clazz.equals(Byte.class)) {
      return (K) Byte.valueOf(val[0]);
    } else if (clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class)) {
      return (K) Boolean.valueOf(val[0] != 0);
    } else if (clazz.equals(Short.TYPE) || clazz.equals(Short.class)) {
      return (K) Short.valueOf(Bytes.toShort(val));
    } else if (clazz.equals(Integer.TYPE) || clazz.equals(Integer.class)) {
      return (K) Integer.valueOf(Bytes.toInt(val));
    } else if (clazz.equals(Long.TYPE) || clazz.equals(Long.class)) {
      return (K) Long.valueOf(Bytes.toLong(val));
    } else if (clazz.equals(Float.TYPE) || clazz.equals(Float.class)) {
      return (K) Float.valueOf(Bytes.toFloat(val));
    } else if (clazz.equals(Double.TYPE) || clazz.equals(Double.class)) {
      return (K) Double.valueOf(Bytes.toDouble(val));
    } else if (clazz.equals(String.class)) {
      return (K) Bytes.toString(val);
    } else if (clazz.equals(Utf8.class)) {
      return (K) new Utf8(Bytes.toString(val));
    }
    throw new RuntimeException("Can't parse data as class: " + clazz);
  }

  /**
   * Converts an instance of a <em>basic class</em> to an array of bytes.
   * @param o Instance of Enum|Byte|Boolean|Short|Integer|Long|Float|Double|String|Utf8
   * @return array of bytes with <code>o</code> serialized with org.apache.hadoop.hbase.util.Bytes
   */
  public static byte[] toBytes(Object o) {
    Class<?> clazz = o.getClass();
    if (clazz.equals(Enum.class)) {
      return new byte[] { (byte)((Enum<?>) o).ordinal() }; // yeah, yeah it's a hack
    } else if (clazz.equals(Byte.TYPE) || clazz.equals(Byte.class)) {
      return new byte[] { (Byte) o };
    } else if (clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class)) {
      return new byte[] { ((Boolean) o ? (byte) 1 :(byte) 0)};
    } else if (clazz.equals(Short.TYPE) || clazz.equals(Short.class)) {
      return Bytes.toBytes((Short) o);
    } else if (clazz.equals(Integer.TYPE) || clazz.equals(Integer.class)) {
      return Bytes.toBytes((Integer) o);
    } else if (clazz.equals(Long.TYPE) || clazz.equals(Long.class)) {
      return Bytes.toBytes((Long) o);
    } else if (clazz.equals(Float.TYPE) || clazz.equals(Float.class)) {
      return Bytes.toBytes((Float) o);
    } else if (clazz.equals(Double.TYPE) || clazz.equals(Double.class)) {
      return Bytes.toBytes((Double) o);
    } else if (clazz.equals(String.class)) {
      return Bytes.toBytes((String) o);
    } else if (clazz.equals(Utf8.class)) {
      return ((Utf8) o).getBytes();
    } else if (clazz.isArray() && clazz.getComponentType().equals(Byte.TYPE)) {
      return (byte[])o;
    }
    throw new RuntimeException("Can't parse data as class: " + clazz);
  }

  /**
   * Serializes an object following the given schema.
   * Does not handle <code>array/map</code> if it is not inside a <code>record</code>
   * @param o Utf8|ByteBuffer|Integer|Long|Float|Double|Boolean|Enum|Persistent
   * @param schema The schema describing the object (or a compatible description)
   * @return array of bytes of the serialized object
   * @throws IOException
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static byte[] toBytes(Object o, Schema schema) throws IOException {
    Type type = schema.getType();
    switch (type) {
    case STRING:  return Bytes.toBytes(((CharSequence)o).toString()); // TODO: maybe ((Utf8)o).getBytes(); ?
    case BYTES:   return ((ByteBuffer)o).array();
    case INT:     return Bytes.toBytes((Integer)o);
    case LONG:    return Bytes.toBytes((Long)o);
    case FLOAT:   return Bytes.toBytes((Float)o);
    case DOUBLE:  return Bytes.toBytes((Double)o);
    case BOOLEAN: return (Boolean)o ? new byte[] {1} : new byte[] {0};
    case ENUM:    return new byte[] { (byte)((Enum<?>) o).ordinal() };
    case UNION:
    case RECORD:
      SpecificDatumWriter writer = writerMap.get(schema.getFullName());
      if (writer == null) {
        writer = new SpecificDatumWriter(schema);// ignore dirty bits
        writerMap.put(schema.getFullName(),writer);
      }

      BinaryEncoder encoderFromCache = encoders.get();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      outputStream.set(bos);
      BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(bos, null);
      if (encoderFromCache == null) {
        encoders.set(encoder);
      }

      //reset the buffers
      ByteArrayOutputStream os = outputStream.get();
      os.reset();

      writer.write(o, encoder);
      encoder.flush();
      return os.toByteArray();
    default: throw new RuntimeException("Unknown type: "+type);
    }
  }
}
