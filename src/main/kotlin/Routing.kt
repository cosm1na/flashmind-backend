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
            call.respondText("Cont creat cu succes și salvat în baza de date!")
        }

        post("/login") {
            val userDinAndroid = call.receive<AuthRequest>()

            val userGasit = transaction<ResultRow?> {
                TabelUseri.select { TabelUseri.email eq userDinAndroid.email }.singleOrNull()
            }

            if (userGasit == null) {
                call.respond(HttpStatusCode.Unauthorized, LoginResponse(false, "Eroare: Contul nu există!"))
            } else {
                val parolaReala = userGasit[TabelUseri.parola]
                if (parolaReala == userDinAndroid.parola) {
                    val idUtilizator = userGasit[TabelUseri.id]
                    call.respond(HttpStatusCode.OK, LoginResponse(true, "Te-ai logat cu succes!", idUtilizator))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, LoginResponse(false, "Eroare: Parola este greșită!"))
                }
            }
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
                call.respond(HttpStatusCode.BadRequest, "ID-ul utilizatorului lipsește sau e invalid!")
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
                } get TabelCards.id
            }
            val cardSalvat = Card(idNouGenerat, cardPrimit.deckId, cardPrimit.fata, cardPrimit.spate)
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
                        spate = rand[TabelCards.spate]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, listaCards)
        }
    }
}