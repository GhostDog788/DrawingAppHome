package il.ghostdog.drawingapp


data class LobbySearchRViewData(
    val lobbyId: String,
    val leaderName: String,
    val playersCount: Int,
    val status: GameStatus,
    val currentRound: Int,
    val rounds: Int
)
