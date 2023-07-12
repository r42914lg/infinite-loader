package com.r42914lg.myrealm.domain

import kotlinx.coroutines.flow.Flow

interface ReactiveLoader<T> {
    class State<T>(
        val data: T,
        val isLoading: Boolean,
        val isError: Boolean,
    )

    data class InnerState<T>(
        val page: Int,
        val isLoadingFromRemote: Boolean,
        val hasMoreInRemote: Boolean,
        val pullToRefreshInProgress: Boolean,
        val remoteError: Boolean,
    )

    val state: Flow<State<Flow<T>>>

    fun load()
    fun resetAndLoad()
    fun pullToRefresh()
    fun onClear()
}
