package com.r42914lg.myrealm.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.*
import com.r42914lg.myrealm.domain.ItemEntityRoom
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        ItemEntityRoom::class,
    ],
    version = 1,
)
abstract class MyDatabase : RoomDatabase() {

    abstract fun ItemDao(): ItemDao

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var ctx: Context? = null

        fun setAppContext(appCtx: Context) {
            ctx = appCtx
        }

        @SuppressLint("StaticFieldLeak")
        @Volatile private var INSTANCE: MyDatabase? = null

        fun getDatabase(): MyDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(ctx!!).also { INSTANCE = it } }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(appContext, MyDatabase::class.java, "test")
                .fallbackToDestructiveMigration()
                .build()
    }
}

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(entities: List<ItemEntityRoom>)

    @Query("SELECT * FROM ItemEntityRoom")
    fun getItems(): Flow<List<ItemEntityRoom>>

    @Query("DELETE FROM ItemEntityRoom")
    fun clearAll()
}