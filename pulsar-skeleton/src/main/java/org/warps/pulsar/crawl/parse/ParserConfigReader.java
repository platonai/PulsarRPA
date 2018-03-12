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
package org.warps.pulsar.crawl.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.warps.pulsar.common.ResourceLoader;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static org.warps.pulsar.common.config.CapabilityTypes.PULSAR_CONFIG_RESOURCE_PREFIX;

/**
 * A reader to load the information stored in the
 * <code>$PULSAR_HOME/conf/parse-plugins.xml</code> file.
 *
 * @author mattmann
 * @version 1.0
 */
public class ParserConfigReader {

    /* our log stream */
    public static final Logger LOG = LoggerFactory.getLogger(ParserConfigReader.class);

    /** The property name of the parse-plugins location */
    public static final String PARSE_PLUGINS_FILE = "parse.plugin.file";

    /** the parse-plugins file */
    private String parseConfigFile = null;

    private List<String> notDefinedParsers = new LinkedList<>();

    /**
     * Reads the <code>parse-plugins.xml</code> file and returns the
     * {@link ParserConfig} defined by it.
     *
     * @return A {@link ParserConfig} specified by the
     *         <code>parse-plugins.xml</code> file.
     */
    @Nonnull
    public ParserConfig parse(ImmutableConfig conf) {
        ParserConfig parserConfig = new ParserConfig();

        String resourcePrefix = conf.get(PULSAR_CONFIG_RESOURCE_PREFIX, "");
        String fileResource = conf.get(PARSE_PLUGINS_FILE, "parse-plugins.xml");

        Document document;
        try (Reader reader = new ResourceLoader().getResourceAsReader(fileResource, resourcePrefix)) {
            InputSource inputSource = new InputSource(reader);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            document = parser.parse(inputSource);
        } catch (IOException e) {
            LOG.error("Failed to find resource " + fileResource);
            return parserConfig;
        } catch (ParserConfigurationException | SAXException e) {
            LOG.warn("Unable to parse [" + parseConfigFile + "]." + "Reason is [" + e + "]");
            return parserConfig;
        }

        Element parsePlugins = document.getDocumentElement();

        // build up the alias hash map
        Map<String, String> aliases = getAliases(parsePlugins);
        // And store it on the parse plugin list
        parserConfig.setAliases(aliases);

        // get all the mime type nodes
        NodeList mimeTypes = parsePlugins.getElementsByTagName("mimeType");

        // iterate through the mime types
        for (int i = 0; i < mimeTypes.getLength(); i++) {
            Element mimeType = (Element) mimeTypes.item(i);
            String mimeTypeStr = mimeType.getAttribute("name");

            // for each mimeType, get the plugin list
            NodeList parserNodes = mimeType.getElementsByTagName("parser");

            // iterate through the plugins, add them in order read
            // OR if they have a special order="" attribute, then hold those in
            // a separate list, and then insert them into the final list at the
            // order specified
            if (parserNodes != null && parserNodes.getLength() > 0) {
                List<String> parsers = new ArrayList<>(parserNodes.getLength());

                for (int j = 0; j < parserNodes.getLength(); j++) {
                    Element parserNode = (Element) parserNodes.item(j);
                    String parserId = parserNode.getAttribute("id");
                    String parserClass = aliases.get(parserId);
                    if (parserClass == null) {
                        notDefinedParsers.add(parserId);
                        continue;
                    }

                    parsers.add(parserClass);
                }

                // now add the plugin list and map it to this mimeType
                if (!parsers.isEmpty()) {
                    parserConfig.setParsers(mimeTypeStr, parsers);
                }
            } else {
                LOG.warn("No plugins defined for mime type: " + mimeTypeStr + ", continuing parse");
            }
        }

        return parserConfig;
    }

    private Map<String, String> getAliases(Element parsePluginsRoot) {
        Map<String, String> aliases = new HashMap<>();
        NodeList aliasRoot = parsePluginsRoot.getElementsByTagName("aliases");

        if (aliasRoot == null || aliasRoot.getLength() == 0) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("No aliases defined in parse-plugins.xml!");
            }
            return aliases;
        }

        if (aliasRoot.getLength() > 1) {
            // log a warning, but try and continue processing
            if (LOG.isWarnEnabled()) {
                LOG.warn("There should only be one \"aliases\" tag in parse-plugins.xml");
            }
        }

        Element aliasRootElem = (Element) aliasRoot.item(0);
        NodeList aliasElements = aliasRootElem.getElementsByTagName("alias");

        if (aliasElements != null && aliasElements.getLength() > 0) {
            for (int i = 0; i < aliasElements.getLength(); i++) {
                Element aliasElem = (Element) aliasElements.item(i);
                String name = aliasElem.getAttribute("name");
                String clazz = aliasElem.getAttribute("class");
                if (name != null && clazz != null) {
                    aliases.put(name, clazz);
                }
            }
        }
        return aliases;
    }
}
