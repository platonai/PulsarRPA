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

import org.bson.Document;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

public class TestBSONDecorator {

  @Test
  public void testContainsField() {
    // Init the object used for testing
    Document dbo1 = DocumentBuilder
            .start()
            .add("root0", "value")
            .add("root1", new Document("leaf1", 1))
            .add("root2",
                    new Document("parent1", new Document("leaf2", "test")))
            .get();
    BSONDecorator dboc = new BSONDecorator(dbo1);

    // Root level field, does exist
    assertTrue(dboc.containsField("root0"));
    // Root level field, does not exist
    assertFalse(dboc.containsField("doestNotExist"));

    // 1-deep level field, does exist
    assertTrue(dboc.containsField("root1.leaf1"));
    // 1-deep level field, parent does not exist
    assertFalse(dboc.containsField("doesNotExist.leaf2"));
    // 1-deep level field, leaf does not exist
    assertFalse(dboc.containsField("root1.doestNotExist"));

    // 3-deep level field, does exist
    assertTrue(dboc.containsField("root2.parent1.leaf2"));
    // 3-deep level field, leaf does not exist
    assertFalse(dboc.containsField("root2.parent1.doestNotExist"));
    // 3-deep level field, first parent does not exist
    assertFalse(dboc.containsField("doesNotExist.parent1.leaf2"));
    // 3-deep level field, intermediate parent does not exist
    assertFalse(dboc.containsField("root2.doesNotExist.leaf2"));
  }

  @Test
  public void testBinaryField() {
    // Init the object used for testing
    Document dbo1 = DocumentBuilder
            .start()
            .add("root0", "value")
            .add("root1", new Document("leaf1", "abcdefgh".getBytes(Charset.defaultCharset())))
            .add(
                    "root2",
                    new Document("parent1", new Document("leaf2", "test"
                            .getBytes(Charset.defaultCharset()))))
            .add("root3", ByteBuffer.wrap("test2".getBytes(Charset.defaultCharset()))).get();
    BSONDecorator dboc = new BSONDecorator(dbo1);

    // Access first bytes field
    assertTrue(dboc.containsField("root1.leaf1"));
    assertArrayEquals("abcdefgh".getBytes(Charset.defaultCharset()), dboc.getBytes("root1.leaf1")
            .array());

    // Access second bytes field
    assertTrue(dboc.containsField("root2.parent1.leaf2"));
    assertArrayEquals("test".getBytes(Charset.defaultCharset()), dboc.getBytes("root2.parent1.leaf2")
            .array());

    // Access third bytes field
    assertTrue(dboc.containsField("root3"));
    assertArrayEquals("test2".getBytes(Charset.defaultCharset()), dboc.getBytes("root3").array());
  }

  @Test
  public void testNullStringField() {
    // Init the object used for testing
    Document dbo1 = DocumentBuilder
            .start()
            .add("key1", null)
            .get();
    BSONDecorator dboc = new BSONDecorator(dbo1);

    assertTrue(dboc.containsField("key1"));
    assertNull(dboc.getUtf8String("key1"));

    assertFalse(dboc.containsField("key2"));
  }

  @Test
  public void testNullFields() {
    BSONDecorator dboc = new BSONDecorator(new Document());

    assertNull(dboc.getInt("key1"));
    assertNull(dboc.getLong("key1"));
    assertNull(dboc.getDouble("key1"));
    assertNull(dboc.getUtf8String("key1"));
    assertNull(dboc.getBoolean("key1"));
    assertNull(dboc.getBytes("key1"));
    assertNull(dboc.getDate("key1"));

    assertNull(dboc.getDBObject("key1"));
    assertNull(dboc.getDBList("key1"));
  }
}
