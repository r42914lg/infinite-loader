package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

interface Repository {
    suspend fun getItems(page: Int): ItemChunkDto<Item>
    suspend fun addItems(chunk: List<Item>)
    suspend fun clearItems()
}

class LocalRepository private constructor(): Repository {
    override suspend fun getItems(page: Int): ItemChunkDto<Item> {
        val res = DbInteractor.read()

        val pageData = res.filter {
            it.id >= page * ITEMS_PER_PAGE + 1 && it.id < (page + 1) * ITEMS_PER_PAGE + 1
        }
        val nextPageData = res.filter {
            it.id >= (page + 1) * ITEMS_PER_PAGE + 1 && it.id < (page + 2) * ITEMS_PER_PAGE + 1
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

class ReactiveLocalRepo private constructor() : ReactiveRepository {
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

    companion object {
        fun getInstance(): ReactiveRepository =
            ReactiveLocalRepo()
    }
}

class ReactiveLocalRepoInMem private constructor() : ReactiveRepository {

    private val data = mutableListOf<ItemEntityRoom>()
    private val dataFlow = MutableSharedFlow<List<ItemEntityRoom>>(replay = 1)
    private val cs = CoroutineScope(SupervisorJob())
    override fun getItems(): Flow<List<ItemEntityRoom>> {
        cs.launch {
            dataFlow.emit(data)
        }

        return dataFlow
    }

    override suspend fun addItems(chunk: List<ItemEntityRoom>) {
        delay(50)
        data += chunk
        dataFlow.emit(data)
    }

    override suspend fun clearItems() {
        delay(50)
        data.clear()
        dataFlow.emit(data)
    }

    companion object {
        fun getInstance(): ReactiveRepository =
            ReactiveLocalRepoInMem()
    }
}
