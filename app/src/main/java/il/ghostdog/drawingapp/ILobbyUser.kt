package il.ghostdog.drawingapp

import android.content.SharedPreferences
import android.icu.util.Calendar
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.NtpUtils
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


interface ILobbyUser {
    var pingTimerJob: Job?
    var checkPingTimerJob: Job?
    var mPingInterval: Int
    var mCheckPingInterval: Int
    var partyLeader: String?
    var databaseMyLobby: DatabaseReference?
    var sharedPref: SharedPreferences?

    fun startPingTimer(scope: LifecycleCoroutineScope): Job {
        pingTimerJob?.cancel()
        return scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay((mPingInterval * 1000).toLong())
                updateMyStatus()
            }
        }
    }
    fun startPingCheckTimer(scope: LifecycleCoroutineScope, playerId: String): Job {
        checkPingTimerJob?.cancel()
        return scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay((mCheckPingInterval * 1000).toLong())
                if (playerId == partyLeader) {
                    checkPlayersStatus()
                } else {
                    checkLeaderStatus()
                }
            }
        }
    }

    suspend fun updateMyStatus(){
        //need to take the time from the internet not the device
        if(FirebaseAuth.getInstance().currentUser!!.uid == partyLeader){
            val date: Date = getCurrentDateFromNtp()
            val israelZone: ZoneId = ZoneId.of("Asia/Jerusalem")
            val localDateTime = LocalDateTime.ofInstant(date.toInstant(), israelZone)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val current = localDateTime.format(formatter)
            databaseMyLobby!!.child("playersStatus").child(FirebaseAuth.getInstance().currentUser!!.uid)
                .setValue(current)
        }else{
            databaseMyLobby!!.child("playersStatus").child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(1)
        }
    }
    fun checkPlayersStatus(){
        if(FirebaseAuth.getInstance().currentUser!!.uid != partyLeader) return
        databaseMyLobby!!.child("playersStatus").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (player in snapshot.children){
                    if(player.key != partyLeader){
                        if(player.value == 0L){
                            ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, player.key!!)
                        }else if(player.value == 1L){
                            databaseMyLobby!!.child("playersStatus").child(player.key!!)
                                .setValue(0)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    fun checkLeaderStatus(){
        if(FirebaseAuth.getInstance().currentUser!!.uid == partyLeader) return
        databaseMyLobby!!.child("playersStatus").child(partyLeader!!).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val timeString = snapshot.getValue(String::class.java) ?: return
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val dateTimeLastLeader = LocalDateTime.parse(timeString, formatter)
                GlobalScope.launch(Dispatchers.Default){
                    val date = getCurrentDateFromNtp()
                    val israelZone: ZoneId = ZoneId.of("Asia/Jerusalem")
                    val localDateTime = LocalDateTime.ofInstant(date.toInstant(), israelZone)
                    val difference = (localDateTime.toEpochSecond(ZoneOffset.UTC) - dateTimeLastLeader.toEpochSecond(ZoneOffset.UTC))
                    println("Time difference $difference")
                    if(difference > mCheckPingInterval){
                        ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, partyLeader!!)
                        onLeaderDisconnected()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun onLeaderDisconnected()

    suspend fun getCurrentDateFromNtp(): Date {
        var date: Date? = null

        while (date == null) {
            try {
                val client = NTPUDPClient()
                client.defaultTimeout = 10000 // set the timeout to 10 seconds
                val address = InetAddress.getByName("pool.ntp.org")
                val info: TimeInfo = client.getTime(address)
                val time = info.message.receiveTimeStamp.time
                date = Date(time)
            } catch (e: Exception) {
                e.printStackTrace()
                delay(500) // wait 0.5 second before retrying
            }
        }

        return date
    }
}