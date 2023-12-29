package jupiterpi.majesty

interface PlayerHandler {
    suspend fun refreshGameState()

    data class CardChoice(val card: Game.CardInQueue, val place: Place)
    suspend fun requestCardFromQueue(): CardChoice

    suspend fun requestHealedCardPlace(card: Card): Place

    suspend fun requestBuySellMeeples(): Int

    data class Results(val winner: List<Player>)
    suspend fun displayResults(results: Results)
}