package il.ghostdog.drawingapp

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class CreateLobbyActivity : AppCompatActivity() {

    private var lobbyId: String? = null

    private var customProgressDialog: Dialog? = null

    private var mAuth : FirebaseAuth? = null
    private var databaseMyLobby : DatabaseReference? = null
    private var databaseUsers : DatabaseReference? = null
    private var partyLeader : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_lobby)

        lobbyId = intent.getStringExtra("lobbyId")

        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("lobbies")
        databaseUsers = dataBaseInstance.getReference("users")
        databaseMyLobby = databaseLobbies.child(lobbyId!!)

        mAuth = FirebaseAuth.getInstance()

        setUpLobby()
    }

    private fun onStartGame() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("lobbyId", lobbyId)
        if(mAuth!!.currentUser!!.uid == partyLeader) {
            databaseMyLobby!!.child("status").setValue(GameStatus.active.toString())
        }
        startActivity(intent)
        finish()
    }

    private fun setUpLobby(){
        showProgressDialog()

        databaseUsers!!.child(mAuth!!.currentUser!!.uid).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserData::class.java)
                databaseMyLobby!!.child("players").child(mAuth!!.currentUser!!.uid)
                    .setValue(PlayerData(user!!.nickname))
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        databaseMyLobby!!.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                cancelProgressDialog()
                if(dataSnapshot.hasChild("leader")){
                    setUpPlayer()
                }else{
                    setUpLeader()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // An error occurred
                Toast.makeText(applicationContext, "Error in setting up lobby", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setUpPlayer() {
        databaseMyLobby!!.child("leader").addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                partyLeader = dataSnapshot.getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
                // An error occurred
                Toast.makeText(applicationContext, "Error in setting up party leader", Toast.LENGTH_SHORT).show()
            }
        })

        databaseMyLobby!!.child("status").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameStatus = snapshot.getValue(String::class.java)

                if (gameStatus == GameStatus.active.toString()){
                    onStartGame()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun setUpLeader() {
        databaseMyLobby!!.child("leader").setValue(mAuth!!.currentUser!!.uid)
        databaseMyLobby!!.child("status").setValue(GameStatus.preparing.toString())

        partyLeader = mAuth!!.currentUser!!.uid

        val btnCreate = findViewById<Button>(R.id.btnStartGame)
        btnCreate.visibility = View.VISIBLE
        btnCreate.setOnClickListener{ onStartGame()}
    }


    private fun showProgressDialog(){
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
}