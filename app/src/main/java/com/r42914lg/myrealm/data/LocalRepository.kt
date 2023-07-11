package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.MyApp.Companion.CHUNK_SIZE
import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.domain.ItemChunkDto
import com.r42914lg.myrealm.domain.toDomain
import com.r42914lg.myrealm.domain.toEntity
import io.realm.Realm
import io.realm.kotlin.toflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow

interface Repository {
    suspend fun getItems(page: Int): ItemChunkDto<Item>
    suspend fun addItems(chunk: List<Item>)
    suspend fun clearItems()
}

open class LocalRepository : Repository {
    override suspend fun getItems(page: Int): ItemChunkDto<Item> {
        val res = DbInteractor.read()

        val pageData = res.filter {
            it.id >= page * CHUNK_SIZE && it.id >= page * (CHUNK_SIZE + 1)
        }
        val nextPageData = res.filter {
            it.id >= page * (CHUNK_SIZE + 1) && it.id >= page * (CHUNK_SIZE + 2)
        }

        return ItemChunkDto(
            pageData.toDomain(),
            page,
            nextPageData.isNotEmpty()
        )
    }

    override suspend fun addItems(chunk: List<Item>) {
        DbInteractor.addOrUpdate(chunk.toEntity())
    }

    override suspend fun clearItems() {
        DbInteractor.deleteAll()
    }
}
