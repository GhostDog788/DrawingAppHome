package il.ghostdog.drawingapp

import android.content.Intent

object Constants {
    const val defaultWidth = 1058
    var viewWidth = 0
    val SHARED_LOBBIES_NAME = "LobbySettings"
    val REQUESTING_PLAYER_NODE_NAME = "requesting"
    val PING_INTERVAL = 5
    val PING_INTERVAL_CHECK = 20
    val DRAWING_UPDATE_INTERVAL = 4
    var myUserName: String = "Error"//default value need to be set

    const val BASE_URL = "https://fcm.googleapis.com"
    const val SERVER_KEY = "AAAAAhHc4bA:APA91bEEywfu_Zv9Xw8nsnfb3Sye1TwrEF2XzDFOjwUXoTRPXcqjgZI2MUyIfy4cTEwyg8HQzJmZPxqWmlppuWqVWjZzBH_kpe5QJYyJcMFj-h-mF17RKX07y3Om4h-BDa4gV3lVj_8q"
    const val CONTENT_TYPE = "application/json"

    var GUESS_WORDS_MAP = LinkedHashMap<String, ArrayList<String>>()
    var lastSeenServiceIntent: Intent? = null
}