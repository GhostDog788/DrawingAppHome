package il.ghostdog.drawingapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Job
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


interface ILobbyUser {
    var pingTimerJob: Job?
    var checkPingTimerJob: Job?
    var mPingInterval: Int
    var mCheckPingInterval: Int

    fun startPingTimer() : Job
    fun startPingCheckTimer() : Job

    fun updateMyStatus(databaseMyLobby: DatabaseReference, partyLeader: String){
        databaseMyLobby.child("playersStatus").child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(1)

        if(FirebaseAuth.getInstance().currentUser!!.uid == partyLeader){
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val current = LocalDateTime.now().format(formatter)
            databaseMyLobby.child("playersStatus").child(FirebaseAuth.getInstance().currentUser!!.uid)
                .setValue(current)
        }else{
            databaseMyLobby.child("playersStatus").child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(1)
        }
    }
    fun checkPlayersStatus(databaseMyLobby: DatabaseReference, partyLeader: String){
        if(FirebaseAuth.getInstance().currentUser!!.uid != partyLeader) return
        databaseMyLobby.child("playersStatus").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (player in snapshot.children){
                    if(player.key != partyLeader){
                        if(player.value == 0L){
                            kickPlayer(databaseMyLobby, player.key!!)
                        }else if(player.value == 1L){
                            databaseMyLobby.child("playersStatus").child(player.key!!)
                                .setValue(0)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
    fun checkLeaderStatus(databaseMyLobby: DatabaseReference, partyLeader: String){
        if(FirebaseAuth.getInstance().currentUser!!.uid == partyLeader) return
        databaseMyLobby.child("playersStatus").child(partyLeader).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val timeString = snapshot.getValue(String::class.java)!!
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val dateTimeLastLeader = LocalDateTime.parse(timeString, formatter)
                val dateTimeCurrent = LocalDateTime.now()
                val difference = (dateTimeCurrent.toEpochSecond(ZoneOffset.UTC) - dateTimeLastLeader.toEpochSecond(ZoneOffset.UTC))
                if(difference > mCheckPingInterval){
                    kickLeader(databaseMyLobby, partyLeader)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun kickLeader(databaseMyLobby: DatabaseReference, partyLeader: String) {
        databaseMyLobby.child("players").child(partyLeader).removeValue()
        databaseMyLobby.child("playersStatus").child(partyLeader).removeValue()
    }

    fun onLeaderDisconnected()

    fun kickPlayer(databaseMyLobby: DatabaseReference, playerId: String) {
        databaseMyLobby.child("players").child(playerId ).removeValue()
        databaseMyLobby.child("playersStatus").child(playerId ).removeValue()
    }
}