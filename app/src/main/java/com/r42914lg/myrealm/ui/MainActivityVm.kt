package com.r42914lg.myrealm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.domain.Loader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

class MainActivityVm(
    private val loader: Loader<List<Item>>,
) : ViewModel() {

    private val itemState = loader.state
    val uiState: Flow<UiState>
        get() =
            itemState.transform {
                if (it.isError)
                    UiState.Error
                else if (it.isLoading)
                    UiState.Loading
                else
                    UiState.RenderItems(it.data)
            }

    init {
        load()
    }

    fun onAction(action: MainActivity.Action) {
        when (action) {
            MainActivity.Action.Load -> load()
            MainActivity.Action.PullToRefresh -> pullToRefresh()
            MainActivity.Action.ResetAndLoad -> resetAndLoad()
        }
    }

    private fun load() {
        viewModelScope.launch {
            loader.load()
        }
    }

    private fun resetAndLoad() {
        viewModelScope.launch {
            loader.resetAndLoad()
        }
    }

    private fun pullToRefresh() {
        viewModelScope.launch {
            loader.pullToRefresh()
        }
    }

    override fun onCleared() {
        super.onCleared()
        loader.onClear()
    }

    sealed interface UiState {
        object Loading : UiState
        object Error : UiState
        class RenderItems(val data: List<Item>) : UiState
    }
}