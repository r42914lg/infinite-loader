package com.r42914lg.myrealm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.domain.ReactiveLoader
import kotlinx.coroutines.launch

class MainActivityVmReactive(
    private val loader: ReactiveLoader<List<Item>>,
) : ViewModel() {

    val itemState = loader.state

    fun onAction(event: MainActivityEvent) {
        when (event) {
            MainActivityEvent.Load -> load()
            MainActivityEvent.PullToRefresh -> pullToRefresh()
            MainActivityEvent.ResetAndLoad -> resetAndLoad()
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
}
