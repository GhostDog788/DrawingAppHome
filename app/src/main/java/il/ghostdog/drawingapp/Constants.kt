package il.ghostdog.drawingapp

object Constants {
    const val defaultWidth = 1058
    var viewWidth = 0
    val SHARED_LOBBIES_NAME = "LobbySettings"
    val REQUESTING_PLAYER_NODE_NAME = "requesting"
    val PING_INTERVAL = 5
    val PING_INTERVAL_CHECK = 20
    val DRAWING_UPDATE_INTERVAL = 4

    var GUESS_WORDS_MAP = LinkedHashMap<String, ArrayList<String>>()
}