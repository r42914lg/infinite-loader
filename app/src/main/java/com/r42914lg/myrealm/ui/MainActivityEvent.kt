package com.r42914lg.myrealm.ui

sealed interface MainActivityEvent {
    object Load : MainActivityEvent
    object PullToRefresh : MainActivityEvent
    object ResetAndLoad : MainActivityEvent
}