package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.domain.ItemChunkDto
import com.r42914lg.myrealm.utils.Result
import com.r42914lg.myrealm.utils.ServiceLocator
import com.r42914lg.myrealm.utils.runOperationCatching

interface RemoteApi {
    suspend fun getItems(page: Int): ItemChunkDto<Item>
}

class RemoteDataSource private constructor(
    private val api: RemoteApi,
) {
    suspend fun getItems(page: Int): Result<ItemChunkDto<Item>, Throwable> = runOperationCatching {
        api.getItems(page)
    }

    companion object {
        fun getInstance(): RemoteDataSource =
            RemoteDataSource(ServiceLocator.resolve())
    }
}

class TestApiImpl private constructor(): RemoteApi {
    override suspend fun getItems(page: Int): ItemChunkDto<Item> {
        val startIndex = page * ITEMS_PER_PAGE
        return ItemChunkDto(
            REMOTE_ITEMS.subList(startIndex, startIndex + ITEMS_PER_PAGE),
            page,
            page + 1 < REMOTE_PAGES_COUNT,
        )
    }

    companion object {
        fun getInstance(): RemoteApi =
            TestApiImpl()
    }
}
