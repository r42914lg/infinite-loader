package com.r42914lg.myrealm.domain

import androidx.room.Entity
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

@Entity
data class ItemEntityRoom(
    @androidx.room.PrimaryKey
    var id: Int = 0,
    var prop1: String = "",
    var prop2: String = "",
)

open class ItemEntityRealm : RealmObject() {
    @PrimaryKey
    var id: Int = 0
    var prop1Renamed: String = ""
    var prop3New: String = ""
}

fun List<Item>.toRoomEntity() =
    map {
        ItemEntityRoom(
            id = it.id,
            prop1 = it.prop1,
            prop2 = it.prop2,
        )
    }

fun List<Item>.toRealmEntity() =
    map {
        ItemEntityRealm().apply {
            id = it.id
            prop1Renamed = it.prop1
            prop3New = it.prop2
        }
    }

fun List<ItemEntityRoom>.roomToDomain() =
    map {
        Item(
            id = it.id,
            prop1 = it.prop1,
            prop2 = it.prop2
        )
    }

fun List<ItemEntityRealm>.realmToDomain() =
    map {
        Item(
            id = it.id,
            prop1 = it.prop1Renamed,
            prop2 = it.prop3New
        )
    }