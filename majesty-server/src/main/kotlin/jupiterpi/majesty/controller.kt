package jupiterpi.majesty

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.util.*

class Lobby(val gameId: String) {
    val players = mutableListOf<Player>()
}

val lobbies = mutableListOf<Lobby>()
val games = mutableMapOf<String, Game>()

fun Application.configureController() {
    routing {
        route("game/{id}") {
            post("join") {
                @Serializable
                data class JoinGameDTO(val name: String)
                val dto = call.receive<JoinGameDTO>()
                val gameId: String = call.parameters["id"]!!

                val lobby = lobbies.singleOrNull { it.gameId == gameId } ?: Lobby(gameId).also { lobbies += it }
                lobby.players += Player(dto.name)

                call.respondText("Joined player", status = HttpStatusCode.OK)
            }
            post("start") {
                val gameId: String = call.parameters["id"]!!
                val lobby = lobbies.single { it.gameId == gameId }
                lobbies -= lobby
                val game = Game(lobby.players)
                game.players.forEach { it.game = game }
                games[gameId] = game

                call.respondText("Started game", status = HttpStatusCode.OK)

                launch { game.run() }
            }

            webSocket("game/{player}") {
                val player = (games[call.parameters["id"]!!]?.players ?: lobbies.single { it.gameId == call.parameters["id"]!! }.players)
                    .single { it.name == call.parameters["player"]!! }

                val handler = PlayerSocketHandler(player, this)
                player.handler = handler

                if (player.gameHasStarted) handler.refreshGameState()
                handler.runResponseLoop()
            }
        }
    }
}

class PlayerSocketHandler(
    private val player: Player,
    private val session: DefaultWebSocketServerSession,
) : PlayerHandler {
    override suspend fun refreshGameState() = sendPacket("game", GameDTO(player.game))

    override suspend fun requestCardFromQueue(): PlayerHandler.CardChoice {
        @Serializable data class CardChoiceDTO(val cardIndex: Int, val place: Place)
        val response = request<CardChoiceDTO>("card_from_queue")
        return PlayerHandler.CardChoice(player.game.cardsQueue[response.cardIndex], response.place)
    }
    override suspend fun requestHealedCardPlace(card: Card): Place = request("healed_card_place", CardDTO(card))

    override suspend fun requestBuySellMeeples(): Int {
        @Serializable data class BuySellMeeplesDTO(val buySellMeeples: Int)
        return request<BuySellMeeplesDTO>("buy_sell_meeples").buySellMeeples
    }

    override suspend fun displayResults(results: PlayerHandler.Results) {
        @Serializable data class ResultsDTO(val winner: List<String>)
        sendPacket("results", ResultsDTO(results.winner.map { it.name }))
    }

    /* --- */

    private val responses = mutableMapOf<String, String>()

    suspend fun runResponseLoop() {
        @Serializable data class SocketResponse(val requestId: String, val payload: String)
        while (true) {
            val response = session.receiveDeserialized<SocketResponse>()
            responses[response.requestId] = response.payload
        }
    }

    private suspend inline fun <reified R> request(type: String) = request<String, R>(type, null)
    private suspend inline fun <reified T, reified R> request(type: String, payload: T? = null): R {
        val response = requestRaw(type, if (payload == null) null else serialization.encodeToString(payload))
        return if (R::class == String::class) response as R
        else serialization.decodeFromString(response)
    }
    private suspend fun requestRaw(type: String, payload: String? = null): String {
        val requestId = UUID.randomUUID().toString()
        @Serializable data class SocketRequest(val requestId: String, val type: String, val payload: String?)
        sendPacket("request", SocketRequest(requestId, type, payload))

        while (true) {
            delay(100)
            val response = responses[requestId]
            if (response != null) return response
        }
    }

    @Serializable private data class Packet<T>(val topic: String, val payload: T)
    private suspend inline fun <reified T> sendPacket(topic: String, payload: T) = session.sendSerialized(Packet(topic, payload))
}