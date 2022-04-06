package ai.platon.pulsar.index

import ai.platon.pulsar.common.DateTimes.isoInstantFormat
import ai.platon.pulsar.common.DateTimes.parseHttpDateTime
import ai.platon.pulsar.common.HttpHeaders
import ai.platon.pulsar.common.MimeTypeResolver
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.index.IndexDocument
import ai.platon.pulsar.crawl.index.IndexingFilter
import ai.platon.pulsar.persist.WebPage
import org.apache.oro.text.regex.PatternMatcher
import org.apache.oro.text.regex.Perl5Matcher
import java.time.Instant

/**
 * Add (or reset) a few metaData properties as respective fields (if they are
 * available), so that they can be accurately used within the search index.
 *
 *
 * 'lastModifed' is indexed to support query by date, 'contentLength' obtains
 * content length from the HTTP header, 'type' field is indexed to support query
 * by type and finally the 'title' field is an attempt to reset the title if a
 * content-disposition hint exists. The logic is that such a presence is
 * indicative that the content provider wants the filename therein to be used as
 * the title.
 *
 *
 * Still need to make content-length searchable!
 *
 * @author John Xing
 */
class MoreIndexingFilter(
    var MIME: MimeTypeResolver,
    override var conf: ImmutableConfig,
) : IndexingFilter {
    private val matcher: PatternMatcher = Perl5Matcher()

    constructor(conf: ImmutableConfig) : this(MimeTypeResolver(conf), conf) {}

    override fun setup(conf: ImmutableConfig) {
        this.conf = conf
        MIME = MimeTypeResolver(conf)
    }

    override fun filter(doc: IndexDocument, url: String, page: WebPage): IndexDocument? {
        addTime(doc, page, url)
        addLength(doc, page, url)
        addType(doc, page, url)
        val filename = page.headers.dispositionFilename
        if (filename != null) {
            doc.removeField("meta_title")
            doc.add("meta_title", filename)
        }
        return doc
    }

    // Add time related meta info. Add last-modified if present. Index date as
    // last-modified, or, if that's not present, use fetch time.
    private fun addTime(doc: IndexDocument, page: WebPage, url: String): IndexDocument {
        var time = Instant.EPOCH
        val lastModified = page.headers[HttpHeaders.LAST_MODIFIED]
        if (lastModified != null) {
            // try parse last-modified
            time = parseHttpDateTime(lastModified, Instant.EPOCH) // use as time
        }
        if (time.toEpochMilli() > 0) { // if no last-modified
            time = page.modifiedTime // use Modified time
        }

        // un-stored, indexed and un-tokenized
        if (time.toEpochMilli() > 0) {
            doc.add("header_last_modified", isoInstantFormat(time))
            doc.add("last_modified_s", isoInstantFormat(time))
        }
        return doc
    }

    // Add Content-Length
    private fun addLength(doc: IndexDocument, page: WebPage, url: String): IndexDocument {
        val contentLength: CharSequence? = page.headers[HttpHeaders.CONTENT_LENGTH]
        if (contentLength != null) {
            val trimmed = contentLength.toString().trim { it <= ' ' }
            if (!trimmed.isEmpty()) doc.add("content_length", trimmed)
        }
        return doc
    }

    /**
     *
     *
     * Add Content-Type and its primaryType and subType add contentType,
     * primaryType and subType to field "type" as un-stored, indexed and
     * un-tokenized, so that search results can be confined by contentType or its
     * primaryType or its subType.
     *
     *
     *
     * For example, if contentType is application/vnd.ms-powerpoint, search can be
     * done with one of the following qualifiers
     * type:application/vnd.ms-powerpoint type:application type:vnd.ms-powerpoint
     * all case insensitive. The query filter is implemented in
     *
     *
     * @param doc
     * @param page
     * @param url
     * @return
     */
    private fun addType(doc: IndexDocument, page: WebPage, url: String): IndexDocument {
        val mimeType: String?
        var contentType = page.contentType
        if (contentType.isEmpty()) {
//      contentType = page.getHeaders().get(new Utf8(HttpHeaders.CONTENT_TYPE));
            contentType = page.headers.getOrDefault(HttpHeaders.CONTENT_TYPE, "")
        }
        mimeType = if (contentType.isEmpty()) {
            // Note by Jerome Charron on 20050415:
            // Content Type not solved by a previous plugin
            // Or unable to solve it... Trying to find it
            // Should be better to use the doc content too
            // (using MimeTypes.getMimeType(byte[], String), but I don't know
            // which field it is?
            // if (MAGIC) {
            // contentType = MIME.getMimeType(url, content);
            // } else {
            // contentType = MIME.getMimeType(url);
            // }
            MIME.getMimeType(url)
        } else {
            MIME.forName(MimeTypeResolver.cleanMimeType(contentType))
        }

        // Checks if we solved the content-type.
        if (mimeType == null) {
            return doc
        }
        doc.add("mime_type", mimeType)

        // Check if we need to split the content type in sub parts
        if (conf.getBoolean("moreIndexingFilter.indexMimeTypeParts", true)) {
            val parts = getParts(mimeType)
            for (part in parts) {
                doc.add("mime_type", part)
            }
        }

        // leave this for future improvement
        // MimeTypeParameterList parameterList = mimeType.getParameters()
        return doc
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    companion object {
        /**
         * Utility method for splitting mime type into type and subtype.
         *
         * @param mimeType
         * @return
         */
        fun getParts(mimeType: String): Array<String> {
            return mimeType.split("/".toRegex()).toTypedArray()
        }
    }

    init {
        setup(conf)
    }
}
