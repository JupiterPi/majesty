package jupiterpi.majesty

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.util.*
import kotlin.reflect.full.findAnnotation

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

                val handler = SocketHandler(player, this)
                player.handler = handler

                if (player.gameHasStarted) handler.refreshGameState()
                handler.runResponseLoop()
            }
        }
    }
}

class SocketHandler(
    val player: Player,
    val session: DefaultWebSocketServerSession,
) {
    suspend fun refreshGameState() = sendPacket("game", GameDTO(player.game))

    @Serializable data class NotificationDTO<T>(val type: String, val playerName: String, val notification: T)
    suspend inline fun <reified T> sendNotification(player: Player, notification: T) {
        val dto = NotificationDTO(T::class.findAnnotation<Notification>()!!.type, player.name, notification)
        sendPacket("notifications", dto)
    }

    suspend fun requestCardFromQueue(): Place {
        @Serializable data class CardChoiceDTO(val place: Place)
        return request<CardChoiceDTO>("card_from_queue").place
    }
    suspend fun requestHealedCardPlace(card: Card): Place {
        @Serializable data class HealedCardPlaceDTO(val place: Place)
        return request<CardDTO, HealedCardPlaceDTO>("healed_card_place", CardDTO(card)).place
    }
    suspend fun requestBuySellMeeples(): Int {
        @Serializable data class BuySellMeeplesDTO(val buySellMeeples: Int)
        return request<BuySellMeeplesDTO>("buy_sell_meeples").buySellMeeples
    }

    data class Results(val winner: List<Player>)
    suspend fun displayResults(results: Results) {
        @Serializable data class ResultsDTO(val winner: List<String>)
        sendPacket("results", ResultsDTO(results.winner.map { it.name }))
    }

    /* --- */

    private val responses = mutableMapOf<String, String>()

    suspend fun runResponseLoop() {
        try {
            while (true) {
                @Serializable data class InPacket(val topic: String, val payload: String)
                val packet = session.receiveDeserialized<InPacket>()
                when (packet.topic) {
                    "request" -> {
                        @Serializable data class SocketResponse(val requestId: String, val payload: String)
                        val response = serialization.decodeFromString<SocketResponse>(packet.payload)
                        responses[response.requestId] = response.payload
                    }
                    "chat" -> {
                        @Serializable data class ChatMessageDTO(val message: String)
                        val message = serialization.decodeFromString<ChatMessageDTO>(packet.payload)
                        player.game.players.forEach {
                            it.handler.sendNotification(player, MessageNotification(message.message))
                        }
                    }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            //player.handler = null
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

    @Serializable data class OutPacket<T>(val topic: String, val payload: T)
    suspend inline fun <reified T> sendPacket(topic: String, payload: T) = session.sendSerialized(OutPacket(topic, payload))
}