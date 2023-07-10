package com.r42914lg.myrealm.domain

import kotlinx.coroutines.flow.Flow

interface Loader<T> {
    class State<T>(
        val data: T,
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
    val currentData: T,
    val isLoadingFromCache: Boolean,
    val isLoadingFromRemote: Boolean,
    val hasMoreInCache: Boolean,
    val hasMoreInRemote: Boolean,
    val pullToRefreshInProgress: Boolean,
    val remoteError: Boolean,
)