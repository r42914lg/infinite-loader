package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.MyApp.Companion.CHUNK_SIZE
import com.r42914lg.myrealm.domain.ItemEntity
import io.realm.Realm

object DbInteractor {
    fun read(): List<ItemEntity> {
        val db = Realm.getDefaultInstance()
        return db.where(ItemEntity::class.java)
            .findAll()
    }

    fun addOrUpdate(items: List<ItemEntity>) {
        val db = Realm.getDefaultInstance()
        db.beginTransaction()
        items.forEach {
            db.copyToRealmOrUpdate(it)
        }
        db.commitTransaction()
    }

    fun deleteAll() {
        val db = Realm.getDefaultInstance()
        db.where(ItemEntity::class.java)
            .findAll()
            .forEach {
                it?.deleteFromRealm()
            }
    }
}
