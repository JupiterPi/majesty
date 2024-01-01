package jupiterpi.majesty

interface PlayerHandler {
    suspend fun refreshGameState()

    suspend fun requestCardFromQueue(): Place

    suspend fun requestHealedCardPlace(card: Card): Place

    suspend fun requestBuySellMeeples(): Int

    data class Results(val winner: List<Player>)
    suspend fun displayResults(results: Results)
}