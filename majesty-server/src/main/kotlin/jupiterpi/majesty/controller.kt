package jupiterpi.majesty

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.reflect.*
import io.ktor.websocket.*
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

fun createGameId(): String {
    val id = (('A'..'Z') + ('a'..'z') + ('0'..'9')).let { chars -> List(8) { chars.random() }.joinToString("") }
    return if (!games.keys.contains(id)) id else createGameId()
}

fun Application.configureController() {
    routing {
        route("api") {
            get("newGame") {
                @Serializable data class NewGameDTO(val id: String)
                call.respond(NewGameDTO(createGameId()))
            }
            route("game/{id}") {
                post("join") {
                    @Serializable data class JoinGameDTO(val name: String)
                    val dto = call.receive<JoinGameDTO>()
                    val gameId: String = call.parameters["id"]!!

                    val lobby = lobbies.singleOrNull { it.gameId == gameId } ?: Lobby(gameId).also { lobbies += it }
                    lobby.players += Player(dto.name)

                    call.respondText("Joined player", status = HttpStatusCode.OK)
                }
                post("start") {
                    val gameId: String = call.parameters["id"]!!
                    val lobby = lobbies.singleOrNull { it.gameId == gameId } ?: return@post call.respondText("Lobby not found!", status = HttpStatusCode.NotFound)
                    if (lobby.players.size < 2 || lobby.players.size > 4) return@post call.respondText("Must be 2, 3, or 4 players!", status = HttpStatusCode.Forbidden)
                    lobbies -= lobby
                    val game = Game(lobby.players.shuffled())
                    game.players.forEach { it.game = game }
                    games[gameId] = game

                    call.respondText("Started game", status = HttpStatusCode.OK)

                    launch { game.run() }
                }

                webSocket("game/{player}") {
                    val players = (games[call.parameters["id"]!!]?.players ?: lobbies.singleOrNull { it.gameId == call.parameters["id"]!! }?.players) ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid game id!"))
                    val player = players.singleOrNull { it.name == call.parameters["player"]!! } ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Player not found!"))

                    player.handler.runResponseLoop(this)
                }
            }
        }
    }
}

class SocketHandler(private val player: Player) {
    suspend fun refreshGameState() = sendPacket("game", GameDTO(player.game))

    @Serializable data class NotificationDTO<T>(val type: String, val playerName: String?, val notification: T)
    suspend inline fun <reified T> sendNotification(player: Player?, notification: T) {
        val dto = NotificationDTO(T::class.findAnnotation<Notification>()!!.type, player?.name, notification)
        sendPacket("notification", dto)
        addNotification { sendPacket("notification", dto) }
    }
    fun addNotification(notification: suspend () -> Unit) { notifications += notification }

    @Serializable data class CardChoiceDTO(val cardIndex: Int, val place: Place)
    suspend fun requestCardFromQueue(): CardChoiceDTO {
        return request<CardChoiceDTO>("card_from_queue")
    }
    suspend fun requestHealedCardPlace(card: Card): Place {
        @Serializable data class HealedCardPlaceDTO(val place: Place)
        return request<CardDTO, HealedCardPlaceDTO>("healed_card_place", CardDTO(card)).place
    }
    suspend fun requestBuySellMeeples(): Int {
        @Serializable data class BuySellMeeplesDTO(val buySellMeeples: Int)
        return request<BuySellMeeplesDTO>("buy_sell_meeples").buySellMeeples
    }

    /* --- */

    private var session: DefaultWebSocketServerSession? = null

    private val notifications = mutableListOf<suspend () -> Unit>()

    private val responses = mutableMapOf<String, String>()

    @Serializable data class SocketRequest(val requestId: String, val type: String, val payload: String?)
    private val currentRequests = mutableListOf<SocketRequest>()

    suspend fun runResponseLoop(session: DefaultWebSocketServerSession) {
        this.session = session

        if (player.gameHasStarted) refreshGameState()

        notifications.forEach { it.invoke() }
        currentRequests.forEach { sendPacket("request", it) }

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
            this.session = null
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
        val packet = SocketRequest(requestId, type, payload).also { currentRequests += it }
        sendPacket("request", packet)
        val response: String
        while (true) {
            delay(100)
            response = responses[requestId] ?: continue
            break
        }
        currentRequests -= packet
        return response
    }

    @Serializable data class OutPacket<T>(val topic: String, val payload: T)
    suspend inline fun <reified T> sendPacket(topic: String, payload: T) = sendPacket(OutPacket(topic, payload), typeInfo<OutPacket<T>>())
    suspend fun <T> sendPacket(packet: OutPacket<T>, typeInfo: TypeInfo) = session?.sendSerialized(packet, typeInfo)
}