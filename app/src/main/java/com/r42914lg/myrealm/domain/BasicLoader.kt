package com.r42914lg.myrealm.domain

import com.r42914lg.myrealm.data.LocalRepository
import com.r42914lg.myrealm.data.RemoteDataSource
import com.r42914lg.myrealm.utils.doOnError
import com.r42914lg.myrealm.utils.doOnSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class BasicLoader(
    private val defaultData: List<Item>,
    private val remoteDataSource: RemoteDataSource,
    private val localRepository: LocalRepository,
) : Loader<List<Item>> {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val cs = CoroutineScope(SupervisorJob())

    private val _state: MutableStateFlow<InnerState<ItemChunkDto<Item>>> =
        MutableStateFlow(getDefaultInnerState(defaultData))

    override val state: Flow<Loader.State<List<Item>>>
        get() = _state.map {
            Loader.State(
                data = it.currentData.items,
                isLoading = it.isLoadingFromCache || it.isLoadingFromRemote,
                isError = it.remoteError
            )
        }

    override suspend fun load() {
        launchWork { state ->
            launchLoad(state = state)
        }
    }

    override suspend fun resetAndLoad() {
        launchWork {
            launchLoad(state = getDefaultInnerState(defaultData))
        }
    }

    override suspend fun pullToRefresh() {
        launchWork {
            if (it.pullToRefreshInProgress)
                it
            else
                pullToRefreshInternal(it)
        }
    }

    override fun onClear() {
        cs.cancel()
    }

    private fun pullToRefreshInternal(state: InnerState<ItemChunkDto<Item>>): InnerState<ItemChunkDto<Item>> =
        launchRemoteLoad(state.copy(
            pullToRefreshInProgress = true
        ))

    private fun launchWork(f: (InnerState<ItemChunkDto<Item>>) -> InnerState<ItemChunkDto<Item>>) {
        cs.launch(workDispatcher) {
            _state.value = f(_state.value)
        }
    }

    private fun launchLoad(state: InnerState<ItemChunkDto<Item>>): InnerState<ItemChunkDto<Item>> =
        if (state.hasMoreInCache && !state.isLoadingFromCache) {
            launchCacheLoad(state)
        } else if (state.hasMoreInRemote
            && !state.isLoadingFromCache
            && !state.isLoadingFromRemote
            && !state.remoteError) {

            launchRemoteLoad(state)
        } else
            state


    private fun launchRemoteLoad(state: InnerState<ItemChunkDto<Item>>): InnerState<ItemChunkDto<Item>> {
        val pageToLoad = if (state.pullToRefreshInProgress)
            0
        else
            state.currentData.page

        cs.launch(Dispatchers.IO) {
            remoteDataSource.getItems(pageToLoad)
                .doOnError {
                    launchWork {
                        it.copy(
                            isLoadingFromRemote = false,
                            remoteError = true,
                        )
                    }
                    return@launch
                }
                .doOnSuccess { chunk ->
                    launchWork {
                        onRemoteLoaded(chunk, it)
                    }
                }
        }
        return state.copy(
            isLoadingFromRemote = true
        )
    }

    private fun launchCacheLoad(state: InnerState<ItemChunkDto<Item>>): InnerState<ItemChunkDto<Item>> {
        val pageToLoad = state.currentData.page
        cs.launch(Dispatchers.IO) {
            val chunk = localRepository.getItems(pageToLoad)
            launchWork {
                onCacheLoaded(chunk, it)
            }
        }
        return state.copy(
            isLoadingFromCache = true
        )
    }

    private fun onCacheLoaded(
        chunk: ItemChunkDto<Item>,
        state: InnerState<ItemChunkDto<Item>>,
    ): InnerState<ItemChunkDto<Item>> {
        val mergedData = state.currentData.items + chunk.items
        return state.copy(
            currentData = ItemChunkDto(
                mergedData,
                chunk.page + 1,
                chunk.hasMore,
            ),
            isLoadingFromCache = false,
            hasMoreInCache = chunk.hasMore,
        )
    }

    private fun onRemoteLoaded(
        chunk: ItemChunkDto<Item>,
        state: InnerState<ItemChunkDto<Item>>,
    ): InnerState<ItemChunkDto<Item>> {

        val mergedData = if (state.pullToRefreshInProgress)
            chunk.items
        else
            state.currentData.items + chunk.items

        return state.copy(
            currentData = ItemChunkDto(
                mergedData,
                chunk.page + 1,
                chunk.hasMore,
            ),
            pullToRefreshInProgress = false,
            isLoadingFromRemote = false,
            remoteError = false,
            hasMoreInRemote = chunk.hasMore,
        )
    }

    private fun <T> getDefaultInnerState(defaultData: List<T>): InnerState<ItemChunkDto<T>> =
        InnerState(
            ItemChunkDto(defaultData, 0, true),
            isLoadingFromCache = false,
            isLoadingFromRemote = false,
            hasMoreInCache = true,
            hasMoreInRemote = true,
            pullToRefreshInProgress = false,
            remoteError = false,
        )
}
