package ai.platon.pulsar.persist.model

import ai.platon.pulsar.persist.PersistUtils.u8
import ai.platon.pulsar.persist.gora.generated.GFieldGroup

class FieldGroup private constructor(private val fieldGroup: GFieldGroup) {
    fun unbox(): GFieldGroup {
        return fieldGroup
    }

    var id: Long
        get() = fieldGroup.id
        set(id) {
            fieldGroup.id = id
        }

    var parentId: Long
        get() = fieldGroup.parentId
        set(id) {
            fieldGroup.parentId = id
        }

    /**
     * TODO: name is a internal field used by gora, choose another one
     * */
    var name: String
        get() = fieldGroup.name.toString()
        set(name) {
            fieldGroup.name = name
        }

    val fieldsCopy: Map<String, String?>
        get() = fieldGroup.fields.entries.associate { it.key.toString() to it.value?.toString() }

    operator fun get(key: String): String? {
        return fieldGroup.fields[u8(key)]?.toString()
    }

    operator fun set(key: String, value: String) {
        // Note: map
        // fields is Dirtyable
        fieldGroup.fields[u8(key)] = value
        fieldGroup.setDirty()
    }
    
    fun remove(key: String) {
        fieldGroup.fields.remove(u8(key))
        fieldGroup.setDirty()
    }

    fun clear() {
        fieldGroup.fields.clear()
        fieldGroup.setDirty()
    }
    
    override fun toString(): String {
        return FieldGroupFormatter(this).format()
    }

    companion object {
        @JvmOverloads
        fun newGFieldGroup(id: Int, name: String = "", parentId: Int = 0): GFieldGroup {
            val fieldGroup = GFieldGroup.newBuilder().build()
            fieldGroup.id = id.toLong()
            fieldGroup.parentId = parentId.toLong()
            fieldGroup.name = name
            return fieldGroup
        }

        @JvmOverloads
        fun newFieldGroup(id: Int, name: String, parentId: Int = 0): FieldGroup {
            return box(newGFieldGroup(id, name, parentId))
        }

        fun box(fieldGroup: GFieldGroup): FieldGroup {
            return FieldGroup(fieldGroup)
        }
    }
}
