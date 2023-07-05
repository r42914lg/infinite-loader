package com.r42914lg.myrealm.data

import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class VisitInfo : RealmObject() {

    @PrimaryKey
    var id = UUID.randomUUID().toString()

    var visitCount: Int = 0

}

object DbInteractor {
    private fun read() {
        val db = Realm.getDefaultInstance()
        val visitInfo = db.where(VisitInfo::class.java).findFirst()
    }

    private fun update() {
        val db = Realm.getDefaultInstance()
        val visitInfo = db.where(VisitInfo::class.java).findFirst()

        db.beginTransaction()
        visitInfo?.apply {
            visitCount += 1
        }

        db.commitTransaction()
    }

    private fun delete() {
        val db = Realm.getDefaultInstance()
        val visitInfo = db.where(VisitInfo::class.java).findFirst()
        visitInfo?.deleteFromRealm()
    }
}
