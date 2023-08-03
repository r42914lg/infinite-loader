package com.r42914lg.myrealm

import android.app.Application
import com.r42914lg.myrealm.data.*
import com.r42914lg.myrealm.domain.BasicLoader
import com.r42914lg.myrealm.migrations.DBMigrationHelper
import com.r42914lg.myrealm.utils.ServiceLocator
import io.realm.Realm
import io.realm.RealmConfiguration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        initRealm()
        initRoom()
        initDependencies()
    }

    private fun initRealm() {
        Realm.init(this)
        val config = RealmConfiguration.Builder()
            .name("test.db")
            .schemaVersion(2)
            .migration(DBMigrationHelper())
            .build()

        Realm.setDefaultConfiguration(config)
    }

    private fun initRoom() {
        MyDatabase.setAppContext(this)
    }

    private fun initDependencies() {
        ServiceLocator.register { TestApiImpl.getInstance() }
        ServiceLocator.register { RemoteDataSource.getInstance() }
        ServiceLocator.register { LocalRepository.getInstance() }
        ServiceLocator.register { BasicLoader.getInstance() }
    }
}
