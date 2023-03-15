package il.ghostdog.drawingapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class EndGameActivity : AppCompatActivity(), ILobbyUser {

    private var mPlayersMap: HashMap<String, PlayerData> = HashMap()
    private var lobbyId: String? = null

    override var pingTimerJob: Job? = null
    override var checkPingTimerJob: Job? = null
    override var mPingInterval: Int = Constants.PING_INTERVAL
    override var mCheckPingInterval: Int = Constants.PING_INTERVAL_CHECK
    override var partyLeader: String? = null
    override var databaseMyLobby: DatabaseReference? = null
    override var sharedPref: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_game)

        sharedPref = applicationContext.getSharedPreferences(Constants.SHARED_LOBBIES_NAME, Context.MODE_PRIVATE)

        mPlayersMap = intent.getSerializableExtra("players") as HashMap<String, PlayerData>
        lobbyId = intent.getStringExtra("lobbyId")!!
        partyLeader = intent.getStringExtra("partyLeader")
        databaseMyLobby = FirebaseDatabase.getInstance().getReference("lobbies").child(lobbyId!!)

        val orderedByPoints = mPlayersMap.toList().sortedBy { pair -> pair.second.points }
        setUpWinners(orderedByPoints)
        giveMoney(orderedByPoints)

        findViewById<Button>(R.id.btnToMainMenu).setOnClickListener { exitGame() }
        findViewById<Button>(R.id.btnBackToLobby).setOnClickListener { backToLobby()}
    }

    private fun giveMoney(orderedByPoints: List<Pair<String, PlayerData>>) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val myPair = orderedByPoints.find { p -> p.first == uid }
        val ref = FirebaseDatabase.getInstance().getReference("users").child(uid)
        var placeMultiplayer = 1.0
        when(orderedByPoints.indexOf(myPair)){
            0 -> placeMultiplayer = 1.3
            1 -> placeMultiplayer = 1.2
            2 -> placeMultiplayer = 1.1
        }
        val money = (((myPair!!.second.points / 300) * 5.432) * placeMultiplayer).roundToInt() + 1
        findViewById<TextView>(R.id.tvMoneyEarned).text = money.toString()
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(UserData::class.java)!!
                userData.money += money
                ref.setValue(userData)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setUpWinners(orderedByPoints: List<Pair<String, PlayerData>>) {
        val firstPlayer = orderedByPoints[orderedByPoints.lastIndex]
        findViewById<TextView>(R.id.tvName1).text = firstPlayer.second.name
        findViewById<TextView>(R.id.tvPoints1).text = firstPlayer.second.points.toString()

        if(orderedByPoints.size > 1) {
            val secondPlayer = orderedByPoints[orderedByPoints.lastIndex - 1]
            findViewById<TextView>(R.id.tvName2).text = secondPlayer.second.name
            findViewById<TextView>(R.id.tvPoints2).text = secondPlayer.second.points.toString()
        }

        if(orderedByPoints.size > 2) {
            val thirdPlayer = orderedByPoints[orderedByPoints.lastIndex - 2]
            findViewById<TextView>(R.id.tvName3).text = thirdPlayer.second.name
            findViewById<TextView>(R.id.tvPoints3).text = thirdPlayer.second.points.toString()
        }
    }

    private fun exitGame() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        if(uid == partyLeader){
            setNewLeader()
            return
        }
        ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, uid)
        val intent = Intent()
        intent.setClass(this@EndGameActivity, MainMenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setNewLeader() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        mPlayersMap.remove(uid)
        databaseMyLobby!!.child("leader")
            .setValue(mPlayersMap.keys.first()).addOnCompleteListener{
                ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, uid)
                val intent = Intent()
                intent.setClass(this@EndGameActivity, MainMenuActivity::class.java)
                startActivity(intent)
                finish()
            }
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
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        for(key in mPlayersMap.keys){
            if(partyLeader != key){
                if(uid != key) return
                databaseMyLobby!!.child("leader")
                    .setValue(uid)
                val myList = sharedPref?.getStringSet("lobbyIds", emptySet())!!.toMutableSet()
                myList.add(lobbyId)
                val editor = sharedPref?.edit()
                editor?.putStringSet("lobbyIds", myList)
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