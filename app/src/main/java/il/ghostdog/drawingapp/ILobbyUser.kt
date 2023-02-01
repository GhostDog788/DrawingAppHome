package il.ghostdog.drawingapp

import com.google.firebase.database.DatabaseReference


interface ILobbyUser {
    fun updateMyStatus()
    fun checkPlayersStatus()
    fun onLeaderDisconnected()
}