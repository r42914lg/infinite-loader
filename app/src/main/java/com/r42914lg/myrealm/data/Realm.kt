package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.domain.ItemEntityRealm
import io.realm.Realm

object DbInteractor {
    fun read(): List<ItemEntityRealm> {
        val db = Realm.getDefaultInstance()
        return db.where(ItemEntityRealm::class.java)
            .findAll()
    }

    fun addOrUpdate(items: List<ItemEntityRealm>) {
        val db = Realm.getDefaultInstance()
        db.beginTransaction()
        items.forEach {
            db.copyToRealmOrUpdate(it)
        }
        db.commitTransaction()
    }

    fun deleteAll() {
        val db = Realm.getDefaultInstance()
        db.where(ItemEntityRealm::class.java)
            .findAll()
            .forEach {
                it?.deleteFromRealm()
            }
    }
}
