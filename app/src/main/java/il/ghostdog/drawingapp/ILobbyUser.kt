package il.ghostdog.drawingapp

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.net.SocketException
import java.net.UnknownHostException


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
                Log.i("LobbyMsg", "SendUpdate")
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
            val date: Date = getCurrentTimeFromFirebase()
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
                    val date = getCurrentTimeFromFirebase()
                    val israelZone: ZoneId = ZoneId.of("Asia/Jerusalem")
                    val localDateTime = LocalDateTime.ofInstant(date.toInstant(), israelZone)
                    val difference = (localDateTime.toEpochSecond(ZoneOffset.UTC) - dateTimeLastLeader.toEpochSecond(ZoneOffset.UTC))
                    Log.i("LobbyMsg","Time difference $difference")
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

    suspend fun getCurrentTimeFromFirebase() : Date{
        var date: Date? = null

        val database = FirebaseDatabase.getInstance().reference
        database.child(".info/serverTimeOffset").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val offset = dataSnapshot.getValue(Long::class.java) ?: 0L
                val estimatedServerTimeMs = System.currentTimeMillis() + offset
                val currentTime = Date(estimatedServerTimeMs)
                // use the current time here
                date = currentTime
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        withContext(Dispatchers.IO){
            while(date == null)
            {
                awaitFrame()
            }
        }
        return date!!
    }

    //---past attempts
    suspend fun getCurrentTimeFromInternet(): Date {
        return withContext(Dispatchers.IO) {
            var date: Date? = null
            // List of NTP time servers to try
            val ntpServers = listOf(
                "time.google.com",
                "time.windows.com",
                "pool.ntp.org",
                "us.pool.ntp.org",
                "ca.pool.ntp.org",
                "uk.pool.ntp.org",
                "europe.pool.ntp.org",
                "asia.pool.ntp.org",
                "oceania.pool.ntp.org"
            )
            while (date == null) {
                for (ntpServer in ntpServers) {
                    try {
                        val client = NTPUDPClient()
                        client.open()
                        val address = InetAddress.getByName(ntpServer)
                        val info: TimeInfo = client.getTime(address)
                        info.computeDetails()
                        client.close()
                        date = Date(info.returnTime)
                    } catch (e: UnknownHostException) {
                        // handle error
                        e.printStackTrace()
                    } catch (e: SocketException) {
                        // handle error
                        e.printStackTrace()
                    }
                }
                if (date == null){
                    Log.i("LobbyMsg", "Cant connect to any of the servers")
                    delay(200)
                }
            }
            date
        }
    }
    suspend fun getCurrentDateFromNtp(): Date {
        return withContext(Dispatchers.IO) {
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

            date
        }
    }
    //---
}