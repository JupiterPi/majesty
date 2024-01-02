package jupiterpi.majesty

import kotlinx.serialization.Serializable

@Serializable
data class GameDTO(
    val players: List<PlayerDTO>,
    val bSide: Boolean,
    val activePlayerName: String?,
    val cardsQueue: List<CardInQueueDTO>,
) {
    constructor(game: Game) : this(game.players.map { PlayerDTO(it) }, game.bSide, game.activePlayer?.name, game.cardsQueue.map { CardInQueueDTO(it) })
}

@Serializable
data class CardInQueueDTO(
    val card: CardDTO,
    val meeples: Int,
) {
    constructor(cardInQueue: Game.CardInQueue) : this(CardDTO(cardInQueue.card), cardInQueue.meeples)
}

@Serializable
data class PlayerDTO(
    val name: String,
    val score: Int,
    val meeples: Int,
    val cards: Map<Place, List<CardDTO>>,
    val infirmary: List<CardDTO>,
) {
    constructor(player: Player) : this(
        player.name, player.score, player.meeples,
        player.cards.mapValues { it.value.map { CardDTO(it) } },
        player.infirmary.map { CardDTO(it) },
    )
}

@Serializable
data class CardDTO(
    val places: List<Place>,
) {
    constructor(card: Card) : this(card.places)
}