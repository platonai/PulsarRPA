package ai.platon.pulsar.persist.model

import ai.platon.pulsar.persist.gora.generated.GFieldGroup
import org.apache.commons.collections4.CollectionUtils

/**
 * Created by vincent on 17-8-3.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 *
 * The core concept of Document Data Model, DDM
 */
class PageModel(
        val fieldGroups: MutableList<GFieldGroup>
) {
    val numGroups get() = fieldGroups.size

    val numFields get() = fieldGroups.sumOf { it.fields.size }

    val numNonNullFields get() = fieldGroups.sumOf { it.fields.count { it.value != null } }

    val numNonBlankFields get() = fieldGroups.sumOf { it.fields.count { !it.value.isNullOrBlank() } }

    val isEmpty: Boolean get() = fieldGroups.isEmpty()

    val isNotEmpty: Boolean get() = !isEmpty

    fun unbox() = fieldGroups

    fun firstOrNull() = if (isEmpty) null else get(0)

    operator fun get(i: Int) = FieldGroup.box(fieldGroups[i])

    fun add(fieldGroup: FieldGroup) = fieldGroups.add(fieldGroup.unbox())

    fun add(index: Int, fieldGroup: FieldGroup) = fieldGroups.add(index, fieldGroup.unbox())

    fun emplace(groupId: Int, group: String, fields: Map<String, String?>): FieldGroup {
        return emplace(groupId, 0, group, fields)
    }

    fun emplace(groupId: Int, parentId: Int, group: String, fields: Map<String, String?>): FieldGroup {
        val fieldGroup = FieldGroup.newFieldGroup(groupId.toLong(), group, parentId.toLong())
        fieldGroup.fields = fields
        add(fieldGroup)
        return fieldGroup
    }

    fun clear() = fieldGroups.clear()

    fun findById(id: Long): FieldGroup? {
        val gFieldGroup = fieldGroups.firstOrNull { it.id == id }
        return if (gFieldGroup == null) null else FieldGroup.box(gFieldGroup)
    }

    fun deepCopy(): PageModel {
        return PageModel(fieldGroups.mapTo(mutableListOf()) { GFieldGroup.newBuilder(it).build() })
    }

    companion object {
        @JvmStatic
        fun box(fieldGroups: MutableList<GFieldGroup>): PageModel {
            return PageModel(fieldGroups)
        }
    }
}
