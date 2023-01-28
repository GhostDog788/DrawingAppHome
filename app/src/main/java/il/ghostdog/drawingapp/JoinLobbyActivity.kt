package il.ghostdog.drawingapp

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.*

class JoinLobbyActivity : AppCompatActivity() {

    private var mLobbyId : String? = null

    private var customProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_lobby)

        val btnJoinLobby = findViewById<Button>(R.id.btnJoinLobby)
        btnJoinLobby.setOnClickListener{ onJoinLobbyClicked()}
    }

    private fun onJoinLobbyClicked() {
        val lobbyIdInput = findViewById<EditText>(R.id.etLobbyId).text
        if(lobbyIdInput.isEmpty()){
            Toast.makeText(applicationContext, "Please enter lobby id", Toast.LENGTH_SHORT).show()
            return
        }
        mLobbyId = lobbyIdInput.toString()

        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("lobbies")

        showProgressDialog()
        checkIfLobbyExists(databaseLobbies, mLobbyId!!)
    }

    private fun joinLobby(){
        cancelProgressDialog()

        FirebaseDatabase.getInstance().getReference("lobbies").child(mLobbyId!!).child("gamePreferences").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val gamePreferences = snapshot.getValue(GamePreferences::class.java)

                val intent = Intent()
                intent.putExtra("lobbyId", mLobbyId!!)
                when (gamePreferences!!.status) {
                    GameStatus.active -> {
                        intent.setClass(this@JoinLobbyActivity, MainActivity::class.java)
                        intent.putExtra("reEntering", true)
                        intent.putExtra("language", gamePreferences.language)
                        intent.putExtra("rounds", gamePreferences.rounds)
                        intent.putExtra("turnTime", gamePreferences.turnTime)
                    }
                    GameStatus.preparing -> {
                        intent.setClass(this@JoinLobbyActivity, CreateLobbyActivity::class.java)
                    }
                    else -> {
                        Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                cancelProgressDialog()
                startActivity(intent)
                finish()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun checkIfLobbyExists(ref: DatabaseReference, child: String){
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.hasChild(child)){
                    joinLobby()
                }else{
                    cancelProgressDialog()
                    Toast.makeText(applicationContext, "Invalid id", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // An error occurred
                Toast.makeText(applicationContext, "Error in joining lobby", Toast.LENGTH_SHORT).show()
            }
        })
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