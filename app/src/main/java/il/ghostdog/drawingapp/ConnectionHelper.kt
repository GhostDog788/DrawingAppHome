package il.ghostdog.drawingapp


import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.withContext
import java.util.*

class ConnectionHelper {
    companion object {
        fun disconnectPlayerFromLobby(lobbyId: String, playerId: String) {
            val databaseInstance = FirebaseDatabase.getInstance()
            val lobbyDB = databaseInstance.getReference("lobbies").child(lobbyId)
            disconnectPlayerFromLobby(lobbyDB, playerId)
        }
        fun disconnectPlayerFromLobby(databaseMyLobby: DatabaseReference, playerId: String) {
            databaseMyLobby.child("playersStatus").child(playerId).removeValue()
            databaseMyLobby.child("players").child(playerId).removeValue().addOnSuccessListener {
                databaseMyLobby.child("players")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.childrenCount == 0L) {
                                //all players left
                                databaseMyLobby.removeValue()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })
            }
        }
        suspend fun getCurrentTimeFromFirebase() : Date {
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
    }
}