package com.r42914lg.myrealm.migrations

import com.r42914lg.myrealm.domain.ItemEntityRealm
import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import io.realm.RealmSchema

class DBMigrationHelper : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        migration1to2(realm.schema)
        migration2to3(realm.schema)
        migration3to4(realm.schema)
    }

    private fun migration1to2(schema: RealmSchema) {
        val userSchema = schema.get(ItemEntityRealm::class.java.simpleName)
        userSchema?.run {
            addField("prop3New", String::class.java, FieldAttribute.REQUIRED)
            renameField("prop1", "prop1Renamed")
        }
    }

    private fun migration2to3(schema: RealmSchema) {
        val userSchema = schema.get(ItemEntityRealm::class.java.simpleName)
        userSchema?.run {
            transform {
                it.set("prop3New", "M " + it.get<String>("prop1Renamed") + it.get<String>("prop2"))
            }
        }
    }

    private fun migration3to4(schema: RealmSchema) {
        val userSchema = schema.get(ItemEntityRealm::class.java.simpleName)
        userSchema?.run {
            removeField("prop2")
        }
    }
}
