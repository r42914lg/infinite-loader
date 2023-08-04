package com.r42914lg.myrealm.migrations

import com.r42914lg.myrealm.domain.ItemEntityRealm
import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import io.realm.RealmSchema

class DBMigrationHelper : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val schema = realm.schema
        var verInProgress = oldVersion

        if (verInProgress == 0L) {
            migration1to2(schema)
            verInProgress++
        }

        if (verInProgress == 1L) {
            migration2to3(schema)
            verInProgress++
        }

        if (verInProgress < newVersion)
            throw IllegalStateException("Migration missing from $oldVersion to $newVersion")
    }

    private fun migration1to2(schema: RealmSchema) {
        val userSchema = schema.get(ItemEntityRealm::class.java.simpleName)
        userSchema?.run {
            addField("prop3New", String::class.java, FieldAttribute.REQUIRED)
            renameField("prop1", "prop1Renamed")
            transform {
                it.set("prop3New", "M " + it.get<String>("prop1Renamed") + it.get<String>("prop2"))
            }
        }
    }

    private fun migration2to3(schema: RealmSchema) {
        val userSchema = schema.get(ItemEntityRealm::class.java.simpleName)
        userSchema?.run {
            removeField("prop2")
        }
    }
}
