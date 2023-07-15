package com.r42914lg.myrealm.domain

import kotlinx.coroutines.flow.Flow

interface Loader<T> {
    data class State<T>(
        val data: T,
        val isLoading: Boolean,
        val isError: Boolean,
    )

    data class InnerState<T>(
        val currentData: T,
        val isLoadingFromCache: Boolean,
        val isLoadingFromRemote: Boolean,
        val hasMoreInCache: Boolean,
        val hasMoreInRemote: Boolean,
        val pullToRefreshInProgress: Boolean,
        val remoteError: Boolean,
    )

    val state: Flow<State<T>>

    fun load()
    fun resetAndLoad()
    fun pullToRefresh()
    fun onClear()
}
