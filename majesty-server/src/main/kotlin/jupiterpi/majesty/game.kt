package jupiterpi.majesty

import jupiterpi.majesty.Game.CardInQueue
import kotlin.math.pow

class Game(
    val players: List<Player>,
    val bSide: Boolean = false,
) {
    val cardsStack = mutableListOf<Card>()
    init {
        val tier1Cards = when (players.size) {
            2 -> 6; 3 -> 14; 4 -> 26
            else -> throw Exception("Must be 2, 3, or 4 players!")
        }
        cardsStack.addAll(Card.tier1Cards.take(tier1Cards).shuffled())
        cardsStack.addAll(Card.tier2Cards.shuffled())

        repeat(6) { cardsQueue += CardInQueue(cardsStack.removeFirst(), 0) }
    }

    class CardInQueue(val card: Card, var meeples: Int)
    val cardsQueue = mutableListOf<CardInQueue>()

    suspend fun run() {
        repeat(12) {
            players.forEach {
                it.runTurn()
                players.forEach { it.handler.refreshGameState() }
            }
        }

        players.forEach { player ->
            player.score -= 1 * player.infirmary.size
            player.score += player.cards.count { it.value.isNotEmpty() }.toDouble().pow(2).toInt()
        }
        Place.entries.forEach { place ->
            val maxCards = players.maxOf { it.cards[place]!!.size }
            players.filter { it.cards[place]!!.size == maxCards }.forEach { it.score += place.maximumCardsBonus }
        }

        val maxScore = players.maxOf { it.score }
        val results = PlayerHandler.Results(winner = players.filter { it.score == maxScore })
        players.forEach { it.handler.displayResults(results) }
    }
}

class Player(
    val name: String,
    val color: Color,
    val handler: PlayerHandler,
) {
    lateinit var game: Game

    val cards = mapOf(*Place.entries.map { it to mutableListOf<Card>() }.toTypedArray())

    val infirmary = mutableListOf<Card>()
    fun injureCard() = cards.entries.sortedBy { it.key }.firstOrNull { it.value.isNotEmpty() }?.value?.removeFirst()
        ?.let { infirmary.add(0, it) }

    var score = 0
    var meeples = 5

    suspend fun runTurn() {
        val choice = handler.requestCardFromQueue()

        val meeplesSpent = game.cardsQueue.indexOf(choice.card)
        meeples -= meeplesSpent
        game.cardsQueue.take(meeplesSpent).forEach { it.meeples++ }

        game.cardsQueue -= choice.card
        game.cardsQueue += CardInQueue(game.cardsStack.removeFirst(), 0)

        cards[choice.place]!! += choice.card.card
        meeples += choice.card.meeples
        choice.place.applyEffect(this)

        val excessMeeples = meeples - 5
        meeples -= excessMeeples
        score += 1 * excessMeeples
    }
}

enum class Color {
    RED, GREEN, BLUE, YELLOW
}

enum class Place(
    private val aSideEffect: suspend (player: Player) -> Unit,
) {
    MILL({ player ->
        player.score += 1 * player.cards[MILL]!!.size
    }),
    BREWERY({ player ->
        player.score += 2 * player.cards[BREWERY]!!.size
        player.meeples += 1 * player.cards[BREWERY]!!.size
        player.game.players.filter { it.cards[MILL]!!.size >= 1 }.forEach { it.score += 2 }
    }),
    COTTAGE({ player ->
        player.infirmary.removeFirstOrNull()?.let { player.cards[player.handler.requestHealedCardPlace(it)]!! += it }
        player.score += 2 * (player.cards[MILL]!!.size + player.cards[BREWERY]!!.size + player.cards[COTTAGE]!!.size)
    }),
    GUARDHOUSE({ player ->
        player.score += 2 * (player.cards[GUARDHOUSE]!!.size + player.cards[BARRACKS]!!.size + player.cards[INN]!!.size)
    }),
    BARRACKS({ player ->
        player.game.players.filter { it != player }.forEach {
            if (it.cards[GUARDHOUSE]!!.size < player.cards[BARRACKS]!!.size + 1) it.injureCard()
        }
        player.score += 3 * player.cards[BARRACKS]!!.size
    }),
    INN({ player ->
        player.score += 4 * player.cards[INN]!!.size
        player.game.players.filter { it.cards[BREWERY]!!.size >= 1 }.forEach { it.score += 3 }
    }),
    CASTLE({ player ->
        player.score += 5 * player.cards[CASTLE]!!.size
        player.meeples += 5 * player.cards[CASTLE]!!.size
    });

    suspend fun applyEffect(player: Player) {
        if (!player.game.bSide) aSideEffect(player)
        if (player.game.bSide) { /*TODO implement b side effects */ }
    }

    val maximumCardsBonus get() = 10 + Place.entries.indexOf(this)
}

class Card(
    val places: List<Place>,
) {
    companion object {
        val tier1Cards = listOf(
            listOf(Place.MILL) to 7,
            listOf(Place.BREWERY) to 4,
            listOf(Place.COTTAGE) to 3,
            listOf(Place.GUARDHOUSE) to 3,
            listOf(Place.BARRACKS) to 2,
            listOf(Place.INN) to 2,
            listOf(Place.CASTLE) to 3,
            listOf(Place.MILL, Place.BREWERY) to 2,
            listOf(Place.BREWERY, Place.COTTAGE) to 1,
            listOf(Place.COTTAGE, Place.GUARDHOUSE) to 1,
            listOf(Place.BARRACKS, Place.INN) to 1,
            listOf(Place.INN, Place.CASTLE) to 1,
            listOf(Place.MILL, Place.BARRACKS) to 1,
            listOf(Place.GUARDHOUSE, Place.INN) to 1,
            listOf(Place.GUARDHOUSE, Place.CASTLE) to 1,
        ).map { entry -> MutableList(entry.second) { Card(entry.first) } }.flatten()
        val tier2Cards = listOf(
            listOf(Place.MILL) to 2,
            listOf(Place.BREWERY) to 2,
            listOf(Place.COTTAGE) to 2,
            listOf(Place.GUARDHOUSE) to 2,
            listOf(Place.BARRACKS) to 1,
            listOf(Place.INN) to 2,
            listOf(Place.CASTLE) to 2,
            listOf(Place.MILL, Place.BREWERY) to 1,
            listOf(Place.BREWERY, Place.COTTAGE) to 2,
            listOf(Place.COTTAGE, Place.GUARDHOUSE) to 2,
            listOf(Place.GUARDHOUSE, Place.BARRACKS) to 2,
            listOf(Place.BARRACKS, Place.INN) to 2,
            listOf(Place.INN, Place.CASTLE) to 1,
            listOf(Place.MILL, Place.BARRACKS) to 1,
            listOf(Place.BREWERY, Place.BARRACKS) to 1,
            listOf(Place.COTTAGE, Place.INN) to 1,
            listOf(Place.COTTAGE, Place.CASTLE) to 1,
        ).map { entry -> MutableList(entry.second) { Card(entry.first) } }.flatten()
    }
}