package com.example

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object TabelUseri : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 128).uniqueIndex()
    val parola = varchar("parola", 128)

    val username = varchar("username", 128).nullable()
    val pozaProfil = text("poza_profil").nullable()

    override val primaryKey = PrimaryKey(id)
}

object TabelDecks : Table("decks") {
    val id = integer("id").autoIncrement()
    val titlu = varchar("titlu", 128)
    val userId = integer("user_id").references(TabelUseri.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(id)
}

object TabelCards : Table("cards") {
    val id = integer("id").autoIncrement()
    val fata = text("fata")
    val spate = text("spate")
    val deckId = integer("deck_id").references(TabelDecks.id, onDelete = ReferenceOption.CASCADE)

    val cutie = integer("cutie").default(1)
    val dataViitoare = long("data_viitoare").default(System.currentTimeMillis())

    override val primaryKey = PrimaryKey(id)
}

fun initDatabase() {
    Database.connect("jdbc:sqlite:flashmind.sqlite", driver = "org.sqlite.JDBC")

    transaction {
        SchemaUtils.create(TabelUseri, TabelDecks, TabelCards)
    }
}