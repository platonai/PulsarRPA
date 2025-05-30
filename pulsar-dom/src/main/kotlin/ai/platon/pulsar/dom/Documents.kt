package ai.platon.pulsar.dom

import org.jsoup.HttpStatusException
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.helper.DataUtil
import org.jsoup.helper.HttpConnection
import org.jsoup.parser.Parser
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.time.Duration

object Documents {

    /**
     * Parse HTML into a FeaturedDocument. The parser will make a sensible, balanced document tree out of any HTML.
     *
     * @param html    HTML to parse
     * @param baseURI The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @return sane HTML
     */
    fun parse(html: String, baseURI: String): FeaturedDocument {
        return FeaturedDocument(Parser.parse(html, baseURI))
    }

    /**
     * Parse HTML into a FeaturedDocument, using the provided Parser. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.
     *
     * @param html    HTML to parse
     * @param baseURI The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     */
    fun parse(html: String, baseURI: String, parser: Parser): FeaturedDocument {
        return FeaturedDocument(parser.parseInput(html, baseURI))
    }

    /**
     * Parse HTML into a FeaturedDocument. As no base PAGE is specified, absolute URL detection relies on the HTML including a
     * `<base href>` tag.
     *
     * @param html HTML to parse
     * @return sane HTML
     * @see .parse
     */
    fun parse(html: String): FeaturedDocument {
        return FeaturedDocument(Parser.parse(html, ""))
    }

    /**
     * Parse the contents of a file as HTML.
     *
     * @param file          file to load HTML from
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseURI     The URL where the HTML was retrieved from, to resolve relative links against.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    fun parse(file: File, charsetName: String, baseURI: String): FeaturedDocument {
        return FeaturedDocument(DataUtil.load(file, charsetName, baseURI))
    }

    fun parse(path: Path, charsetName: String, baseURI: String): FeaturedDocument {
        return parse(path.toFile(), charsetName, baseURI)
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
    fun parse(file: File, charsetName: String): FeaturedDocument {
        return FeaturedDocument(DataUtil.load(file, charsetName, file.absolutePath))
    }

    fun parse(path: Path, charsetName: String): FeaturedDocument {
        return parse(path.toFile(), charsetName)
    }

    /**
     * Read an input stream, and parse it to a FeaturedDocument.
     *
     * @param stream          input stream to read. Make sure to close it after parsing.
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseURI     The URL where the HTML was retrieved from, to resolve relative links against.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    fun parse(stream: InputStream, charsetName: String, baseURI: String): FeaturedDocument {
        return FeaturedDocument(DataUtil.load(stream, charsetName, baseURI))
    }

    /**
     * Read an input stream, and parse it to a FeaturedDocument. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.
     *
     * @param stream          input stream to read. Make sure to close it after parsing.
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseURI     The URL where the HTML was retrieved from, to resolve relative links against.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    fun parse(stream: InputStream, charsetName: String, baseURI: String, parser: Parser): FeaturedDocument {
        return FeaturedDocument(DataUtil.load(stream, charsetName, baseURI, parser))
    }

    /**
     * Parse a fragment of HTML, with the assumption that it forms the `body` of the HTML.
     *
     * @param bodyHtml body HTML fragment
     * @param baseURI  URL to resolve relative URLs against.
     * @return sane HTML document
     * @see FeaturedDocument.body
     */
    fun parseBodyFragment(bodyHtml: String, baseURI: String): FeaturedDocument {
        return FeaturedDocument(Parser.parseBodyFragment(bodyHtml, baseURI))
    }

    /**
     * Parse a fragment of HTML, with the assumption that it forms the `body` of the HTML.
     *
     * @param bodyHtml body HTML fragment
     * @return sane HTML document
     * @see FeaturedDocument.body
     */
    fun parseBodyFragment(bodyHtml: String): FeaturedDocument {
        return FeaturedDocument(Parser.parseBodyFragment(bodyHtml, ""))
    }

    /**
     * Fetch a URL, and parse it as HTML.
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
     */
    fun parse(url: URL, timeoutMillis: Long): FeaturedDocument {
        val jsoupConnection = HttpConnection.connect(url).timeout(timeoutMillis.toInt())
        return FeaturedDocument(jsoupConnection.get())
    }

    /**
     * Fetch a URL, and parse it as HTML
     * */
    fun parse(url: URL, timeout: Duration): FeaturedDocument {
        return parse(url, timeout.toMillis())
    }
}
