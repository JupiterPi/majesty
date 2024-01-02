package jupiterpi.majesty

import kotlinx.serialization.Serializable

annotation class Notification(val type: String)

@Serializable
@Notification("message")
data class MessageNotification(
    val message: String,
)

@Serializable
@Notification("card_taken")
data class CardTakenNotification(
    val card: CardDTO,
    val meeplesPlaced: Int,
    val meeplesGained: Int,
)

@Serializable
@Notification("mill_played")
data class MillPlayedNotification(
    val score: Int,
)

@Serializable
@Notification("brewery_played")
data class BreweryPlayedNotification(
    val score: Int,
    val meeples: Int,
    val benefitedPlayerNames: List<String>,
)

@Serializable
@Notification("cottage_played")
data class CottagePlayedNotification(
    val healed: Place?,
    val score: Int,
)

@Serializable
@Notification("guardhouse_played")
data class GuardhousePlayedNotification(
    val guards: Int,
    val score: Int,
)

@Serializable
@Notification("barracks_played")
data class BarracksPlayedNotification(
    val attackStrength: Int,
    val attackedPlayers: List<AttackedPlayerDTO>,
    val score: Int,
)
@Serializable data class AttackedPlayerDTO(val name: String, val place: Place)

@Serializable
@Notification("inn_played")
data class InnPlayedNotification(
    val score: Int,
    val benefitedPlayerNames: List<String>,
)

@Serializable
@Notification("castle_played")
data class CastlePlayedNotification(
    val score: Int,
    val meeples: Int,
)

@Serializable
@Notification("meeples_sold")
data class MeeplesSoldNotification(
    val amount: Int,
)

@Serializable
@Notification("final_scoring")
data class FinalScoringNotification(
    val varietyScore: Int,
    val maxCards: List<FinalScoringMaxCardDTO>,
    val infirmaryScore: Int,
)
@Serializable data class FinalScoringMaxCardDTO(val place: Place, val score: Int)