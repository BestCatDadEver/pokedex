package com.carlos.pokedex.core.database

import androidx.room.TypeConverter
import com.carlos.pokedex.dashboard.domain.model.PokemonStat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type
    private val statListType = object : TypeToken<List<PokemonStat>>() {}.type

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value.orEmpty())

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else gson.fromJson(value, stringListType)

    @TypeConverter
    fun fromStatList(value: List<PokemonStat>?): String = gson.toJson(value.orEmpty())

    @TypeConverter
    fun toStatList(value: String?): List<PokemonStat> =
        if (value.isNullOrEmpty()) emptyList() else gson.fromJson(value, statListType)
}
