package ai.platon.pulsar.dom

import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.helper.DataUtil
import org.jsoup.helper.HttpConnection
import org.jsoup.parser.Parser
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.time.Duration

object Documents {

    /**
     * Parse HTML into a FeaturedDocument. The parser will make a sensible, balanced document tree out of any HTML.
     *
     * @param html    HTML to parse
     * @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @return sane HTML
     */
    fun parse(html: String, baseUri: String): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(Parser.parse(html, baseUri))
    }

    /**
     * Parse HTML into a FeaturedDocument, using the provided Parser. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.
     *
     * @param html    HTML to parse
     * @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     */
    fun parse(html: String, baseUri: String, parser: Parser): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(parser.parseInput(html, baseUri))
    }

    /**
     * Parse HTML into a FeaturedDocument. As no base PAGE is specified, absolute URL detection relies on the HTML including a
     * `<base href>` tag.
     *
     * @param html HTML to parse
     * @return sane HTML
     * @see .parse
     */
    fun parse(html: String): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(Parser.parse(html, ""))
    }

    /**
     * Creates a new [Connection] to a URL. Use to fetch and parse a HTML page.
     *
     *
     * Use examples:
     *
     *  * `Document doc = Jsoup.connect("http://example.com").userAgent("Mozilla").data("name", "dom").get();`
     *  * `Document doc = Jsoup.connect("http://example.com").cookie("auth", "token").post();`
     *
     * @param url URL to connect to. The protocol must be `http` or `https`.
     * @return the connection. You can add data, cookies, and headers; set the user-agent, referrer, method; and then execute.
     */
    fun connect(url: String): Connection {
        return HttpConnection.connect(url)
    }

    /**
     * Parse the contents of a file as HTML.
     *
     * @param file          file to load HTML from
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    fun parse(file: File, charsetName: String, baseUri: String): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(DataUtil.load(file, charsetName, baseUri))
    }

    /**
     * Parse the contents of a file as HTML. The location of the file is used as the base PAGE to qualify relative URLs.
     *
     * @param file          file to load HTML from
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     * @see .parse
     */
    fun parse(file: File, charsetName: String): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(DataUtil.load(file, charsetName, file.absolutePath))
    }

    fun parse(file: File, charsetName: String, ignoreScript: Boolean): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(DataUtil.load(file, charsetName, file.absolutePath, ignoreScript))
    }

    /**
     * Read an input stream, and parse it to a FeaturedDocument.
     *
     * @param istream          input stream to read. Make sure to close it after parsing.
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    fun parse(istream: InputStream, charsetName: String, baseUri: String): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(DataUtil.load(istream, charsetName, baseUri))
    }

    fun parse(istream: InputStream, charsetName: String, baseUri: String, ignoreScript: Boolean): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(DataUtil.load(istream, charsetName, baseUri, ignoreScript))
    }

    /**
     * Read an input stream, and parse it to a FeaturedDocument. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.
     *
     * @param istream          input stream to read. Make sure to close it after parsing.
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    fun parse(istream: InputStream, charsetName: String, baseUri: String, parser: Parser): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(DataUtil.load(istream, charsetName, baseUri, parser))
    }

    /**
     * Parse a fragment of HTML, with the assumption that it forms the `body` of the HTML.
     *
     * @param bodyHtml body HTML fragment
     * @param baseUri  URL to resolve relative URLs against.
     * @return sane HTML document
     * @see FeaturedDocument.body
     */
    fun parseBodyFragment(bodyHtml: String, baseUri: String): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(Parser.parseBodyFragment(bodyHtml, baseUri))
    }

    /**
     * Parse a fragment of HTML, with the assumption that it forms the `body` of the HTML.
     *
     * @param bodyHtml body HTML fragment
     * @return sane HTML document
     * @see FeaturedDocument.body
     */
    fun parseBodyFragment(bodyHtml: String): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.FeaturedDocument(Parser.parseBodyFragment(bodyHtml, ""))
    }

    /**
     * Fetch a URL, and parse it as HTML. Provided for compatibility; in most cases use [.connect] instead.
     *
     *
     * The encoding character set is determined by the content-type header or http-equiv meta tag, or falls back to `UTF-8`.
     *
     * @param url           URL to fetch (with a GET). The protocol must be `http` or `https`.
     * @param timeoutMillis Connection and read timeout, in milliseconds. If exceeded, IOException is thrown.
     * @return The parsed HTML.
     * @throws java.net.MalformedURLException if the request URL is not a HTTP or HTTPS URL, or is otherwise malformed
     * @throws HttpStatusException if the response is not OK and HTTP response errors are not ignored
     * @throws UnsupportedMimeTypeException if the response mime type is not supported and those errors are not ignored
     * @throws java.net.SocketTimeoutException if the connection times out
     * @throws IOException if a connection or read error occurs
     * @see .connect
     */
    fun parse(url: URL, timeoutMillis: Long): ai.platon.pulsar.dom.FeaturedDocument {
        val con = HttpConnection.connect(url)
        con.timeout(timeoutMillis.toInt())
        return ai.platon.pulsar.dom.FeaturedDocument(con.get())
    }

    fun parse(url: URL, timeout: Duration): ai.platon.pulsar.dom.FeaturedDocument {
        return ai.platon.pulsar.dom.Documents.parse(url, timeout.toMillis())
    }
}
