package il.ghostdog.drawingapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class CreateLobbyActivity : AppCompatActivity(), PlayerRecyclerAdapter.RecyclerViewEvent {

    private var lobbyId: String? = null

    private var customProgressDialog: Dialog? = null

    private var mAuth : FirebaseAuth? = null
    private var databaseMyLobby : DatabaseReference? = null
    private var databaseUsers : DatabaseReference? = null
    private var partyLeader : String? = null

    private lateinit var rvPlayers: RecyclerView
    private var playerRViewDataList : ArrayList<PlayerRViewData> = ArrayList()

    private var playersMap: LinkedHashMap<String, PlayerData> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_lobby)

        lobbyId = intent.getStringExtra("lobbyId")

        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("lobbies")
        databaseUsers = dataBaseInstance.getReference("users")
        databaseMyLobby = databaseLobbies.child(lobbyId!!)

        mAuth = FirebaseAuth.getInstance()

        rvPlayers = findViewById(R.id.rvPlayers)
        rvPlayers.adapter = PlayerRecyclerAdapter(playerRViewDataList, this)
        rvPlayers.layoutManager = LinearLayoutManager(this@CreateLobbyActivity)

        lifecycleScope.launch {
            setUpLobby()
        }
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

    private suspend fun setUpLobby(){
        showProgressDialog()
        //before adding player to db for the listener to activate on the player arrival
        databaseMyLobby!!.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
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
        withContext(Dispatchers.IO){
            while(partyLeader == null)
            {
                awaitFrame()
            }
        }
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
        //calls to all players at start + new ones
        databaseMyLobby!!.child("players").addChildEventListener(object : ChildEventListener{
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val playerData = snapshot.getValue(PlayerData::class.java)!!
                playersMap[snapshot.key!!] = playerData
                playerRViewDataList.add(PlayerRViewData(snapshot.key!!, playerData, snapshot.key == partyLeader))
                val index: Int
                if(snapshot.key == partyLeader){
                    index = 0
                    val temp = playerRViewDataList[0]
                    playerRViewDataList[0] = playerRViewDataList[playerRViewDataList.size - 1]
                    playerRViewDataList[playerRViewDataList.size - 1] = temp
                }else{
                    index = playerRViewDataList.size - 1
                }
                rvPlayers.adapter!!.notifyItemInserted(index)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                if(snapshot.key == mAuth!!.currentUser!!.uid){
                    //player have been kicked
                    Toast.makeText(applicationContext, "you have been kicked", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@CreateLobbyActivity, MainMenuActivity::class.java))
                    finish()
                }

                playersMap.remove(snapshot.key)
                var index = 0
                while(index < playerRViewDataList.size) {
                    if(playerRViewDataList[index].userId == snapshot.key!!){
                        break
                    }
                    index++
                }
                playerRViewDataList.removeAt(index)
                rvPlayers.adapter!!.notifyItemRemoved(index)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        cancelProgressDialog()
    }

    private fun setUpPlayer() {
        databaseMyLobby!!.child("leader").addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                partyLeader = dataSnapshot.getValue(String::class.java)!! //had to be initialized
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

    override fun onItemClicked(position: Int) {
        val player = playerRViewDataList[position]

        if(player.isLeader || mAuth!!.currentUser!!.uid != partyLeader) return

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Kick player")
        alertDialogBuilder.setMessage("Do you want to kick the player from the lobby?")
        alertDialogBuilder.setPositiveButton("Kick") { dialog, _ ->
            Toast.makeText(applicationContext, "Player Kicked!", Toast.LENGTH_SHORT).show()
            kickPlayer(player)
            dialog.dismiss()
        }
        alertDialogBuilder.setNegativeButton("No"){ dialog, _ ->
            Toast.makeText(applicationContext, "Canceled", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        alertDialogBuilder.show()
    }

    private fun kickPlayer(player: PlayerRViewData) {
        databaseMyLobby!!.child("players").child(player.userId).removeValue()
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