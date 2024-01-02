package jupiterpi.majesty

import jupiterpi.majesty.Game.CardInQueue
import kotlin.math.max
import kotlin.math.pow

class Game(
    val players: List<Player>,
    val bSide: Boolean = false,
) {
    val cardsStack = mutableListOf<Card>()

    class CardInQueue(val card: Card, var meeples: Int)
    val cardsQueue = mutableListOf<CardInQueue>()

    init {
        val tier1Cards = when (players.size) {
            2 -> 6; 3 -> 14; 4 -> 26
            else -> 10/*throw Exception("Must be 2, 3, or 4 players!")*/
        }
        cardsStack.addAll(Card.tier1Cards.take(tier1Cards).shuffled())
        cardsStack.addAll(Card.tier2Cards.shuffled())

        repeat(6) { cardsQueue += CardInQueue(cardsStack.removeFirst(), 0) }
    }

    suspend fun run() {
        players.forEach { it.handler.refreshGameState() }
        repeat(12) {
            players.forEach {
                it.runTurn()
                players.forEach { it.handler.refreshGameState() }
            }
        }

        val infirmaryDeduction = players.associateWith { player -> ((if (bSide) -2 else -1) * player.infirmary.size).also { player.score += it } }
        val varietyScore = players.associateWith { player -> (player.cards.count { it.value.isNotEmpty() }.toDouble().pow(2).toInt()).also { player.score += it } }
        val maxScores = Place.entries.associateWith { place ->
            val maxCards = players.maxOf { it.cards[place]!!.size }
            players.filter { it.cards[place]!!.size == maxCards }.onEach { it.score += place.getMaximumCardsBonus(this) }
        }
        players.forEach { player ->
            val finalScoringMaxCards = maxScores.filterValues { it.contains(player) }.map { FinalScoringMaxCardDTO(it.key, it.key.getMaximumCardsBonus(this)) }
            sendNotification(player, FinalScoringNotification(varietyScore[player]!!, finalScoringMaxCards, infirmaryDeduction[player]!!))
        }

        val maxScore = players.maxOf { it.score }
        val results = SocketHandler.Results(winner = players.filter { it.score == maxScore })
        players.forEach { it.handler.displayResults(results) }
    }

    suspend inline fun <reified T> sendNotification(player: Player, notification: T) = players.forEach { it.handler.sendNotification(player, notification) }
}

class Player(val name: String) {
    lateinit var handler: SocketHandler
    lateinit var game: Game
    val gameHasStarted get() = ::game.isInitialized

    val cards = mapOf(*Place.entries.map { it to mutableListOf<Card>() }.toTypedArray())

    val infirmary = mutableListOf<Card>()
    fun injureCard() = cards.entries.sortedBy { it.key }.firstOrNull { it.value.isNotEmpty() }?.also { it.value.removeFirst().also { infirmary.add(0, it) } }?.key

    var score = 0
    var meeples = 5

    suspend fun runTurn() {
        val choicePlace = handler.requestCardFromQueue()
        val choiceCard = game.cardsQueue.first { it.card.places.contains(choicePlace) }

        val meeplesSpent = game.cardsQueue.indexOf(choiceCard)
        meeples -= meeplesSpent
        game.cardsQueue.take(meeplesSpent).forEach { it.meeples++ }

        game.cardsQueue -= choiceCard
        game.cardsQueue += CardInQueue(game.cardsStack.removeFirst(), 0)

        game.sendNotification(this, CardTakenNotification(CardDTO(choiceCard.card), meeplesSpent, choiceCard.meeples))

        cards[choicePlace]!! += choiceCard.card
        meeples += choiceCard.meeples
        choicePlace.applyEffect(this)

        val excessMeeples = max(0, meeples - 5)
        meeples -= excessMeeples
        score += 1 * excessMeeples
        if (excessMeeples > 0) game.sendNotification(this, MeeplesSoldNotification(excessMeeples))
    }
}

enum class Place(
    private val aSideEffect: suspend (player: Player) -> Unit,
    private val bSideEffect: suspend (player: Player) -> Unit,
    private val bSideMaxCardsBonus: Int,
) {
    //TODO bSide notifications
    MILL({ player ->
        val score = 2 * player.cards[MILL]!!.size
        player.score += score
        player.game.sendNotification(player, MillPlayedNotification(score))
    }, { player ->
        player.score += 2 * player.cards[MILL]!!.size
        player.game.players.filter { it.cards[COTTAGE]!!.size >= 1 }.forEach { it.score += 3 }
    }, 14),
    BREWERY({ player ->
        val score = 2 * player.cards[BREWERY]!!.size
        val meeples = 1 * player.cards[BREWERY]!!.size
        val benefitedPlayers = player.game.players.filter { it.cards[MILL]!!.size >= 1 }
        player.score += score; player.meeples += meeples; benefitedPlayers.forEach { it.score += 2 }
        player.game.sendNotification(player, BreweryPlayedNotification(score, meeples, benefitedPlayers.map { it.name }))
    }, { player ->
        player.meeples += 1 * (player.cards[MILL]!!.size + player.cards[BREWERY]!!.size)
        player.game.players.filter { it.cards[INN]!!.size >= 1 && it.cards[CASTLE]!!.size >= 1 }.forEach { it.score += 10 }
    }, 12),
    COTTAGE({ player ->
        val healedCard = player.infirmary.removeFirstOrNull()
        val healedCardPlace = healedCard?.let { player.handler.requestHealedCardPlace(it) }
        if (healedCard != null) player.cards[healedCardPlace]!! += healedCard
        val score = 2 * (player.cards[MILL]!!.size + player.cards[BREWERY]!!.size + player.cards[COTTAGE]!!.size)
        player.score += score
        player.game.sendNotification(player, CottagePlayedNotification(healedCardPlace, score))
    }, { player ->
        player.score += 3 * player.cards[COTTAGE]!!.size
    }, 12),
    GUARDHOUSE({ player ->
        val score = 2 * (player.cards[GUARDHOUSE]!!.size + player.cards[BARRACKS]!!.size + player.cards[INN]!!.size)
        player.score += score
        player.game.sendNotification(player, GuardhousePlayedNotification(player.cards[GUARDHOUSE]!!.size, score))
    }, { player ->
        player.score += 2 * (player.cards[BREWERY]!!.size + player.cards[COTTAGE]!!.size + player.cards[GUARDHOUSE]!!.size)
        player.game.players.filter { it.cards[INN]!!.size >= 1 }.forEach { it.score += 3 }
    }, 8),
    BARRACKS({ player ->
        val attackStrength = player.cards[BARRACKS]!!.size
        val attackedPlayers = player.game.players.filter { it != player }.map {
            if (it.cards[GUARDHOUSE]!!.size < attackStrength) {
                val injuredCardPlace = it.injureCard()
                return@map if (injuredCardPlace == null) null else AttackedPlayerDTO(it.name, injuredCardPlace)
            } else return@map null
        }.filterNotNull()
        val score = 3 * player.cards[BARRACKS]!!.size
        player.score += score
        player.game.sendNotification(player, BarracksPlayedNotification(attackStrength, attackedPlayers, score))
    }, { player ->
        player.game.players.filter { it != player }.forEach {
            if (it.cards[GUARDHOUSE]!!.size < player.cards[BARRACKS]!!.size) it.injureCard()
        }
        player.score += 3 * (player.cards[BARRACKS]!!.size + player.cards[INN]!!.size + player.cards[CASTLE]!!.size)
    }, 8),
    INN({ player ->
        val score = 4 * player.cards[INN]!!.size
        val benefitedPlayers = player.game.players.filter { it.cards[BREWERY]!!.size >= 1 }
        player.score += score
        benefitedPlayers.forEach { it.score += 3 }
        player.game.sendNotification(player, InnPlayedNotification(score, benefitedPlayers.map { it.name }))
    }, { player ->
        player.score += player.cards[INN]!!.size * 2 * player.cards.maxOf { it.value.size }
    }, 12),
    CASTLE({ player ->
        val score = 5 * player.cards[CASTLE]!!.size
        val meeples = 1 * player.cards[CASTLE]!!.size
        player.score += score
        player.meeples += meeples
        player.game.sendNotification(player, CastlePlayedNotification(score, meeples))
    }, { player ->
        val buySellMeeples = player.handler.requestBuySellMeeples()
        player.meeples += buySellMeeples
        player.score -= buySellMeeples
        player.score += 4 * (player.cards[CASTLE]!!.size + player.infirmary.size)
    }, 16);

    suspend fun applyEffect(player: Player) {
        if (!player.game.bSide) aSideEffect(player)
        if (player.game.bSide) bSideEffect(player)
    }

    fun getMaximumCardsBonus(game: Game) = if (game.bSide) bSideMaxCardsBonus else 10 + Place.entries.indexOf(this)
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