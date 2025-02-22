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

package ai.platon.pulsar.skeleton.common;

import ai.platon.pulsar.common.ObjectCache;
import ai.platon.pulsar.common.config.ImmutableConfig;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.mime.MimeTypesFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public final class MimeTypeResolver {

    private static final String SEPARATOR = ";";
    /* our log stream */
    private static final Logger logger = LoggerFactory.getLogger(MimeTypeResolver.class.getName());

    /* our Tika mime type registry */
    private MimeTypes mimeTypes;

    private TikaConfig tikaConfig;
    /* the tika detectors */
    private final Tika tika = new Tika();
    /* whether magic should be employed or not */
    private boolean mimeMagic;

    public MimeTypeResolver(ImmutableConfig conf) {
        ObjectCache objectCache = ObjectCache.get(conf);
        MimeTypes mimeTypes = objectCache.getBean(MimeTypes.class);

        if (mimeTypes == null) {
            try {
                String customMimeTypeFile = conf.get("mime.types.file");
                if (customMimeTypeFile != null && !customMimeTypeFile.isEmpty()) {
                    try {
                        mimeTypes = MimeTypesFactory.create(conf.getConfResourceAsInputStream(customMimeTypeFile));
                    } catch (Exception e) {
                        logger.error("Can't load mime.types.file : " + customMimeTypeFile + " using Tika's default");
                    }
                }
                if (mimeTypes == null) {
                    mimeTypes = MimeTypes.getDefaultMimeTypes();
                }
            } catch (Exception e) {
                logger.error("Exception in MimeUtil " + e.getMessage());
                throw new RuntimeException(e);
            }

            objectCache.putBean(mimeTypes);
        }

        this.mimeTypes = mimeTypes;
        this.mimeMagic = conf.getBoolean("mime.type.magic", true);
    }

    /**
     * Cleans a {@link MimeType} name by removing out the actual {@link MimeType},
     * from a string of the form:
     * @param origType
     *          The original mime type string to be cleaned.
     * @return The primary type, and subtype, concatenated, e.g., the actual mime
     *         type.
     */
    public static String cleanMimeType(String origType) {
        if (origType == null) {
            return null;
        }

        // take the origType and split it on ';'
        String[] tokenizedMimeType = origType.split(SEPARATOR);
        if (tokenizedMimeType.length > 1) {
            // there was a ';' in there, take the first value
            return tokenizedMimeType[0];
        } else {
            // there wasn't a ';', so just return the orig type
            return origType;
        }
    }

    public void reload(ImmutableConfig conf) {

    }

    /**
     * A facade interface to trying all the possible mime type resolution
     * strategies available within Tika. First, the mime type provided in
     * <code>typeName</code> is cleaned, with {@link #cleanMimeType(String)}. Then
     * the cleaned mime type is looked up in the underlying Tika {@link MimeTypes}
     * registry, by its cleaned name. If the {@link MimeType} is found, then that
     * mime type is used, otherwise URL resolution is used to try and determine
     * the mime type. However, if <code>mime.type.magic</code> is enabled in
     * {@link ImmutableConfig}, then mime type magic resolution is used to try
     * and obtain a better-than-the-default approximation of the {@link MimeType}.
     *
     * @param typeName The original mime type, returned from a ProtocolOutput.
     * @param url The given @see url, that AppConstants was trying to text.
     * @param data The byte data, returned from the text, if any.
     * @return The correctly, automatically guessed {@link MimeType} name.
     */
    public String autoResolveContentType(String typeName, @NotNull String url, @Nullable byte[] data) {
        String retType = null;
        MimeType type = null;

        String cleanedMimeType = MimeTypeResolver.cleanMimeType(typeName);
        // first try to get the type from the cleaned type name
        if (cleanedMimeType != null) {
            try {
                type = mimeTypes.forName(cleanedMimeType);
                cleanedMimeType = type.getName();
            } catch (MimeTypeException mte) {
                // Seems to be a malformed mime type name...
                cleanedMimeType = null;
            }
        }

        // if returned null, or if it's the default type then try url resolution
        if (type == null || type.getName().equals(MimeTypes.OCTET_STREAM)) {
            // If no mime-type header, or cannot find a corresponding registered
            // mime-type, then guess a mime-type from the url pattern

            try {
                retType = tika.detect(url);
            } catch (Exception e) {
                String message = "Problem loading default Tika configuration";
                logger.error(message, e);
                throw new RuntimeException(e);
            }
        } else {
            retType = type.getName();
        }

        // if magic is enabled use mime magic to guess if the mime type returned
        // from the magic guess is different than the one that's already set so far
        // if it is, and it's not the default mime type, then go with the mime type
        // returned by the magic
        if (this.mimeMagic && data != null) {
            String magicType = null;
            // pass URL (file name) and (cleansed) content type from protocol to Tika
            Metadata tikaMeta = new Metadata();
            // tikaMeta.add(Metadata.RESOURCE_NAME_KEY, url);
            tikaMeta.add(Metadata.CONTENT_TYPE, (cleanedMimeType != null ? cleanedMimeType : typeName));
            try (InputStream stream = TikaInputStream.get(data)) {
                magicType = tika.detect(stream, tikaMeta);
            } catch (IOException ignored) {
            }

            if (magicType != null && !magicType.equals(MimeTypes.OCTET_STREAM)
                    && !magicType.equals(MimeTypes.PLAIN_TEXT) && retType != null
                    && !retType.equals(magicType)) {

                // If magic enabled and the current mime type differs from that of the
                // one returned from the magic, take the magic mimeType
                retType = magicType;
            }

            // if type is STILL null after all the resolution strategies, go for the
            // default type
            if (retType == null) {
                try {
                    retType = MimeTypes.OCTET_STREAM;
                } catch (Exception ignored) {
                }
            }
        }

        return retType;
    }

    /**
     * Facade interface to Tika's underlying {@link MimeTypes#getMimeType(String)}
     * method.
     *
     * @param url
     *          A string representation of the document to sense the
     *          {@link MimeType} for.
     * @return An appropriate {@link MimeType}, identified from the given Document
     *         url in string form.
     */
    public String getMimeType(String url) {
        return tika.detect(url);
    }

    /**
     * A facade interface to Tika's underlying {@link MimeTypes#forName(String)}
     * method.
     *
     * @param name
     *          The name of a valid {@link MimeType} in the Tika mime registry.
     * @return The object representation of the {@link MimeType}, if it exists, or
     *         null otherwise.
     */
    public String forName(String name) {
        try {
            return this.mimeTypes.forName(name).toString();
        } catch (MimeTypeException e) {
            logger.error("Exception getting mime type by name: [" + name + "]: Message: " + e.getMessage());
            return null;
        }
    }

    /**
     * Facade interface to Tika's underlying {@link MimeTypes#getMimeType(File)}
     * method.
     *
     * @param path The {@link Path} to sense the {@link MimeType} for.
     * @return The {@link MimeType} of the given {@link File}, or null if it cannot be determined.
     */
    public String detect(Path path) {
        return detect(path.toFile());
    }

    /**
     * Facade interface to Tika's underlying {@link MimeTypes#getMimeType(File)}
     * method.
     *
     * @param file The {@link File} to sense the {@link MimeType} for.
     * @return The {@link MimeType} of the given {@link File}, or null if it cannot be determined.
     */
    public String detect(File file) {
        try {
            return tika.detect(file);
        } catch (Exception e) {
            logger.error("Exception getting mime type for file: [" + file.getPath() + "]: Message: " + e.getMessage());
            return null;
        }
    }
}
