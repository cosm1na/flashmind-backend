package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class AuthRequest(
    val email: String,
    val parola: String
)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        post("/register") {
            val userDinAndroid = call.receive<AuthRequest>()

            transaction {
                TabelUseri.insert {
                    it[email] = userDinAndroid.email
                    it[parola] = userDinAndroid.parola
                }
            }
            call.respondText("Cont creat cu succes si salvat în baza de date!")
        }

        post("/login") {
            val userDinAndroid = call.receive<AuthRequest>()

            val userGasit = transaction<ResultRow?> {
                TabelUseri.select { TabelUseri.email eq userDinAndroid.email }.singleOrNull()
            }

            if (userGasit == null) {
                call.respond(HttpStatusCode.Unauthorized, LoginResponse(false, "Eroare: Contul nu exista!"))
            } else {
                val parolaReala = userGasit[TabelUseri.parola]
                if (parolaReala == userDinAndroid.parola) {
                    val idUtilizator = userGasit[TabelUseri.id]
                    val userNume = userGasit[TabelUseri.username]
                    val userPoza = userGasit[TabelUseri.pozaProfil]

                    call.respond(
                        HttpStatusCode.OK,
                        LoginResponse(true, "Te-ai logat cu succes!", idUtilizator, userNume, userPoza)
                    )
                } else {
                    call.respond(HttpStatusCode.Unauthorized, LoginResponse(false, "Eroare: Parola este gresita!"))
                }
            }
        }

        put("/users/{id}") {
            val idUser = call.parameters["id"]?.toIntOrNull()
            if (idUser == null) {
                call.respond(HttpStatusCode.BadRequest, "ID utilizator invalid!")
                return@put
            }

            val profilNou = call.receive<UpdateProfileRequest>()

            transaction {
                TabelUseri.update({ TabelUseri.id eq idUser }) {
                    it[username] = profilNou.username
                    it[pozaProfil] = profilNou.pozaProfil
                }
            }
            call.respond(HttpStatusCode.OK, "Profil actualizat cu succes!")
        }

        post("/decks") {
            val deckPrimit = call.receive<Deck>()

            val idNouGenerat = transaction {
                TabelDecks.insert {
                    it[titlu] = deckPrimit.titlu
                    it[userId] = deckPrimit.userId
                } get TabelDecks.id
            }
            val deckSalvat = Deck(idNouGenerat, deckPrimit.userId, deckPrimit.titlu)
            call.respond(HttpStatusCode.Created, deckSalvat)
        }

        get("/decks/{userId}") {
            val idUser = call.parameters["userId"]?.toIntOrNull()
            if (idUser == null) {
                call.respond(HttpStatusCode.BadRequest, "ID-ul utilizatorului lipseste sau e invalid!")
                return@get
            }

            val listaDecks = transaction<List<Deck>> {
                TabelDecks.select { TabelDecks.userId eq idUser }.map { rand: ResultRow ->
                    Deck(
                        id = rand[TabelDecks.id],
                        userId = rand[TabelDecks.userId],
                        titlu = rand[TabelDecks.titlu]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, listaDecks)
        }

        post("/cards") {
            val cardPrimit = call.receive<Card>()

            val idNouGenerat = transaction {
                TabelCards.insert {
                    it[fata] = cardPrimit.fata
                    it[spate] = cardPrimit.spate
                    it[deckId] = cardPrimit.deckId
                    it[cutie] = cardPrimit.cutie
                    it[dataViitoare] = cardPrimit.dataViitoare
                } get TabelCards.id
            }
            val cardSalvat = Card(
                idNouGenerat,
                cardPrimit.deckId,
                cardPrimit.fata,
                cardPrimit.spate,
                cardPrimit.cutie,
                cardPrimit.dataViitoare
            )
            call.respond(HttpStatusCode.Created, cardSalvat)
        }

        get("/cards/{deckId}") {
            val idDeck = call.parameters["deckId"]?.toIntOrNull()
            if (idDeck == null) {
                call.respond(HttpStatusCode.BadRequest, "ID-ul pachetului lipsește sau e invalid!")
                return@get
            }

            val listaCards = transaction<List<Card>> {
                TabelCards.select { TabelCards.deckId eq idDeck }.map { rand: ResultRow ->
                    Card(
                        id = rand[TabelCards.id],
                        deckId = rand[TabelCards.deckId],
                        fata = rand[TabelCards.fata],
                        spate = rand[TabelCards.spate],
                        cutie = rand[TabelCards.cutie],
                        dataViitoare = rand[TabelCards.dataViitoare]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, listaCards)
        }

        delete("/decks/{id}") {
            val idDeck = call.parameters["id"]?.toIntOrNull()
            if (idDeck == null) {
                call.respond(HttpStatusCode.BadRequest, "ID invalid!")
                return@delete
            }

            transaction {
                TabelCards.deleteWhere { TabelCards.deckId eq idDeck }
                TabelDecks.deleteWhere { TabelDecks.id eq idDeck }
            }
            call.respond(HttpStatusCode.OK, "Deck sters cu succes!")
        }

        put("/decks/{id}") {
            val idDeck = call.parameters["id"]?.toIntOrNull()
            if (idDeck == null) {
                call.respond(HttpStatusCode.BadRequest, "ID invalid!")
                return@put
            }
            val deckPrimit = call.receive<Deck>()
            transaction {
                TabelDecks.update({ TabelDecks.id eq idDeck }) {
                    it[titlu] = deckPrimit.titlu
                }
            }
            call.respond(HttpStatusCode.OK, "Deck actualizat!")
        }

        delete("/cards/{id}") {
            val idCard = call.parameters["id"]?.toIntOrNull()
            if (idCard == null) {
                call.respond(HttpStatusCode.BadRequest, "ID invalid!")
                return@delete
            }

            transaction {
                TabelCards.deleteWhere { TabelCards.id eq idCard }
            }
            call.respond(HttpStatusCode.OK, "Cartonaș șters cu succes!")
        }

        put("/cards/{id}") {
            val idCard = call.parameters["id"]?.toIntOrNull()
            if (idCard == null) {
                call.respond(HttpStatusCode.BadRequest, "ID invalid!")
                return@put
            }

            val cardActualizat = call.receive<Card>()

            transaction {
                TabelCards.update({ TabelCards.id eq idCard }) {
                    it[fata] = cardActualizat.fata
                    it[spate] = cardActualizat.spate
                    it[cutie] = cardActualizat.cutie
                    it[dataViitoare] = cardActualizat.dataViitoare
                }
            }
            call.respond(HttpStatusCode.OK, "Cartonaș actualizat cu succes!")
        }
    }
}