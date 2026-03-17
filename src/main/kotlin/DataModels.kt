package com.example

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val succes: Boolean,
    val mesaj: String,
    val userId: Int? = null
)

@Serializable
data class Deck(
    val id: Int? = null,
    val userId: Int,
    val titlu: String
)

@Serializable
data class Card(
    val id: Int? = null,
    val deckId: Int,
    val fata: String,
    val spate: String
)