package com.r42914lg.myrealm.domain

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

data class Item(
    val id: Int,
    val prop1: String,
    val prop2: String,
)

data class ItemChunkDto<T>(
    val items: List<T>,
    val page: Int,
    val hasMore: Boolean,
)

data class ItemEntity(
    @PrimaryKey
    val id: Int,
    val prop1: String,
    val prop2: String,
    ) : RealmObject()

fun List<Item>.toEntity() =
    map {
        ItemEntity(
            id = it.id,
            prop1 = it.prop1,
            prop2 = it.prop2,
        )
    }

fun List<ItemEntity>.toDomain() =
    map {
        Item(
            id = it.id,
            prop1 = it.prop1,
            prop2 = it.prop2
        )
    }