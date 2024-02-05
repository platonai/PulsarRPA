package ai.platon.pulsar.persist.model

import ai.platon.pulsar.persist.WebPage.u8
import ai.platon.pulsar.persist.gora.generated.GPageModel

/**
 * Created by vincent on 17-8-3.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 *
 * The core concept of Page Model
 */
class PageModel(
    val pageModel: GPageModel
) {
    companion object {
        const val DEFAULT_GROUP_ID = 1

        @JvmStatic
        fun box(pageModel: GPageModel): PageModel {
            return PageModel(pageModel)
        }
    }

    /**
     * TODO: Find out a way to partially update nested fields.
     * */
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

    /**
     * Return the first field group.
     * */
    @Synchronized
    fun firstOrNull(): FieldGroup? = fieldGroups.firstOrNull()?.let { FieldGroup.box(it) }

    /**
     * Return the last field group.
     * */
    @Synchronized
    fun lastOrNull(): FieldGroup? = fieldGroups.lastOrNull()?.let { FieldGroup.box(it) }

    /**
     * Return the n-th field group.
     * */
    @Synchronized
    operator fun get(index: Int): FieldGroup? = fieldGroups[index]?.let { FieldGroup.box(it) }

    /**
     * Get the n-th field group and retrieve the value associated with [name].
     * */
    @Synchronized
    fun getValue(index: Int, name: String) = get(index)?.get(name)

    /**
     * Find the field group whose id is [groupId].
     * */
    @Synchronized
    fun findGroup(groupId: Int): FieldGroup? {
        val gFieldGroup = fieldGroups.firstOrNull { it.id == groupId.toLong() }
        return if (gFieldGroup == null) null else FieldGroup.box(gFieldGroup)
    }

    /**
     * Find the field group whose id is [groupId] and retrieve the value associated with [name].
     * */
    @Synchronized
    fun findValue(groupId: Int, name: String): String? = findGroup(groupId)?.get(name)

    /**
     * Add a field group.
     * */
    @Synchronized
    fun add(fieldGroup: FieldGroup) {
        fieldGroups.add(fieldGroup.unbox())
        pageModel.setDirty()
    }

    /**
     * Add a field group.
     * */
    @Synchronized
    fun add(index: Int, fieldGroup: FieldGroup) {
        fieldGroups.add(index, fieldGroup.unbox())
        pageModel.setDirty()
    }

    /**
     * Set a field entry to field group whose id is [groupId].
     * */
    @Synchronized
    fun put(groupId: Int, name: String, value: String): Pair<FieldGroup, CharSequence?> {
        val group = findGroup(groupId)
        val parentId = group?.parentId?.toInt() ?: 0
        val groupName = group?.name ?: ""
        return put0(groupId, parentId, groupName, name, value)
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
        return emplace0(groupId, parentId, groupName, fields)
    }

    @Synchronized
    fun remove(groupId: Int) {
        fieldGroups.removeIf { it.id == groupId.toLong() }
        pageModel.setDirty()
    }

    /**
     * Remove the entry associated with [key].
     *
     * @param groupId The group id.
     * @param key The key.
     * @return the old value.
     * */
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
        groupId: Int, parentId: Int, groupName: String, fields: Map<String, String?>
    ): FieldGroup {
        var gFieldGroup = fieldGroups.firstOrNull { it.id == groupId.toLong() }
        if (gFieldGroup == null) {
            gFieldGroup = FieldGroup.newGFieldGroup(groupId, groupName, parentId)
            fieldGroups.add(gFieldGroup)
        }

        gFieldGroup.fields.clear()
        fields.entries.associateTo(gFieldGroup.fields) { u8(it.key) to it.value }

        gFieldGroup.setDirty()
        pageModel.setDirty()

        return FieldGroup.box(gFieldGroup)
    }

    @Synchronized
    private fun put0(
        groupId: Int, parentId: Int, groupName: String, name: String, value: String
    ): Pair<FieldGroup, CharSequence?> {
        var gFieldGroup = fieldGroups.firstOrNull { it.id == groupId.toLong() }
        if (gFieldGroup == null) {
            gFieldGroup = FieldGroup.newGFieldGroup(groupId, groupName, parentId)
            fieldGroups.add(gFieldGroup)
        }

        val u8key = u8(name)
        val oldValue = gFieldGroup.fields.put(u8key, value)
        gFieldGroup.setDirty()
        pageModel.setDirty()

        return FieldGroup.box(gFieldGroup) to oldValue
    }
}
