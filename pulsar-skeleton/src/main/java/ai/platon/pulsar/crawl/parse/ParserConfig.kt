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
package ai.platon.pulsar.crawl.parse;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a natural ordering for which parsing plugin should get
 * called for a particular mimeType. It provides methods to store the
 * parse-plugins.xml data, and methods to retreive the name of the appropriate
 * parsing plugin for a contentType.
 *
 * @author mattmann
 * @version 1.0
 */
class ParserConfig {

    /* a map to link mimeType to an ordered list of parsing plugins */
    private Map<String, List<String>> mimeType2ParserClasses = new HashMap<>();

    /* Aliases to class */
    private Map<String, String> aliases = new HashMap<>();

    public ParserConfig() {
    }

    public void setParsers(String mimeType, List<String> classes) {
        mimeType2ParserClasses.put(mimeType, classes);
    }

    public Map<String, List<String>> getParsers() {
        return mimeType2ParserClasses;
    }

    public List<String> getParsers(String mimeType) {
        return mimeType2ParserClasses.get(mimeType);
    }

    public String getClassName(String aliase) {
        return aliases.get(aliase);
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public void setAliases(Map<String, String> aliases) {
        this.aliases = aliases;
    }

    public List<String> getSupportedMimeTypes() {
        return Lists.newArrayList(mimeType2ParserClasses.keySet());
    }

    @Override
    public String toString() {
        return mimeType2ParserClasses.toString();
    }
}
