package il.ghostdog.drawingapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class EndGameActivity : AppCompatActivity(), ILobbyUser {

    private var mPlayersMap: HashMap<String, PlayerData> = HashMap()
    private var lobbyId: String? = null

    override var pingTimerJob: Job? = null
    override var checkPingTimerJob: Job? = null
    override var mPingInterval: Int = 8
    override var mCheckPingInterval: Int = 15
    override var partyLeader: String? = null
    override var databaseMyLobby: DatabaseReference? = null
    override var sharedPref: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_game)

        sharedPref = applicationContext.getSharedPreferences(Constants.SHARED_LOBBIES_NAME, Context.MODE_PRIVATE)

        mPlayersMap = intent.getSerializableExtra("players") as HashMap<String, PlayerData>
        lobbyId = intent.getStringExtra("lobbyId")!!
        databaseMyLobby = FirebaseDatabase.getInstance().getReference("lobbies").child(lobbyId!!)

        setUpWinners()

        findViewById<Button>(R.id.btnToMainMenu).setOnClickListener { exitGame() }
        findViewById<Button>(R.id.btnBackToLobby).setOnClickListener { backToLobby()}
    }

    private fun setUpWinners() {
        val orderedByPoints = mPlayersMap.toList().sortedBy { pair -> pair.second.points }
        val firstPlayer = orderedByPoints[orderedByPoints.lastIndex]
        val secondPlayer = orderedByPoints[orderedByPoints.lastIndex - 1]

        findViewById<TextView>(R.id.tvName1).text = firstPlayer.second.name
        findViewById<TextView>(R.id.tvPoints1).text = firstPlayer.second.points.toString()

        findViewById<TextView>(R.id.tvName2).text = secondPlayer.second.name
        findViewById<TextView>(R.id.tvPoints2).text = secondPlayer.second.points.toString()

        if(orderedByPoints.size > 2)
        {
            val thirdPlayer = orderedByPoints[orderedByPoints.lastIndex - 2]
            findViewById<TextView>(R.id.tvName3).text = thirdPlayer.second.name
            findViewById<TextView>(R.id.tvPoints3).text = thirdPlayer.second.points.toString()
        }
    }

    private fun exitGame() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, uid)
        val intent = Intent()
        intent.setClass(this@EndGameActivity, MainMenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun backToLobby() {
        databaseMyLobby!!.child("drawerID").removeValue()

        val intent = Intent()
        intent.putExtra("lobbyId", lobbyId)

        intent.setClass(this@EndGameActivity, CreateLobbyActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onLeaderDisconnected() {
        Toast.makeText(applicationContext, "Leader disconnected", Toast.LENGTH_SHORT).show()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        for(key in mPlayersMap.keys){
            if(partyLeader != key){
                if(uid != key) return
                databaseMyLobby!!.child("leader")
                    .setValue(uid)
                val editor = sharedPref?.edit()
                editor?.putString("lobbyId", lobbyId)
                editor?.apply()
                return
            }
        }
    }

    override fun onStop() {
        pingTimerJob?.cancel()
        checkPingTimerJob?.cancel()
        super.onStop()
    }

    override fun onResume() {
        lifecycleScope.launch {
            while (databaseMyLobby == null || partyLeader == null) {
                delay(100)
            }
            pingTimerJob = startPingTimer(lifecycleScope)
            checkPingTimerJob = startPingCheckTimer(lifecycleScope,
                FirebaseAuth.getInstance().currentUser!!.uid)
            updateMyStatus()
        }
        super.onResume()
    }
}