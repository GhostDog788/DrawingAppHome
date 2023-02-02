package il.ghostdog.drawingapp

import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.Job


interface ILobbyUser {
    var pingTimerJob: Job?
    var leaderCheckPingTimerJob: Job?
    var mPingInterval: Int
    var mLeaderCheckPingInterval: Int

    fun startPingTimer() : Job
    fun startLeaderPingCheckTimer() : Job

    fun updateMyStatus()
    fun checkPlayersStatus()
    fun onLeaderDisconnected()
}