package com.r42914lg.myrealm.domain

import com.r42914lg.myrealm.data.LocalRepository
import com.r42914lg.myrealm.data.RemoteDataSource
import com.r42914lg.myrealm.utils.doOnError
import com.r42914lg.myrealm.utils.doOnSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

interface Loader<T> {

    class PageableData<T>(
        val data: List<T>,
        val nextPage: Int,
        val hasMore: Boolean,
    )

    class State<T>(
        val data: PageableData<T>,
        val isLoading: Boolean,
        val isError: Boolean,
    )

    val state: Flow<State<T>>

    suspend fun load()
    suspend fun resetAndLoad()
    suspend fun pullToRefresh()

    fun onClear()
}

data class InnerState<T>(
    val currentData: Loader.PageableData<T>,
    val isLoadingFromCache: Boolean,
    val isLoadingFromRemote: Boolean,
    val hasMoreInCache: Boolean,
    val hasMoreInRemote: Boolean,
    val pullToRefreshInProgress: Boolean,
    val remoteError: Boolean,
)

class LoaderImpl(
    defaultData: List<Item>,
    private val remoteDataSource: RemoteDataSource,
    private val localRepository: LocalRepository,
) : Loader<Item> {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val cs = CoroutineScope(SupervisorJob())

    private val _state: MutableStateFlow<InnerState<Item>> =
        MutableStateFlow(getDefaultInnerState(defaultData))

    override val state: Flow<Loader.State<Item>>
        get() = _state.map {
            Loader.State(
                data = it.currentData,
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
        TODO("Not yet implemented")
    }

    override suspend fun pullToRefresh() {
        TODO("Not yet implemented")
    }

    override fun onClear() {
        TODO("Not yet implemented")
    }

    private fun launchWork(f: (InnerState<Item>) -> InnerState<Item>) {
        cs.launch(workDispatcher) {
            _state.value = f(_state.value)
        }
    }

    private fun launchLoad(state: InnerState<Item>): InnerState<Item> =
        if (state.hasMoreInCache) {
            launchCacheLoad(state)
        } else if (state.hasMoreInRemote
            && !state.isLoadingFromCache
            && !state.isLoadingFromRemote
            && !state.remoteError) {

            launchRemoteLoad(state)
        } else
            state


    private fun launchRemoteLoad(state: InnerState<Item>): InnerState<Item> {
        val pageToLoad = state.currentData.nextPage
        cs.launch(Dispatchers.IO) {
            remoteDataSource.getItems(pageToLoad * LIMIT, LIMIT)
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
                        onRemoteLoaded(
                            Loader.PageableData(
                                data = chunk,
                                nextPage = it.currentData.nextPage + 1,
                                hasMore = chunk.isNotEmpty(),
                            ),
                            it
                        )
                    }
                }
        }
        return state.copy(
            isLoadingFromRemote = true
        )
    }

    private fun launchCacheLoad(state: InnerState<Item>): InnerState<Item> {
        val pageToLoad = state.currentData.nextPage
        cs.launch(Dispatchers.IO) {
            val chunk = localRepository.getItems(pageToLoad * LIMIT, LIMIT)
            launchWork {
                onCacheLoaded(
                    Loader.PageableData(
                        data = chunk,
                        nextPage = it.currentData.nextPage + 1,
                        hasMore = chunk.isNotEmpty(),
                    ), it)
            }
        }
        return state.copy(
            isLoadingFromCache = true
        )
    }

    private fun onCacheLoaded(
        chunk: Loader.PageableData<Item>,
        state: InnerState<Item>,
    ): InnerState<Item> {
        val mergedData = state.currentData.data + chunk.data
        return state.copy(
            currentData = Loader.PageableData(
                mergedData,
                state.currentData.nextPage + 1,
                chunk.hasMore
            ),
            isLoadingFromCache = false,
            hasMoreInCache = chunk.hasMore,
        )
    }

    private fun onRemoteLoaded(
        chunk: Loader.PageableData<Item>,
        state: InnerState<Item>,
    ): InnerState<Item> {
        val mergedData = state.currentData.data + chunk.data
        return state.copy(
            currentData = Loader.PageableData(
                mergedData,
                state.currentData.nextPage + 1,
                chunk.hasMore
            ),
            pullToRefreshInProgress = false,
            isLoadingFromRemote = false,
            remoteError = false,
            hasMoreInRemote = chunk.hasMore,
        )
    }

    private fun <T> getDefaultInnerState(defaultData: List<T>): InnerState<T> =
        InnerState(
            Loader.PageableData(defaultData, 0, true),
            isLoadingFromCache = false,
            isLoadingFromRemote = false,
            hasMoreInCache = false,
            hasMoreInRemote = false,
            pullToRefreshInProgress = false,
            remoteError = false,
        )

    companion object {
        const val LIMIT = 5
    }
}