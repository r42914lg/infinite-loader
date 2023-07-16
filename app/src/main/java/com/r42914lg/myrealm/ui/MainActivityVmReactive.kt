package com.r42914lg.myrealm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.r42914lg.myrealm.domain.Item
import com.r42914lg.myrealm.domain.Loader
import com.r42914lg.myrealm.utils.ServiceLocator
import kotlinx.coroutines.launch

class MainActivityVmReactive(
    private val loader: Loader<List<Item>>,
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

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                return MainActivityVmReactive(
                    ServiceLocator.resolve()
                ) as T
            }
        }
    }
}
