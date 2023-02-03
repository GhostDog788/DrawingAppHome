package il.ghostdog.drawingapp


import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ConnectionHelper {
    companion object {
        fun disconnectPlayerFromLobby(lobbyId: String, playerId: String) {
            val databaseInstance = FirebaseDatabase.getInstance()
            val lobbyDB = databaseInstance.getReference("lobbies").child(lobbyId)

            lobbyDB.child("playersStatus").child(playerId).removeValue()
            lobbyDB.child("players").child(playerId).removeValue().addOnSuccessListener {
                lobbyDB.child("players")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.childrenCount == 0L) {
                                //all players left
                                lobbyDB.removeValue()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })
            }
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
    }
}