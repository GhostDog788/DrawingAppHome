package il.ghostdog.drawingapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

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
        val lobbyIdToDeleteSet = sharedPref.getStringSet("lobbyIds", emptySet())
        for (id in lobbyIdToDeleteSet!!){
            Toast.makeText(applicationContext, "$id", Toast.LENGTH_SHORT).show()
        }
        if(lobbyIdToDeleteSet != null && lobbyIdToDeleteSet.isNotEmpty()) {
            for (lobbyIdToDelete in lobbyIdToDeleteSet) {
                val dbLobby = FirebaseDatabase.getInstance().getReference("lobbies").child(lobbyIdToDelete)
                dbLobby.child("leader").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value == FirebaseAuth.getInstance().currentUser!!.uid) {
                            checkIfOverPingTime(dbLobby, sharedPref, lobbyIdToDelete)
                        }else{
                            val myList = sharedPref.getStringSet("lobbyIds", emptySet())!!.toMutableSet()
                            myList.remove(lobbyIdToDelete)
                            val editor = sharedPref.edit()
                            editor.putStringSet("lobbyIds", myList)
                            editor.apply()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun checkIfOverPingTime(dbLobby: DatabaseReference, sharedPref: SharedPreferences, lobbyId:String) {
        dbLobby.child("playersStatus")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value is Long){ //very uncommon
                        dbLobby.removeValue()
                        val myList = sharedPref.getStringSet("lobbyIds", emptySet())!!.toMutableSet()
                        myList.remove(lobbyId)
                        val editor = sharedPref.edit()
                        editor.putStringSet("lobbyIds", myList)
                        editor.apply()
                        return
                    }
                    val timeString = snapshot.getValue(String::class.java) ?: return
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val dateTimeLastLeader = LocalDateTime.parse(timeString, formatter)
                    GlobalScope.launch(Dispatchers.Default){
                        val date = ConnectionHelper.getCurrentTimeFromFirebase()
                        val israelZone: ZoneId = ZoneId.of("Asia/Jerusalem")
                        val localDateTime = LocalDateTime.ofInstant(date.toInstant(), israelZone)
                        val difference = (localDateTime.toEpochSecond(ZoneOffset.UTC) - dateTimeLastLeader.toEpochSecond(ZoneOffset.UTC))
                        runOnUiThread{
                            Toast.makeText(applicationContext, "$difference", Toast.LENGTH_SHORT).show()
                        }
                        if(difference > Constants.PING_INTERVAL_CHECK + 5) {
                            dbLobby.removeValue()
                            val myList = sharedPref.getStringSet("lobbyIds", emptySet())!!.toMutableSet()
                            myList.remove(lobbyId)
                            val editor = sharedPref.edit()
                            editor.putStringSet("lobbyIds", myList)
                            editor.apply()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
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