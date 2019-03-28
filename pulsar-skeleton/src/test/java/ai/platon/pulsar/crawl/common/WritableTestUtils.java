/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.platon.pulsar.crawl.common;

import ai.platon.pulsar.common.config.MutableConfig;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.Writable;

import static org.junit.Assert.assertEquals;

public class WritableTestUtils {

    public static MutableConfig defaultConf = new MutableConfig();

    /**
     * Utility method for testing writables.
     */
    public static void testWritable(Writable before) throws Exception {
        testWritable(before, defaultConf);
    }

    /**
     * Utility method for testing writables.
     */
    public static void testWritable(Writable before, MutableConfig conf) throws Exception {
        assertEquals(before, writeRead(before, conf));
    }

    /**
     * Utility method for testing writables.
     */
    public static Writable writeRead(Writable before, MutableConfig conf) throws Exception {
        DataOutputBuffer dob = new DataOutputBuffer();
        before.write(dob);

        DataInputBuffer dib = new DataInputBuffer();
        dib.reset(dob.getData(), dob.getLength());

        Writable after = before.getClass().newInstance();
        ((Configurable) after).setConf(conf.unbox());
        after.readFields(dib);
        return after;
    }
}
