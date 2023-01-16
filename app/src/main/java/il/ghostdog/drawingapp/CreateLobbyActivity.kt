package il.ghostdog.drawingapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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

    private lateinit var tvRounds: TextView
    private lateinit var tvTime: TextView
    private var minRounds: Int = 2
    private var maxRounds: Int = 9
    private var minTime: Int = 30
    private var maxTime: Int = 150
    private var timeJumps: Int = 10

    private var gamePreferences: GamePreferences = GamePreferences()

    private var playersMap: LinkedHashMap<String, PlayerData> = LinkedHashMap()

    private val playersChildListener = object : ChildEventListener{
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            Toast.makeText(applicationContext, "How this was called again?", Toast.LENGTH_SHORT).show()
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_lobby)

        lobbyId = intent.getStringExtra("lobbyId")

        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("lobbies")
        databaseUsers = dataBaseInstance.getReference("users")
        databaseMyLobby = databaseLobbies.child(lobbyId!!)

        tvRounds = findViewById(R.id.tvRounds)
        findViewById<Button>(R.id.btnMinusRounds)
            .setOnClickListener{ onAdditiveButtonClicked(minRounds, maxRounds, -1, tvRounds)}
        findViewById<Button>(R.id.btnPlusRounds)
            .setOnClickListener{ onAdditiveButtonClicked(minRounds, maxRounds, 1, tvRounds)}

        tvTime = findViewById(R.id.tvTime)
        findViewById<Button>(R.id.btnMinusTime)
            .setOnClickListener{ onAdditiveButtonClicked(minTime, maxTime, -timeJumps, tvTime)}
        findViewById<Button>(R.id.btnPlusTime)
            .setOnClickListener{ onAdditiveButtonClicked(minTime, maxTime, timeJumps, tvTime)}

        mAuth = FirebaseAuth.getInstance()

        rvPlayers = findViewById(R.id.rvPlayers)
        rvPlayers.adapter = PlayerRecyclerAdapter(playerRViewDataList, this)
        rvPlayers.layoutManager = GridLayoutManager(this@CreateLobbyActivity, 2)

        lifecycleScope.launch {
            setUpLobby()
        }
    }

    private fun onStartGame() {
        val intent = Intent(this, MainActivity::class.java)
        if(mAuth!!.currentUser!!.uid == partyLeader) {
            updateGamePreferences()
            databaseMyLobby!!.child("gamePreferences").setValue(gamePreferences)
        }
        intent.putExtra("lobbyId", lobbyId)
        intent.putExtra("language", gamePreferences.language)
        intent.putExtra("rounds", gamePreferences.rounds)
        intent.putExtra("turnTime", gamePreferences.turnTime)
        databaseMyLobby!!.child("players").removeEventListener(playersChildListener)
        startActivity(intent)
        finish()
    }

    private fun updateGamePreferences() {
        gamePreferences.status = GameStatus.active
        when (findViewById<RadioGroup>(R.id.rgLanguage).checkedRadioButtonId) {
            R.id.rbEnglish -> {
                gamePreferences.language = "english"
            }
            R.id.rbHebrew -> {
                gamePreferences.language = "hebrew"
            }
        }
        gamePreferences.rounds = findViewById<TextView>(R.id.tvRounds).text.toString().toInt()
        gamePreferences.turnTime = findViewById<TextView>(R.id.tvTime).text.toString().toInt()
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
        databaseMyLobby!!.child("players").addChildEventListener(playersChildListener)

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

        databaseMyLobby!!.child("gamePreferences").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(GamePreferences::class.java) ?: return
                gamePreferences = data

                if (gamePreferences.status == GameStatus.active){
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

        findViewById<LinearLayout>(R.id.llGamePreferences).visibility = View.VISIBLE
        tvRounds.text = gamePreferences.rounds.toString()
        tvTime.text = gamePreferences.turnTime.toString()

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

    private fun onAdditiveButtonClicked(minRange: Int, maxRange: Int, amount: Int, display: TextView?) {
        var value = display!!.text.toString().toInt()
        if(value + amount > maxRange || value + amount < minRange) return

        value += amount
        display!!.text = value.toString()
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