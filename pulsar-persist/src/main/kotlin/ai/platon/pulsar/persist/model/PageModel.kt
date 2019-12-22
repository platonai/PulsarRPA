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
class PageModel private constructor(private val fieldGroups: MutableList<GFieldGroup>) {
    fun unbox(): List<GFieldGroup> {
        return fieldGroups
    }

    fun first(): FieldGroup? {
        return if (isEmpty) null else get(0)
    }

    operator fun get(i: Int): FieldGroup {
        return FieldGroup.box(fieldGroups[i])
    }

    fun add(fieldGroup: FieldGroup): Boolean {
        return fieldGroups.add(fieldGroup.unbox())
    }

    fun add(i: Int, fieldGroup: FieldGroup) {
        fieldGroups.add(i, fieldGroup.unbox())
    }

    fun emplace(id: Long, parentId: Long, group: String, fields: Map<String, String>): FieldGroup {
        val fieldGroup = FieldGroup.newFieldGroup(id, group, parentId)
        fieldGroup.fields = fields
        add(fieldGroup)
        return fieldGroup
    }

    val isEmpty: Boolean
        get() = fieldGroups.isEmpty()

    fun size(): Int {
        return fieldGroups.size
    }

    fun clear() {
        fieldGroups.clear()
    }

    fun findById(id: Long): FieldGroup? {
        val gFieldGroup = CollectionUtils.find(fieldGroups) { fg: GFieldGroup -> fg.id == id }
        return if (gFieldGroup == null) null else FieldGroup.box(gFieldGroup)
    }

    companion object {
        @JvmStatic
        fun box(fieldGroups: MutableList<GFieldGroup>): PageModel {
            return PageModel(fieldGroups)
        }
    }

}