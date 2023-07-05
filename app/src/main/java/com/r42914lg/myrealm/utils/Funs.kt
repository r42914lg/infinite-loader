package com.r42914lg.myrealm.utils

import android.util.Log
import com.getkeepsafe.relinker.BuildConfig

inline fun <reified T> T.log(message: String) {
    if (BuildConfig.DEBUG)
        Log.d("LG >>> " + T::class.java.simpleName, message)
}