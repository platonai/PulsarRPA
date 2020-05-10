package ai.platon.pulsar.persist.model

import ai.platon.pulsar.persist.gora.generated.GFieldGroup
import org.apache.commons.lang3.math.NumberUtils
import java.util.*

/**
 * Created by vincent on 17-8-2.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class FieldGroupFormatter {
    var name: String? = null
    var title: String? = null
    var author: String? = null
    var authors: List<String>? = null
    var directors: List<String>? = null
    var created: String? = null
    var content: String? = null
    var lastModified: String? = null
    var vote = 0
    var review = 0
    var forward = 0
    var oppose = 0
    var sourceLink: String? = null
    var sourceTitle: String? = null
    private var fieldGroup: GFieldGroup
    var id: Long = 0

    constructor(fieldGroup: GFieldGroup) {
        this.fieldGroup = fieldGroup
    }

    constructor(fieldGroup: FieldGroup) {
        this.fieldGroup = fieldGroup.unbox()
    }

    fun parseFields() {
        val fields = fieldGroup.fields
        var k: CharSequence = ""
        var v: CharSequence? = ""
        k = "id"
        v = fields[k]
        if (v != null) id = NumberUtils.toLong(v.toString(), 0)
        k = "name"
        v = fields[k]
        if (v != null) name = v.toString()
        k = "title"
        v = fields[k]
        if (v != null) title = v.toString()
        k = "author"
        v = fields[k]
        if (v != null) author = v.toString()
        k = "authors"
        v = fields[k]
        if (v != null) authors = Arrays.asList(*v.toString().split(",").toTypedArray())
        k = "directors"
        v = fields[k]
        if (v != null) directors = Arrays.asList(*v.toString().split(",").toTypedArray())
        k = "created"
        v = fields[k]
        if (v != null) created = v.toString()
        k = "content"
        v = fields[k]
        if (v != null) content = v.toString()
        k = "reviewCount"
        v = fields[k]
        if (v != null) review = NumberUtils.toInt(v.toString(), 0)
        k = "forwardCount"
        v = fields[k]
        if (v != null) forward = NumberUtils.toInt(v.toString(), 0)
        k = "voteCount"
        v = fields[k]
        if (v != null) vote = NumberUtils.toInt(v.toString(), 0)
        k = "opposeCount"
        v = fields[k]
        if (v != null) oppose = NumberUtils.toInt(v.toString(), 0)
        k = "sourceLink"
        v = fields[k]
        if (v != null) sourceLink = v.toString()
        k = "sourceTitle"
        v = fields[k]
        if (v != null) sourceTitle = v.toString()
    }

    fun format(): String {
        val sb = StringBuilder()
        sb.append("id:\t").append(fieldGroup.id)
        sb.append("parentId:\t").append(fieldGroup.parentId)
        sb.append("group:\t").append(fieldGroup.name)
        val fields = fieldGroup.fields.entries.joinToString("\n") { it.key.toString() + ":\t" + it.value }
        sb.append("fields:\t").append(fields)
        return sb.toString()
    }

    val fields: Map<String, Any>
        get() {
            val result: MutableMap<String, Any> = HashMap()
            result["id"] = fieldGroup.id.toString()
            result["parentId"] = fieldGroup.parentId.toString()
            result["group"] = fieldGroup.name.toString()
            val fields = fieldGroup.fields.entries.associate { it.key.toString() to it.value.toString() }
            result["fields"] = fields
            return result
        }

    override fun toString(): String {
        return format()
    }
}