package il.ghostdog.drawingapp

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class JoinLobbyActivity : AppCompatActivity(), IProgressDialogUser {

    private var mLobbyId : String? = null
    private var mTempRequestingId: String? = null

    override var customProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_lobby)

        mLobbyId = intent.getStringExtra("lobbyId")
        if (mLobbyId != null){
            findViewById<EditText>(R.id.etLobbyId).setText(mLobbyId!!)
        }

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

        showProgressDialog(this@JoinLobbyActivity)
        checkIfLobbyExists(databaseLobbies, mLobbyId!!)
    }

    private fun joinLobby(){
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
                        addPlayer()
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
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkIfLobbyExists(ref: DatabaseReference, child: String){
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.hasChild(child)){
                    if(dataSnapshot.child(child).child("gamePreferences").child("status")
                            .getValue(GameStatus::class.java) == GameStatus.preparing){
                        joinLobby()
                    }else{
                        val myLobby = ref.child(child)
                        checkIfReturning(myLobby)
                    }
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

    private fun checkIfReturning(myLobby: DatabaseReference) {
        myLobby.child("players").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val mAuth = FirebaseAuth.getInstance()
                if(snapshot.hasChild(mAuth.currentUser!!.uid)){
                    joinLobby()
                }else{
                    Toast.makeText(applicationContext, "Sending to leader", Toast.LENGTH_SHORT).show()
                    addPlayer(true)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addPlayer(requesting: Boolean = false){
        val mAuth = FirebaseAuth.getInstance()
        val databaseUsers = FirebaseDatabase.getInstance().getReference("users")
        databaseUsers.child(mAuth.currentUser!!.uid).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserData::class.java)
                if(requesting){
                    mTempRequestingId = "${Constants.REQUESTING_PLAYER_NODE_NAME}-${UUID.randomUUID().toString().substring(0,3)}"
                    FirebaseDatabase.getInstance().getReference("lobbies").child(mLobbyId!!)
                        .child("players")
                        .child(mTempRequestingId!!)
                        .setValue(user!!.nickname).addOnCompleteListener{
                            addJoinRequestListener()
                        }
                }else {
                    FirebaseDatabase.getInstance().getReference("lobbies").child(mLobbyId!!)
                        .child("players").child(mAuth.currentUser!!.uid)
                        .setValue(PlayerData(user!!.nickname))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun addJoinRequestListener(){
        FirebaseDatabase.getInstance().getReference("lobbies").child(mLobbyId!!)
            .child("players")
            .child(mTempRequestingId!!).addValueEventListener(joinRequestListener)
    }
    private fun removeJoinRequestListener(){
        FirebaseDatabase.getInstance().getReference("lobbies").child(mLobbyId!!)
            .child("players")
            .child(mTempRequestingId!!).removeEventListener(joinRequestListener)
    }
    private val joinRequestListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.value != "Allow" && snapshot.value != "Decline") return
            removeJoinRequestListener()
            FirebaseDatabase.getInstance().getReference("lobbies").child(mLobbyId!!)
                .child("players").child(mTempRequestingId!!).removeValue()
            if(snapshot.value == "Allow"){
                joinLobby()
            }else {
                Toast.makeText(applicationContext, "Declined", Toast.LENGTH_SHORT).show()
            }
            cancelProgressDialog()
        }
        override fun onCancelled(error: DatabaseError) {}
    }
}