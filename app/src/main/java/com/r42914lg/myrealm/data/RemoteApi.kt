package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.domain.ItemChunkDto
import com.r42914lg.myrealm.domain.ItemDto
import com.r42914lg.myrealm.utils.Result
import com.r42914lg.myrealm.utils.runOperationCatching

interface RemoteApi {
    suspend fun getItems(page: Int): ItemChunkDto<Item>
}

class RemoteDataSource(
    private val api: RemoteApi,
) {
    suspend fun getItems(page: Int): Result<ItemChunkDto<Item>, Throwable> = runOperationCatching {
        api.getItems(page)
    }
}