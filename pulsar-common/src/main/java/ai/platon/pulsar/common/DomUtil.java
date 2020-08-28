/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.parsers.DOMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Set;

/**
 * <p>DomUtil class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public class DomUtil {

    private final static Logger LOG = LoggerFactory.getLogger(DomUtil.class);

    /**
     * Returns parsed dom tree or null if any error
     *
     * @param is a {@link java.io.InputStream} object.
     * @return A parsed DOM tree from the given {@link java.io.InputStream}.
     */
    public static Element getDom(InputStream is) {

        Element element = null;

        DOMParser parser = new DOMParser();

        InputSource input;
        try {
            input = new InputSource(is);
            input.setEncoding("UTF-8");
            parser.parse(input);
            int i = 0;
            while (!(parser.getDocument().getChildNodes().item(i) instanceof Element)) {
                i++;
            }
            element = (Element) parser.getDocument().getChildNodes().item(i);
        } catch (FileNotFoundException e) {
            LOG.error("Failed to find file: ", e);
        } catch (SAXException e) {
            LOG.error("Failed with the following SAX exception: ", e);
        } catch (IOException e) {
            LOG.error("Failed with the following IOException", e);
        }
        return element;
    }

    /**
     * save dom into ouputstream
     *
     * @param os a {@link java.io.OutputStream} object.
     * @param e a {@link org.w3c.dom.Element} object.
     */
    public static void saveDom(OutputStream os, Element e) {
        DOMSource source = new DOMSource(e);
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            StreamResult result = new StreamResult(os);
            transformer.transform(source, result);
            os.flush();
        } catch (UnsupportedEncodingException e1) {
            LOG.error("Failed with the following UnsupportedEncodingException: ", e1);
        } catch (IOException e1) {
            LOG.error("Failed to with the following IOException: ", e1);
        } catch (TransformerConfigurationException e2) {
            LOG.error(
                    "Failed with the following TransformerConfigurationException: ", e2);
        } catch (TransformerException ex) {
            LOG.error("Failed with the following TransformerException: ", ex);
        }
    }

    /**
     * <p>getAttribute.</p>
     *
     * @param node a {@link org.w3c.dom.Node} object.
     * @param attrName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getAttribute(Node node, String attrName) {
        NamedNodeMap map = node.getAttributes();

        if (map != null) {
            Node attrNode = map.getNamedItem(attrName);
            if (attrNode != null) {
                return attrNode.getNodeValue();
            }
        }

        return null;
    }

    /**
     * <p>getId.</p>
     *
     * @param node a {@link org.w3c.dom.Node} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getId(Node node) {
        return getId(node, false);
    }

    /**
     * <p>getId.</p>
     *
     * @param node a {@link org.w3c.dom.Node} object.
     * @param prefix a boolean.
     * @return a {@link java.lang.String} object.
     */
    public static String getId(Node node, boolean prefix) {
        String id = getAttribute(node, "id");

        if (id != null) {
            String p = prefix ? "#" : "";
            return p + id;
        }

        return null;
    }

    /**
     * <p>getClassString.</p>
     *
     * @param node a {@link org.w3c.dom.Node} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getClassString(Node node) {
        return getAttribute(node, "class");
    }

    /**
     * <p>getClasses.</p>
     *
     * @param node a {@link org.w3c.dom.Node} object.
     * @return a {@link java.util.Set} object.
     */
    public static Set<String> getClasses(Node node) {
        return getClasses(node, "");
    }

    /**
     * <p>getClasses.</p>
     *
     * @param node a {@link org.w3c.dom.Node} object.
     * @param prefix a {@link java.lang.String} object.
     * @return a {@link java.util.Set} object.
     */
    public static Set<String> getClasses(Node node, String prefix) {
        Set<String> classes = Sets.newHashSet();

        String cls = getAttribute(node, "class");
        if (cls != null) {
            for (String s : cls.split("\\s+")) {
                if (s != null) classes.add(prefix + s);
            }
        }

        return classes;
    }

    /**
     * TODO : Use real css selector
     *
     * @param node a {@link org.w3c.dom.Node} object.
     * @return a {@link java.util.Set} object.
     */
    public static Set<String> getSimpleSelectors(Node node) {
        Set<String> selectors = getClasses(node, ".");

        String id = getId(node, true);
        if (id != null) selectors.add(id);

        // selectors.add(node.getNodeName());

        return selectors;
    }

    /**
     * <p>getPrettyName.</p>
     *
     * @param node a {@link org.w3c.dom.Node} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getPrettyName(Node node) {
        NamedNodeMap map = node.getAttributes();
        String msg = node.getNodeName().toLowerCase();

        if (map != null) {
            Node id = map.getNamedItem("id");
            if (id != null) {
                msg += "#" + id.getNodeValue();
            }

            Node cls = map.getNamedItem("class");
            if (cls != null) {
                msg += "." + StringUtils.join(cls.getNodeValue().split("\\s+"), '.');
            }
        }

        return msg;
    }
}
