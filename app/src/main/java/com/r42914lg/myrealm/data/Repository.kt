package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.domain.*
import kotlinx.coroutines.flow.Flow

interface Repository {
    suspend fun getItems(page: Int): ItemChunkDto<Item>
    suspend fun addItems(chunk: List<Item>)
    suspend fun clearItems()
}

class LocalRepository private constructor(): Repository {
    override suspend fun getItems(page: Int): ItemChunkDto<Item> {
        val res = DbInteractor.read()

        val pageData = res.filter {
            it.id >= page * ITEMS_PER_PAGE && it.id >= page * (ITEMS_PER_PAGE + 1)
        }
        val nextPageData = res.filter {
            it.id >= page * (ITEMS_PER_PAGE + 1) && it.id >= page * (ITEMS_PER_PAGE + 2)
        }

        return ItemChunkDto(
            pageData.realmToDomain(),
            page,
            nextPageData.isNotEmpty()
        )
    }

    override suspend fun addItems(chunk: List<Item>) {
        DbInteractor.addOrUpdate(chunk.toRealmEntity())
    }

    override suspend fun clearItems() {
        DbInteractor.deleteAll()
    }

    companion object {
        fun getInstance(): Repository =
            LocalRepository()
    }
}

class LocalRepositoryInMem private constructor(): Repository {

    private val data = mutableListOf<Item>()
    override suspend fun getItems(page: Int): ItemChunkDto<Item> {
        val startIndex = page * ITEMS_PER_PAGE
        return ItemChunkDto(
            data.subList(startIndex, startIndex + ITEMS_PER_PAGE) as List<Item>,
            page,
            page + 1 < LOCAL_PAGES_COUNT,
        )
    }

    override suspend fun addItems(chunk: List<Item>) {
        data.addAll(chunk)
    }

    override suspend fun clearItems() {
        data.clear()
    }

    companion object {
        fun getInstance(): Repository =
            LocalRepositoryInMem()
    }
}

/**
 * Reactive
 */

interface ReactiveRepository {
    fun getItems(): Flow<List<ItemEntityRoom>>
    suspend fun addItems(chunk: List<ItemEntityRoom>)
    suspend fun clearItems()
}

class ReactiveLocalRepo(

) : ReactiveRepository {
    override fun getItems(): Flow<List<ItemEntityRoom>> =
        MyDatabase.getDatabase().ItemDao()
            .getItems()


    override suspend fun addItems(chunk: List<ItemEntityRoom>) {
        MyDatabase.getDatabase().ItemDao()
            .insertItem(chunk)
    }

    override suspend fun clearItems() {
        MyDatabase.getDatabase().ItemDao()
            .clearAll()
    }
}
