package ai.platon.pulsar.persist.ext

import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.persist.HyperlinkPersistable
import ai.platon.pulsar.persist.WebPage
import org.apache.gora.util.ByteUtils
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.time.Instant
import java.time.temporal.ChronoUnit
//
///**
// * Get content as bytes, the underling buffer is duplicated
// *
// * @return a duplication of the underling buffer.
// */
//val WebPage.contentAsBytes: ByteArray get() {
//    val content: ByteBuffer = content ?: return ByteUtils.toBytes('\u0000')
//    return ByteUtils.toBytes(content)
//}
//
///**
// * TODO: Encoding is always UTF-8?
// *
// * Get the page content as a string
// */
//val WebPage.contentAsString: String get() = ByteUtils.toString(contentAsBytes)
//
///**
// * Get the page content as input stream
// */
//val WebPage.contentAsInputStream: ByteArrayInputStream get() {
//    val buffer: ByteBuffer = content ?: return ByteArrayInputStream(ByteUtils.toBytes('\u0000'))
//    return ByteArrayInputStream(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
//}
//
///**
// * Get the page content as sax input source
// */
//val WebPage.contentAsSaxInputSource: InputSource get() {
//    val inputSource = InputSource(contentAsInputStream)
//    val encoding: String? = encoding
//    if (encoding != null) {
//        inputSource.encoding = encoding
//    }
//    return inputSource
//}
//
///**
// * Set the page content
// */
//fun WebPage.setContent(value: String?) {
//    if (value != null) {
//        setContent(value.toByteArray())
//    } else {
//        content = null
//    }
//}
//
///**
// * Set the page content
// */
//fun WebPage.setContent(value: ByteArray?) {
//    content = if (value != null) {
//        ByteBuffer.wrap(value)
//    } else {
//        null
//    }
//}
//
//fun WebPage.setTextCascaded(text: String?) {
//    setContent(text)
//    setContentText(text)
//    setPageText(text)
//}

//
///**
// * Record all links appeared in a page
// * The links are in FIFO order, for each time we fetch and parse a page,
// * we push newly discovered links to the queue, if the queue is full, we drop out some old ones,
// * usually they do not appears in the page any more.
// *
// *
// * TODO: compress links
// * TODO: HBase seems not modify any nested array
// *
// * @param hypeLinks a [java.lang.Iterable] object.
// */
//fun WebPage.addHyperlinks(hypeLinks: Iterable<HyperlinkPersistable>) {
//    var links: MutableList<CharSequence?> = unbox().links
//
//    // If there are too many links, Drop the front 1/3 links
//    if (links.size > AppConstants.MAX_LINK_PER_PAGE) {
//        links = links.subList(links.size - AppConstants.MAX_LINK_PER_PAGE / 3, links.size)
//    }
//    for (l in hypeLinks) {
//        val url = WebPage.u8(l.url)
//        if (!links.contains(url)) {
//            links.add(url)
//        }
//    }
//    setLinks(links)
//    impreciseLinkCount = links.size
//}
//
//fun WebPage.sniffModifiedTime(): Instant {
//    var modifiedTime: Instant = modifiedTime
//    val headerModifiedTime: Instant = headers.lastModified
//    val contentModifiedTime: Instant = contentModifiedTime
//    if (isValidContentModifyTime(headerModifiedTime) && headerModifiedTime.isAfter(modifiedTime)) {
//        modifiedTime = headerModifiedTime
//    }
//    if (isValidContentModifyTime(contentModifiedTime) && contentModifiedTime.isAfter(modifiedTime)) {
//        modifiedTime = contentModifiedTime
//    }
//    val contentPublishTime: Instant = contentPublishTime
//    if (isValidContentModifyTime(contentPublishTime) && contentPublishTime.isAfter(modifiedTime)) {
//        modifiedTime = contentPublishTime
//    }
//
//    // A fix
//    if (modifiedTime.isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) {
//        // LOG.warn("Invalid modified time " + DateTimeUtil.isoInstantFormat(modifiedTime) + ", url : " + page.url());
//        modifiedTime = Instant.now()
//    }
//    return modifiedTime
//}
//
//fun WebPage.updateContentPublishTime(newPublishTime: Instant): Boolean {
//    if (!isValidContentModifyTime(newPublishTime)) {
//        return false
//    }
//
//    val lastPublishTime: Instant = contentPublishTime
//    if (newPublishTime.isAfter(lastPublishTime)) {
//        prevContentPublishTime = lastPublishTime
//        contentPublishTime = newPublishTime
//    }
//    return true
//}
//
//fun WebPage.updateContentModifiedTime(newModifiedTime: Instant): Boolean {
//    if (!isValidContentModifyTime(newModifiedTime)) {
//        return false
//    }
//    val lastModifyTime = contentModifiedTime
//    if (newModifiedTime.isAfter(lastModifyTime)) {
//        prevContentModifiedTime = lastModifyTime
//        contentModifiedTime = newModifiedTime
//    }
//    return true
//}
//
//fun WebPage.updateRefContentPublishTime(newRefPublishTime: Instant): Boolean {
//    if (!isValidContentModifyTime(newRefPublishTime)) {
//        return false
//    }
//
//    val latestRefPublishTime: Instant = refContentPublishTime
//
//    // LOG.debug("Ref Content Publish Time: " + latestRefPublishTime + " -> " + newRefPublishTime + ", Url: " + getUrl());
//    if (newRefPublishTime.isAfter(latestRefPublishTime)) {
//        prevRefContentPublishTime = latestRefPublishTime
//        refContentPublishTime = newRefPublishTime
//
//        // LOG.debug("[Updated] " + latestRefPublishTime + " -> " + newRefPublishTime);
//        return true
//    }
//
//    return false
//}
//
//private fun WebPage.isValidContentModifyTime(publishTime: Instant): Boolean {
//    return publishTime.isAfter(AppConstants.MIN_ARTICLE_PUBLISH_TIME)
//}
