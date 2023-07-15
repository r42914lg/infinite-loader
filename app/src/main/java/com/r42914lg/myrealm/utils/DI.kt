package com.r42914lg.myrealm.utils

import kotlin.reflect.KClass

object ServiceLocator {
    val serviceDictionary = mutableMapOf<KClass<*>, () -> Any>()

    inline fun <reified T: Any> register(noinline factory: () -> T) {
        serviceDictionary[T::class] = factory
    }

    inline fun <reified T: Any> resolve(): T {
        if (!serviceDictionary.containsKey(T::class))
            throw IllegalArgumentException("Service ${T::class.simpleName} not registered")

        return serviceDictionary[T::class]!!.invoke() as T
    }
}