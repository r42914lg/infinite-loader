package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.utils.Result
import com.r42914lg.myrealm.utils.runOperationCatching

interface RemoteApi {
    suspend fun getItems(offset: Int, limit: Int): List<Item>
}

class RemoteDataSource(
    private val api: RemoteApi,
) {
    suspend fun getItems(offset: Int, limit: Int): Result<List<Item>, Throwable> = runOperationCatching {
        api.getItems(offset, limit)
    }
}