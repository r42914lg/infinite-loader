package com.r42914lg.myrealm.domain

import com.r42914lg.myrealm.data.LOCAL_ITEMS
import com.r42914lg.myrealm.data.RemoteDataSource
import com.r42914lg.myrealm.data.Repository
import com.r42914lg.myrealm.domain.Loader.*
import com.r42914lg.myrealm.utils.ServiceLocator
import com.r42914lg.myrealm.utils.doOnError
import com.r42914lg.myrealm.utils.doOnSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class BasicLoader private constructor(
    private val defaultData: List<Item>,
    private val remoteDataSource: RemoteDataSource,
    private val localRepository: Repository,
) : Loader<List<Item>> {

    data class InnerState<T>(
        val currentData: T,
        val isLoadingFromCache: Boolean,
        val isLoadingFromRemote: Boolean,
        val hasMoreInCache: Boolean,
        val hasMoreInRemote: Boolean,
        val pullToRefreshInProgress: Boolean,
        val remoteError: Boolean,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val cs = CoroutineScope(SupervisorJob())

    private val _state: MutableStateFlow<InnerState<ItemChunkDto<Item>>> =
        MutableStateFlow(getDefaultInnerState(defaultData))

    override val state: Flow<State<List<Item>>>
        get() = _state.map {
            State(
                data = it.currentData.items,
                isLoading = it.isLoadingFromCache || it.isLoadingFromRemote,
                isError = it.remoteError
            )
        }

    init {
        cs.launch {
            localRepository.addItems(LOCAL_ITEMS)
            load()
        }
    }

    override fun load() {
        launchWork { state ->
            launchLoad(state = state)
        }
    }

    override fun resetAndLoad() {
        launchWork {
            launchLoad(state = getDefaultInnerState(defaultData))
        }
    }

    override fun pullToRefresh() {
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

    private fun launchLoad(state: InnerState<ItemChunkDto<Item>>): InnerState<ItemChunkDto<Item>> {
        if (state.hasMoreInCache && !state.isLoadingFromCache)
            return launchCacheLoad(state)

        return if (state.hasMoreInRemote
            && !state.isLoadingFromCache
            && !state.isLoadingFromRemote
            && !state.remoteError
        ) {
            launchRemoteLoad(state)
        } else
            state
    }


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

    companion object {
        fun getInstance(): Loader<List<Item>> =
            BasicLoader(
                listOf(),
                ServiceLocator.resolve(),
                ServiceLocator.resolve(),
            )
    }
}

