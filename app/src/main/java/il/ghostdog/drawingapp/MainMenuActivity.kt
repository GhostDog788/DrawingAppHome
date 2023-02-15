package il.ghostdog.drawingapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        cleanPastLobbies()

        if(Constants.GUESS_WORDS_MAP.isEmpty()) {
            fillGuessWordMap()
        }

        val btnJoinLobby = findViewById<Button>(R.id.btnJoinLobby)
        btnJoinLobby.setOnClickListener{ onJoinLobbyClicked()}

        val btnCreateLobby = findViewById<Button>(R.id.btnCreateLobby)
        btnCreateLobby.setOnClickListener{ onCreateLobbyClicked()}

        val btnSignOut = findViewById<Button>(R.id.btnSignOut)
        btnSignOut.setOnClickListener{ onSignOut()}
    }

    private fun fillGuessWordMap() {
        val dbR = FirebaseDatabase.getInstance().getReference("guessWords")
        dbR.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val list = (child.value as ArrayList<String>?)!!
                    Constants.GUESS_WORDS_MAP[child.key!!] = list
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun cleanPastLobbies() {
        val sharedPref = applicationContext.getSharedPreferences(Constants.SHARED_LOBBIES_NAME, Context.MODE_PRIVATE)
        val lobbyIdToDelete = sharedPref.getString("lobbyId","E")
        if(lobbyIdToDelete != null && lobbyIdToDelete != "E"){
            val dbLobby = FirebaseDatabase.getInstance().getReference("lobbies").child(lobbyIdToDelete)
            dbLobby.child("leader").addListenerForSingleValueEvent(object :  ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value == FirebaseAuth.getInstance().currentUser!!.uid){
                        dbLobby.removeValue()
                    }
                    val editor = sharedPref?.edit()
                    editor?.remove("lobbyId")
                    editor?.apply()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun onJoinLobbyClicked() {
        startActivity(Intent(this, JoinLobbyActivity::class.java))
        finish()
    }

    private fun onCreateLobbyClicked() {
        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("lobbies")
        val lobbyId = UUID.randomUUID().toString().substring(0,4) //unique id of the lobby and join code

        //creates new lobby in db and sets leaderId
        databaseLobbies.child(lobbyId).child("leader")
            .setValue(FirebaseAuth.getInstance()!!.currentUser!!.uid)
        val intent = Intent(this, CreateLobbyActivity::class.java)
        intent.putExtra("lobbyId", lobbyId)
        startActivity(intent)
        finish()
    }

    private fun onSignOut(){
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}