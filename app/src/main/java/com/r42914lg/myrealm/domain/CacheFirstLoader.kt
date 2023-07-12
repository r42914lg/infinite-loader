package com.r42914lg.myrealm.domain

import com.r42914lg.myrealm.data.ReactiveRepository
import com.r42914lg.myrealm.data.RemoteDataSource
import com.r42914lg.myrealm.domain.ReactiveLoader.*
import com.r42914lg.myrealm.utils.doOnError
import com.r42914lg.myrealm.utils.doOnSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CacheFirstLoader(
    private val remoteDataSource: RemoteDataSource,
    private val localRepository: ReactiveRepository,
) : ReactiveLoader<List<Item>> {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val cs = CoroutineScope(SupervisorJob())

    private val _state: MutableStateFlow<InnerState<ItemChunkDto<Item>>> =
        MutableStateFlow(getDefaultInnerState())

    override val state: Flow<State<Flow<List<Item>>>>
        get() = _state.map {
            State(
                data = localRepository.getItems(),
                isLoading = it.isLoadingFromRemote,
                isError = it.remoteError
            )
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
        return if (state.hasMoreInRemote
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
        state: InnerState<ItemChunkDto<Item>>,
    ): InnerState<ItemChunkDto<Item>> {

        cs.launch {
            if (state.pullToRefreshInProgress)
                localRepository.clearItems()

            localRepository.addItems(chunk.items)
        }

        return state.copy(
            page = chunk.page + 1,
            pullToRefreshInProgress = false,
            isLoadingFromRemote = false,
            remoteError = false,
            hasMoreInRemote = chunk.hasMore,
        )
    }

    private fun <T> getDefaultInnerState(): InnerState<ItemChunkDto<T>> =
        InnerState(
            page = 0,
            isLoadingFromRemote = false,
            hasMoreInRemote = true,
            pullToRefreshInProgress = false,
            remoteError = false,
        )
}
