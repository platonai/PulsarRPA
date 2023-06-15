package ai.platon.pulsar.persist.model

import ai.platon.pulsar.persist.WebPage.u8
import ai.platon.pulsar.persist.gora.generated.GFieldGroup
import ai.platon.pulsar.persist.gora.generated.GPageModel

/**
 * Created by vincent on 17-8-3.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * The core concept of Page Model
 */
class PageModel(
        val pageModel: GPageModel
) {
    @get:Synchronized
    val fieldGroups get() = pageModel.fieldGroups

    @get:Synchronized
    val numGroups get() = fieldGroups.size

    @get:Synchronized
    val numFields get() = fieldGroups.sumOf { it.fields.size }

    @get:Synchronized
    val numNonNullFields get() = fieldGroups.sumOf { it.fields.count { it.value != null } }

    @get:Synchronized
    val numNonBlankFields get() = fieldGroups.sumOf { it.fields.count { !it.value.isNullOrBlank() } }

    @get:Synchronized
    val isEmpty: Boolean get() = fieldGroups.isEmpty()

    @get:Synchronized
    val isNotEmpty: Boolean get() = !isEmpty

    @get:Synchronized
    val boxedFieldGroups get() = fieldGroups.map { FieldGroup.box(it) }

    fun unbox() = pageModel

    @Synchronized
    fun firstOrNull(): FieldGroup? = fieldGroups.firstOrNull()?.let { FieldGroup.box(it) }

    @Synchronized
    fun lastOrNull(): FieldGroup? = fieldGroups.lastOrNull()?.let { FieldGroup.box(it) }

    @Synchronized
    operator fun get(index: Int): FieldGroup? = fieldGroups[index]?.let { FieldGroup.box(it) }

    @Synchronized
    fun getValue(index: Int, name: String) = get(index)?.get(name)

    @Synchronized
    fun findById(groupId: Int): FieldGroup? {
        val gFieldGroup = fieldGroups.firstOrNull { it.id == groupId.toLong() }
        return if (gFieldGroup == null) null else FieldGroup.box(gFieldGroup)
    }

    @Synchronized
    fun findValueById(groupId: Int, name: String): String? = findById(groupId)?.get(name)

    @Synchronized
    fun add(fieldGroup: FieldGroup) {
        fieldGroups.add(fieldGroup.unbox())
        pageModel.setDirty()
    }

    @Synchronized
    fun add(index: Int, fieldGroup: FieldGroup) {
        fieldGroups.add(index, fieldGroup.unbox())
        pageModel.setDirty()
    }

    @Deprecated("Inappropriate name", ReplaceWith("set(groupId, name, value)"))
    fun add(groupId: Int, name: String, value: String) = set(groupId, name, value)

    @Synchronized
    fun set(groupId: Int, name: String, value: String) {
        val group = findById(groupId)
        val fields = group?.unbox()?.fields ?: mutableMapOf()
        fields[u8(name)] = value

        val parentId = group?.parentId?.toInt() ?: 0
        val groupName = group?.name ?: ""
        emplace0(groupId, parentId, groupName, fields)
    }

    @Synchronized
    fun emplace(groupId: Int, fields: Map<String, String?>): FieldGroup {
        return emplace(groupId, 0, "", fields)
    }

    @Synchronized
    fun emplace(groupId: Int, groupName: String, fields: Map<String, String?>): FieldGroup {
        return emplace(groupId, 0, groupName, fields)
    }

    @Synchronized
    fun emplace(groupId: Int, parentId: Int, groupName: String, fields: Map<String, String?>): FieldGroup {
        val f = fields.entries.associate { u8(it.key)!! to it.value }
        return emplace0(groupId, parentId, groupName, f)
    }

    @Synchronized
    fun remove(groupId: Int) {
        fieldGroups.removeIf { it.id == groupId.toLong() }
        pageModel.setDirty()
    }

    @Synchronized
    fun remove(groupId: Int, key: String): String? {
        val gFieldGroup = findRawById(groupId) ?: return null
        val oldValue = gFieldGroup.fields.remove(u8(key)) ?: return null

        gFieldGroup.setDirty()
        // Can we ignore pageModel's dirty flag?
        pageModel.setDirty()

        return oldValue.toString()
    }

    @Synchronized
    fun clear() {
        fieldGroups.clear()
        pageModel.setDirty()
    }

    @Synchronized
    fun deepCopy(): PageModel {
        val other = GPageModel.newBuilder(pageModel).build()
        return PageModel(other)
    }

    private fun findRawById(groupId: Int) = fieldGroups.firstOrNull { it.id == groupId.toLong() }

    private fun removeRawById(groupId: Int, key: String): CharSequence? {
        return findRawById(groupId)?.fields?.remove(u8(key))
    }

    @Synchronized
    private fun emplace0(
        groupId: Int, parentId: Int, groupName: String, fields: Map<out CharSequence, CharSequence?>
    ): FieldGroup {
        var gFieldGroup = fieldGroups.firstOrNull { it.id == groupId.toLong() }
        if (gFieldGroup == null) {
            gFieldGroup = FieldGroup.newGFieldGroup(groupId, groupName, parentId)
            fieldGroups.add(gFieldGroup)
        }

        // fieldGroup.fields = fields
        gFieldGroup.fields.putAll(fields)
        gFieldGroup.setDirty()
        pageModel.setDirty()

        return FieldGroup.box(gFieldGroup)
    }

    companion object {
        @JvmStatic
        fun box(pageModel: GPageModel): PageModel {
            return PageModel(pageModel)
        }
    }
}
