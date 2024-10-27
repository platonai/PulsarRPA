
package ai.platon.pulsar.common

import java.io.IOException
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/** Unit tests for GZIPUtils methods.  */
class TestGZIPUtils {
    /* a short, highly compressable, string */
    var SHORT_TEST_STRING: String = "aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbcccccccccccccccc"
    
    /* a short, highly compressable, string */
    var LONGER_TEST_STRING: String = (SHORT_TEST_STRING + SHORT_TEST_STRING
        + SHORT_TEST_STRING + SHORT_TEST_STRING + SHORT_TEST_STRING
        + SHORT_TEST_STRING + SHORT_TEST_STRING + SHORT_TEST_STRING
        + SHORT_TEST_STRING + SHORT_TEST_STRING + SHORT_TEST_STRING
        + SHORT_TEST_STRING)
    
    /* a snapshot of the pulsar webpage */
    var WEBPAGE: String = """<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
  <meta http-equiv="content-type"
 content="text/html; charset=ISO-8859-1">
  <title>Pulsar</title>
</head>
<body>
<h1
 style="font-family: helvetica,arial,sans-serif; text-align: center; color: rgb(255, 153, 0);"><a
 href="http://www.pulsar.org/"><font style="color: rgb(255, 153, 0);">Pulsar</font></a><br>
<small>an open source web-search engine</small></h1>
<hr style="width: 100%; height: 1px;" noshade="noshade">
<table
 style="width: 100%; text-align: left; margin-left: auto; margin-right: auto;"
 border="0" cellspacing="0" cellpadding="0">
  <tbody>
    <tr>
      <td style="vertical-align: top; text-align: center;"><a
 href="http://sourceforge.net/project/showfiles.php?group_id=59548">Download</a><br>
      </td>
      <td style="vertical-align: top; text-align: center;"><a
 href="tutorial.html">Tutorial</a><br>
      </td>
      <td style="vertical-align: top; text-align: center;"><a
 href="http://cvs.sourceforge.net/cgi-bin/viewcvs.cgi/pulsar/pulsar/">CVS</a><br>
      </td>
      <td style="vertical-align: top; text-align: center;"><a
 href="service/index.html">Javadoc</a><br>
      </td>
      <td style="vertical-align: top; text-align: center;"><a
 href="http://sourceforge.net/tracker/?atid=491356&amp;group_id=59548&amp;func=browse">Bugs</a><br>
      </td>
      <td style="vertical-align: top; text-align: center;"><a
 href="http://sourceforge.net/mail/?group_id=59548">Lists</a></td>
      <td style="vertical-align: top; text-align: center;"><a
 href="policies.html">Policies</a><br>
      </td>
    </tr>
  </tbody>
</table>
<hr style="width: 100%; height: 1px;" noshade="noshade">
<h2>Introduction</h2>
Pulsar is a nascent effort to implement an open-source web search
engine. Web search is a basic requirement for internet navigation, yet
the number of web search engines is decreasing. Today's oligopoly could
soon be a monopoly, with a single company controlling nearly all web
search for its commercial gain. &nbsp;That would not be good for the
users of internet. &nbsp;Pulsar aims to enable anyone to easily and
cost-effectively deploy a world-class web search engine.<br>
<br>
To succeed, the Pulsar software must be able to:<br>
<ul>
  <li> crawl several billion pages per month</li>
  <li>maintain an index of these pages</li>
  <li>search that index up to 1000 times per second</li>
  <li>provide very high quality search results</li>
  <li>operate at minimal cost</li>
</ul>
<h2>Status</h2>
Currently we're just a handful of developers working part-time to put
together a demo. &nbsp;The demo is coded entirely in Java. &nbsp;However
persistent data is written in well-documented formats so that modules
may eventually be re-written in other languages (e.g., Perl, C++) as the
project progresses.<br>
<br>
<hr style="width: 100%; height: 1px;" noshade="noshade"> <a
 href="http://sourceforge.net"> </a>
<div style="text-align: center;"><a href="http://sourceforge.net"><img
 src="http://sourceforge.net/sflogo.php?group_id=59548&amp;type=1"
 style="border: 0px solid ; width: 88px; height: 31px;"
 alt="SourceForge.net Logo" title=""></a></div>
</body>
</html>
"""
    
    // tests
    @Test
    fun testZipUnzip() {
        var testBytes = SHORT_TEST_STRING.toByteArray()
        testZipUnzip(testBytes)
        testBytes = LONGER_TEST_STRING.toByteArray()
        testZipUnzip(testBytes)
        testBytes = WEBPAGE.toByteArray()
        testZipUnzip(testBytes)
    }
    
    @Test
    fun testZipUnzipBestEffort() {
        var testBytes = SHORT_TEST_STRING.toByteArray()
        testZipUnzipBestEffort(testBytes)
        testBytes = LONGER_TEST_STRING.toByteArray()
        testZipUnzipBestEffort(testBytes)
        testBytes = WEBPAGE.toByteArray()
        testZipUnzipBestEffort(testBytes)
    }
    
    @Test
    fun testTruncation() {
        var testBytes = SHORT_TEST_STRING.toByteArray()
        testTruncation(testBytes)
        testBytes = LONGER_TEST_STRING.toByteArray()
        testTruncation(testBytes)
        testBytes = WEBPAGE.toByteArray()
        testTruncation(testBytes)
    }
    
    @Test
    fun testLimit() {
        var testBytes = SHORT_TEST_STRING.toByteArray()
        testLimit(testBytes)
        testBytes = LONGER_TEST_STRING.toByteArray()
        testLimit(testBytes)
        testBytes = WEBPAGE.toByteArray()
        testLimit(testBytes)
    }
    
    // helpers
    fun testZipUnzip(origBytes: ByteArray) {
        val compressedBytes = GZIPUtils.zip(origBytes)!!
        
        assertTrue("compressed array is not smaller!", compressedBytes.size < origBytes.size)
        
        var uncompressedBytes: ByteArray? = null
        try {
            uncompressedBytes = GZIPUtils.unzip(compressedBytes)
        } catch (e: IOException) {
            fail("caught exception '$e' during unzip()")
        }
        assertEquals(
            uncompressedBytes!!.size.toLong(),
            origBytes.size.toLong(),
            "uncompressedBytes is wrong size",
        )
        
        for (i in origBytes.indices) if (origBytes[i] != uncompressedBytes[i]) fail("uncompressedBytes does not match origBytes")
    }
    
    fun testZipUnzipBestEffort(origBytes: ByteArray) {
        val compressedBytes = GZIPUtils.zip(origBytes)!!
        
        assertTrue(
            "compressed array is not smaller!",
            compressedBytes.size < origBytes.size
        )
        
        val uncompressedBytes = GZIPUtils.unzipBestEffort(compressedBytes)
        assertEquals(uncompressedBytes.size.toLong(), origBytes.size.toLong(), "uncompressedBytes is wrong size")
        
        for (i in origBytes.indices) if (origBytes[i] != uncompressedBytes[i]) fail("uncompressedBytes does not match origBytes")
    }
    
    fun testTruncation(origBytes: ByteArray) {
        val compressedBytes = GZIPUtils.zip(origBytes)
        
        println("original data has len " + origBytes.size)
        assert(compressedBytes != null)
        println("compressed data has len " + compressedBytes!!.size)
        
        for (i in compressedBytes.size downTo 0) {
            val truncCompressed = ByteArray(i)
            
            System.arraycopy(compressedBytes, 0, truncCompressed, 0, i)
            
            val trunc = GZIPUtils.unzipBestEffort(truncCompressed)
            
            if (trunc == null) {
                // System.out.println("truncated to len " + i + ", trunc is null");
            } else {
//                System.out.println("truncated to len " + i + ", trunc.length=  "
//                        + trunc.length);
                
                for (j in trunc.indices) if (trunc[j] != origBytes[j]) fail(
                    "truncated/uncompressed array differs at pos " + j
                        + " (compressed data had been truncated to len " + i + ")"
                )
            }
        }
    }
    
    fun testLimit(origBytes: ByteArray) {
        val compressedBytes = GZIPUtils.zip(origBytes)
        
        assertTrue("compressed array is not smaller!", compressedBytes.size < origBytes.size)
        
        for (i in origBytes.indices) {
            val uncompressedBytes = GZIPUtils.unzipBestEffort(compressedBytes, i)
            assertEquals(uncompressedBytes.size, i, "uncompressedBytes is wrong size")
            for (j in 0 until i) if (origBytes[j] != uncompressedBytes[j]) {
                fail("uncompressedBytes does not match origBytes")
            }
        }
    }
}
