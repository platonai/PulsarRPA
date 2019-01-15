package `fun`.platonic.scent.dom.data

import `fun`.platonic.pulsar.common.FuzzyProbability
import `fun`.platonic.pulsar.common.FuzzyTracker
import `fun`.platonic.pulsar.common.StringUtil
import `fun`.platonic.scent.dom.*
import `fun`.platonic.scent.dom.nodes.getFeature
import com.google.common.collect.Lists
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import java.util.*

class BlockPattern(value: String): BlockLabel(value) {

    constructor(label: BlockLabel): this(label.text)

    override val isBuiltin: Boolean
        get() = patterns.contains(this)

    fun matches(ele: Element): Boolean {
        return matches(ele, this)
    }

    companion object {
        var H = BlockPattern("H")
        val N2 = BlockPattern("N2")
        val II = BlockPattern("II")
        val Table = BlockPattern("Table")
        val Dl = BlockPattern("Dl")
        val List = BlockPattern("List")

        val Links = BlockPattern("Links")
        val DenseLinks = BlockPattern("DenseLinks")
        val Images = BlockPattern("Images")
        val LinkImages = BlockPattern("LinkImages")
        val DenseText = BlockPattern("DenseText")

        val patterns: MutableSet<BlockPattern> = HashSet()

        init {
            patterns.add(H)

            patterns.add(N2)
            patterns.add(II)
            patterns.add(Table)
            patterns.add(Dl)

            patterns.add(DenseLinks)
            patterns.add(Images)
            patterns.add(LinkImages)
            patterns.add(DenseText)

            // patterns.put(List);
            // patterns.put(Links);
        }

        @Deprecated("Use machine learning instead")
        fun matches(ele: Element, pattern: BlockPattern): Boolean {
            if (pattern == N2) {
                return isN2(ele)
            }

            if (pattern == II) {
                return isII(ele)
            }

            if (pattern == Table) {
                return isTable(ele)
            }

            if (pattern == List) {
                return isList(ele)
            }

            if (pattern == Dl) {
                return isDl(ele)
            }

            if (pattern == DenseLinks) {
                return isDenseLinks(ele)
            }

            if (pattern == Links) {
                return isLinks(ele)
            }

            if (pattern == LinkImages) {
                isLinkImages(ele)
            }

            if (pattern == Images) {
                return isImages(ele)
            }

            if (pattern == H) {
                return isH(ele)
            }

            return false
        }

        fun getMatchPatterns(ele: Element): List<BlockPattern> {
            return patterns.filter { matches(ele, it) }
        }

        fun isH(ele: Element): Boolean {
            // TODO: WHY h[2-4] ranther than h[1-6] ?
            return ele.tagName().matches("h[2-4]".toRegex())
        }

        @Deprecated("Use machine learning instead")
        fun isTable(ele: Element): Boolean {
            var ele = ele
            val knownTags = Lists.newArrayList("table", "tbody")
            if (!knownTags.contains(ele.tagName())) {
                return false
            }

            if (ele.tagName() == "table") {
                val tbody = ele.getElementsByTag("tbody").first()
                if (tbody != null) ele = tbody
            }

            val _child = ele.getFeature(C)

            // TODO : why need a minimal child number?
            // tooo few children, or tooo many grand children
            return !(_child < 3)
        }

        @Deprecated("Use machine learning instead")
        fun isDl(ele: Element): Boolean {
            val knownTags = Lists.newArrayList("dl")
            if (!knownTags.contains(ele.tagName())) {
                return false
            }

            val _child = ele.getFeature(C)

            // tooo few children, or tooo many grand children
            return !(_child < 3)

        }

        @Deprecated("Use machine learning instead")
        fun isII(ele: Element): Boolean {
            return listLikely(ele)
        }

        /**
         * Calculate the likelihood of the children, if the children are very
         * likely, it's a list-like code block
         */
        @Deprecated("Use machine learning instead")
        fun isN2(ele: Element): Boolean {
            val _child = ele.getFeature(C)
            val _txt_nd = ele.getFeature(TN)

            // tooo few children
            if (_child < 3) {
                return false
            }

            if (_txt_nd / _child < 2 || _txt_nd / _child >= 3) {
                return false
            }

            val childTags = HashSet<String>()
            val numGrandson = HashSet<Double>()

            for (child in ele.children()) {
                childTags.add(child.tagName())
                numGrandson.add(child.getFeature(C))
            }

            // 如果列表项的tag标签和直接子孩子数高度相似，那么可以忽略内部结构的相似性，这是对基于方差判断方法的补充
            return childTags.size <= 3 && numGrandson.size <= 3

        }

        @Deprecated("Use machine learning instead")
        fun isList(ele: Element): Boolean {
            val _child = ele.getFeature(C)

            // since most list like blocks are found out by variance rule,
            // we consider blocks complex enough here
            return if (_child < 8) {
                false
            } else !isLinks(ele) && !isLinkImages(ele) && listLikely(ele)

        }

        /**
         * Notice : List likely block is not only "List" pattern!
         */
        @Deprecated("Use machine learning instead")
        fun isLinks(ele: Element): Boolean {
            if (isDenseLinks(ele)) {
                return false
            }

            val _a = ele.getFeature(A)
            val _img = ele.getFeature(IMG)

            // Images and LinkImages are more important
            return if (isLinkImages(ele) || isImages(ele)) {
                false
            } else _a >= 3 && _img < 6 && _img / _a <= 0.2

        }

        @Deprecated("Use machine learning instead")
        fun isDenseLinks(ele: Element): Boolean {
            val _a = ele.getFeature(A)
            val _img = ele.getFeature(IMG)
            val _txt_nd = ele.getFeature(TN)

            return if (_a >= 30 && _txt_nd >= 30 && _img < 0.3 * _a) {
                true
            } else _a >= 100 && _txt_nd >= 100 && _img < 0.3 * _a

        }

        @Deprecated("Use machine learning instead")
        fun isImages(ele: Element): Boolean {
            val _a = ele.getFeature(A)
            val _img = ele.getFeature(IMG)
            val _txt_nd = ele.getFeature(TN)

            // if there are too few images, but if they are big enough, they are welcome
            return if (_img >= 1 && _img <= 3 && _a <= 1) {
                true
            } else _img > 3 && _a / _img <= 0.2 && _txt_nd / _img <= 1.5
        }

        /**
         * link-image example :
         *
         * [<img src="..."></img><a>
         * <div>[<span>something</span> **awesome**](...)</div>
         * <div>[<span>something</span> **awesome**](...)</div>
         * <div><span>damn</span> the **awesome** things</div></a>](...)
         */
        @Deprecated("Use machine learning instead")
        fun isLinkImages(ele: Element): Boolean {
            if (!listLikely(ele)) {
                return false
            }

            val _a = ele.getFeature(A)
            val _img = ele.getFeature(IMG)
            val _char = ele.getFeature(CH)
            val _txt_nd = ele.getFeature(TN)

            // too few images or links
            if (_img < 3 || _a < 3) {
                return false
            }

            // long text not permitted, tooo many text block not permitted
            if (_char / _a > 80 || _char / _img > 80 || _txt_nd / Math.max(_a, _img) > 8) {
                return false
            }

            val rate = _a / _img
            return rate >= 0.8 && rate <= 1.2 || rate >= 1.8 && rate <= 2.2 || rate >= 2.8 && rate <= 3.2

        }

        fun isDesyText(ele: Element): Boolean {
            val _char = ele.getFeature(CH)
            val _a = ele.getFeature(A)
            val _img = ele.getFeature(IMG)
            val _txt_nd = ele.getFeature(TN)

            return !(_char < 300 || _txt_nd < 30 || _char / _txt_nd < 10 || _a / _txt_nd > 0.2 || _img / _txt_nd > 0.1)

        }

        /**
         * Notice : List likely block is not only "List" pattern!
         */
        @Deprecated("Use machine learning instead")
        fun listLikely(ele: Element): Boolean {
            val knownTags = Lists.newArrayList("ul", "ol")
            if (knownTags.contains(ele.tagName())) {
                return true
            }

            // calculate the likelihood of the children, if the children are very
            // likely, it's a list-like code block
            val _child = ele.getFeature(C)
            val _img = ele.getFeature(IMG)

            // tooo few children, or tooo many grand children
            if (_child < 3) {
                return false
            }

            if (_img > 3) {
                return false
            }

            val childTags = HashSet<String>()
            val numGrandson = HashSet<Double>()

            for (child in ele.children()) {
                childTags.add(child.tagName())
                numGrandson.add(child.getFeature(C))
            }

            // 如果列表项的tag标签和直接子孩子数高度相似，那么可以忽略内部结构的相似性，这是对基于方差判断方法的补充
            // 0.2 表示 : 每5个里边允许有一个干扰项，即20%干扰项
            return childTags.size / _child <= 0.2 && numGrandson.size / _child <= 0.2
        }
    }
}

class BlockPatternTracker : FuzzyTracker<BlockPattern>() {

    val patterns: List<String>
        get() {
            return keySet().map { it.text }
        }

    val asString: String
        get() = filterToString(FuzzyProbability.UNSURE)

    fun filterToString(p: FuzzyProbability): String {
        val patterns = StringBuilder()

        for (pattern in keySet()) {
            if (`is`(pattern, p)) {
                patterns.append(pattern)
                patterns.append(StringUtils.SPACE)
            }
        }

        return patterns.toString()
    }
}
