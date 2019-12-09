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
package ai.platon.pulsar.parse.tika

import org.apache.tika.exception.TikaException
import org.apache.tika.mime.MediaType
import org.apache.tika.mime.MimeTypes
import org.apache.tika.mime.MimeTypesFactory
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.Parser
import org.apache.tika.parser.html.HtmlParser
import org.apache.tika.parser.microsoft.OfficeParser
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser
import org.apache.tika.parser.mp3.Mp3Parser
import org.apache.tika.parser.odf.OpenDocumentParser
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.function.Consumer
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Parse xml config file.
 */
class PulsarTikaConfig {
    private val parsers: MutableMap<String, Parser> = HashMap()
    val mimeRepository: MimeTypes

    init {
        val context = ParseContext()
        for (parser in arrayOf<Parser>(
                HtmlParser(),
                OfficeParser(),
                OpenDocumentParser(),
                Mp3Parser(),
                OOXMLParser()
        )) {
            parser.getSupportedTypes(context).forEach { type: MediaType -> parsers[type.toString()] = parser }
        }
        mimeRepository = MimeTypesFactory.create("tika-mimetypes.xml")
    }

    /**
     * Returns the parser instance configured for the given MIME type. Returns
     * `null` if the given MIME type is unknown.
     *
     * @param mimeType MIME type
     * @return configured Parser instance, or `null`
     */
    fun getParser(mimeType: String): Parser? {
        return parsers[mimeType]
    }

    companion object {
        /**
         * Provides a default configuration (TikaConfig). Currently creates a new
         * instance each time it's called; we may be able to have it return a shared
         * instance once it is completely immutable.
         *
         * @return default configuration
         */
        val defaultConfig = PulsarTikaConfig()
    }
}
