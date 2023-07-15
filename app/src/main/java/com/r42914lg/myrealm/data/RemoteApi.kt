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
            ITEMS.subList(startIndex, startIndex + ITEMS_PER_PAGE),
            page,
            page + 1 < REMOTE_PAGES_COUNT,
        )
    }

    companion object {
        fun getInstance(): RemoteApi =
            TestApiImpl()
    }
}

const val LOCAL_PAGES_COUNT = 2
const val REMOTE_PAGES_COUNT = 3
const val ITEMS_PER_PAGE = 10
val ITEMS = listOf(
    Item(1, "1", "1"),
    Item(2, "2", "2"),
    Item(3, "3", "3"),
    Item(4, "4", "4"),
    Item(5, "5", "5"),
    Item(6, "6", "6"),
    Item(7, "7", "7"),
    Item(8, "8", "8"),
    Item(9, "9", "9"),
    Item(10, "10", "10"),
    Item(11, "11", "11"),
    Item(12, "12", "12"),
    Item(13, "13", "13"),
    Item(14, "14", "14"),
    Item(15, "15", "15"),
    Item(16, "16", "16"),
    Item(17, "17", "17"),
    Item(18, "18", "18"),
    Item(19, "19", "19"),
    Item(20, "20", "20"),
    Item(21, "21", "21"),
    Item(22, "22", "22"),
    Item(23, "23", "23"),
    Item(24, "24", "24"),
    Item(25, "25", "25"),
    Item(26, "26", "26"),
    Item(27, "27", "27"),
    Item(28, "28", "28"),
    Item(29, "29", "29"),
    Item(30, "30", "30")
)
