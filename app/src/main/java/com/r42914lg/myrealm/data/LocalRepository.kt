package com.r42914lg.myrealm.data

import com.r42914lg.myrealm.domain.Item


interface Repository {
    suspend fun getItems(offset: Int, limit: Int): List<Item>
    suspend fun addItems(chunk: List<Item>)
}

class LocalRepository : Repository {
    override suspend fun getItems(offset: Int, limit: Int): List<Item> {
        TODO("Not yet implemented")
    }

    override suspend fun addItems(chunk: List<Item>) {
        TODO("Not yet implemented")
    }

}