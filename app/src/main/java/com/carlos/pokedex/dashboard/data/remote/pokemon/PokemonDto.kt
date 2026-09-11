package com.carlos.pokedex.dashboard.data.remote.pokemon

import com.google.gson.annotations.SerializedName

data class PokemonDto(
    val height: Long,
    val id: Long,
    val name: String,
    val sprites: Sprites,
    val weight: Long,
    val types: List<TypeSlot>?,
    val stats: List<StatSlot>?,
    val abilities: List<AbilitySlot>?,
)


data class Sprites(
    @SerializedName("back_default")
    val backDefault: String?,
    @SerializedName("back_female")
    val backFemale: Any?,
    @SerializedName("back_shiny")
    val backShiny: String?,
    @SerializedName("back_shiny_female")
    val backShinyFemale: Any?,
    @SerializedName("front_default")
    val frontDefault: String,
    @SerializedName("front_female")
    val frontFemale: Any?,
    @SerializedName("front_shiny")
    val frontShiny: String?,
    @SerializedName("front_shiny_female")
    val frontShinyFemale: Any?
)

data class NamedResource(
    val name: String,
    val url: String
)

data class TypeSlot(
    val slot: Int,
    val type: NamedResource
)

data class StatSlot(
    @SerializedName("base_stat")
    val baseStat: Int,
    val effort: Int,
    val stat: NamedResource
)

data class AbilitySlot(
    val ability: NamedResource,
    @SerializedName("is_hidden")
    val isHidden: Boolean,
    val slot: Int
)
