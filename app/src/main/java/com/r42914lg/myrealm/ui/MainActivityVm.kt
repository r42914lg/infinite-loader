package com.r42914lg.myrealm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.domain.Loader
import kotlinx.coroutines.launch

class MainActivityVm(
    private val loader: Loader<Item>,
) : ViewModel() {

    val itemState = loader.state

    init {
        viewModelScope.launch {
            loader.load()
        }
    }

    fun resetAndLoad() {
        viewModelScope.launch {
            loader.resetAndLoad()
        }
    }

    fun pullToRefresh() {
        viewModelScope.launch {
            loader.pullToRefresh()
        }
    }

    override fun onCleared() {
        super.onCleared()
        loader.onClear()
    }
}