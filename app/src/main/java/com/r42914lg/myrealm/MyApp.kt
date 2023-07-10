package com.r42914lg.myrealm

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }

    private fun initRealm() {
        Realm.init(this)
        val config = RealmConfiguration.Builder()
            .name("test.db")
            .schemaVersion(1)
            .build()

        Realm.setDefaultConfiguration(config)
    }

    companion object {
        const val CHUNK_SIZE = 3
    }
}