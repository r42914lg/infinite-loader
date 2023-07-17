package com.r42914lg.myrealm.domain

import com.r42914lg.myrealm.data.LOCAL_ITEMS
import com.r42914lg.myrealm.data.ReactiveRepository
import com.r42914lg.myrealm.data.RemoteDataSource
import com.r42914lg.myrealm.utils.ServiceLocator
import com.r42914lg.myrealm.utils.doOnError
import com.r42914lg.myrealm.utils.doOnSuccess
import com.r42914lg.myrealm.domain.Loader.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CacheFirstLoader private constructor(
    private val remoteDataSource: RemoteDataSource,
    private val localRepository: ReactiveRepository,
) : Loader<List<Item>> {

    data class InnerState(
        val page: Int,
        val isLoadingFromRemote: Boolean,
        val isStoringToCache: Boolean,
        val hasMoreInRemote: Boolean,
        val pullToRefreshInProgress: Boolean,
        val remoteError: Boolean,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val cs = CoroutineScope(SupervisorJob())

    private val _state: MutableStateFlow<InnerState> =
        MutableStateFlow(getDefaultInnerState())

    override val state: Flow<State<List<Item>>>
        get() = _state.map {
            val currentData: List<ItemEntityRoom> = localRepository.getItems().first()
            State(
                data = currentData.roomToDomain(),
                isLoading = it.isLoadingFromRemote || it.isStoringToCache,
                isError = it.remoteError
            )
        }

    init {
        cs.launch {
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
            launchLoad(state = getDefaultInnerState())
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

    private fun pullToRefreshInternal(state: InnerState): InnerState =
        launchRemoteLoad(state.copy(
            pullToRefreshInProgress = true
        ))

    private fun launchWork(f: (InnerState) -> InnerState) {
        cs.launch(workDispatcher) {
            _state.value = f(_state.value)
        }
    }

    private fun launchLoad(state: InnerState): InnerState {
        return if (state.hasMoreInRemote
            && !state.isLoadingFromRemote
            && !state.remoteError
        ) {
            launchRemoteLoad(state)
        } else
            state
    }


    private fun launchRemoteLoad(state: InnerState): InnerState {
        val pageToLoad = if (state.pullToRefreshInProgress)
            0
        else
            state.page

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

    private fun onRemoteLoaded(
        chunk: ItemChunkDto<Item>,
        state: InnerState,
    ): InnerState {

        cs.launch(Dispatchers.IO) {
            if (state.pullToRefreshInProgress)
                localRepository.clearItems()

            localRepository.addItems(chunk.items.toRoomEntity())
            launchWork {
                it.copy(
                    isStoringToCache = false
                )
            }
        }

        return state.copy(
            page = chunk.page + 1,
            pullToRefreshInProgress = false,
            isLoadingFromRemote = false,
            isStoringToCache = true,
            remoteError = false,
            hasMoreInRemote = chunk.hasMore,
        )
    }

    private fun getDefaultInnerState(): InnerState =
        InnerState(
            page = 0,
            isLoadingFromRemote = false,
            isStoringToCache = false,
            hasMoreInRemote = true,
            pullToRefreshInProgress = false,
            remoteError = false,
        )

    companion object {
        fun getInstance(): Loader<List<Item>> =
            CacheFirstLoader(
                ServiceLocator.resolve(),
                ServiceLocator.resolve(),
            )
    }
}
