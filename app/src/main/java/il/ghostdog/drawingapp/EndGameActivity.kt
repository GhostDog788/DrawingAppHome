package il.ghostdog.drawingapp

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.SoundPool
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class EndGameActivity : AppCompatActivity(), ILobbyUser, IAudioUser {

    private var mPlayersMap: HashMap<String, PlayerData> = HashMap()
    private var lobbyId: String? = null

    override var pingTimerJob: Job? = null
    override var checkPingTimerJob: Job? = null
    override var mPingInterval: Int = Constants.PING_INTERVAL
    override var mCheckPingInterval: Int = Constants.PING_INTERVAL_CHECK
    override var partyLeader: String? = null
    override var databaseMyLobby: DatabaseReference? = null
    override var sharedPref: SharedPreferences? = null

    override lateinit var soundPool: SoundPool
    override var clickSoundId: Int = -1
    override var errorSoundId: Int = -1
    override var softClickSoundId: Int = -1
    private var taDaSoundId: Int = -1
    private var taDa2SoundId: Int = -1
    private var taDa3SoundId: Int = -1
    private var drumRollSoundId: Int = -1

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

        setUpSoundPool(this)
        taDaSoundId = soundPool.load(this, R.raw.tada, 1)
        taDa2SoundId = soundPool.load(this, R.raw.tada2, 1)
        taDa3SoundId = soundPool.load(this, R.raw.tada3, 1)
        drumRollSoundId = soundPool.load(this, R.raw.drum_roll, 1)

        val btnToMain = findViewById<Button>(R.id.btnToMainMenu)
        btnToMain.visibility = View.INVISIBLE
        btnToMain.setOnClickListener {
            soundPool.play(clickSoundId, 1F, 1F,0,0, 1F)
            exitGame() }
        val btnBack = findViewById<Button>(R.id.btnBackToLobby)
        btnBack.visibility = View.INVISIBLE
        btnBack.setOnClickListener {
            soundPool.play(clickSoundId, 1F, 1F,0,0, 1F)
            backToLobby()}
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

        showAnim(orderedByPoints.size)
    }

    private fun showAnim(size: Int) {
        val excludeThird = size < 3
        if(excludeThird) {
            findViewById<LinearLayout>(R.id.llThird).visibility = View.GONE
        }else{
            findViewById<LinearLayout>(R.id.llThird).visibility = View.INVISIBLE
        }
        findViewById<LinearLayout>(R.id.llSecond).visibility = View.INVISIBLE
        findViewById<LinearLayout>(R.id.llFirst).visibility = View.INVISIBLE
        lifecycleScope.launch {
            delay(1500)
            if(!excludeThird) {
                //anim of 3trd place
                soundPool.play(taDa3SoundId, 1F, 1F,0,0, 1F)
                findViewById<LinearLayout>(R.id.llThird).visibility = View.VISIBLE
                YoYo.with(Techniques.FadeInLeft).duration(2000).onEnd {
                    YoYo.with(Techniques.Bounce).playOn(findViewById(R.id.llThird))
                }.playOn(findViewById(R.id.llThird))
                delay(3000)
            }
            //anim of 2scd place
            soundPool.play(taDa2SoundId, 1F, 1F,0,0, 1F)
            findViewById<LinearLayout>(R.id.llSecond).visibility = View.VISIBLE
            var technique = Techniques.FadeInRight
            if(excludeThird) technique = Techniques.FadeInUp
            YoYo.with(technique).duration(2000).onEnd{
                YoYo.with(Techniques.Pulse).playOn(findViewById(R.id.llSecond))
            }.playOn(findViewById(R.id.llSecond))
            delay(3000)
            //anim of 1fst first place
            findViewById<LinearLayout>(R.id.llFirst).visibility = View.VISIBLE
            soundPool.play(drumRollSoundId, 1F, 1F,0,0, 1F)
            YoYo.with(Techniques.FadeInDown).duration(3000).onEnd{
                soundPool.play(taDaSoundId, 1F, 1F,0,0, 1F)
                YoYo.with(Techniques.Tada).onEnd{
                    val btnToMain = findViewById<Button>(R.id.btnToMainMenu)
                    val btnBack = findViewById<Button>(R.id.btnBackToLobby)
                    btnBack.visibility = View.VISIBLE
                    YoYo.with(Techniques.FadeIn).duration(1500).onEnd{
                        btnToMain.visibility = View.VISIBLE
                        YoYo.with(Techniques.FadeIn).duration(1500).playOn(btnToMain)
                    }.playOn(btnBack)
                    lifecycleScope.launch{
                        delay(1500)

                        var countT = 10000
                        while (countT > 0){
                            if(!excludeThird) {
                                YoYo.with(Techniques.Swing).duration(1500)
                                    .playOn(findViewById(R.id.llThird))
                                delay(1500)
                            }
                            YoYo.with(Techniques.Swing).duration(1500).playOn(findViewById(R.id.llSecond))
                            delay(1500)
                            YoYo.with(Techniques.Swing).playOn(findViewById(R.id.llFirst))
                            delay(2000)
                            countT--
                        }
                    }
                }.playOn(findViewById(R.id.llFirst))
            }.playOn(findViewById(R.id.llFirst))
        }
    }

    private fun exitGame() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        if(uid == partyLeader){
            setNewLeader()
            return
        }
        ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, uid)
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
    }

    private fun setNewLeader() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        mPlayersMap.remove(uid)
        databaseMyLobby!!.child("leader")
            .setValue(mPlayersMap.keys.first()).addOnCompleteListener{
                ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, uid)
                //check if this is the last activity
                val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val taskList = activityManager.getRunningTasks(10)
                if (taskList[0].numActivities == 1 && taskList[0].topActivity!!.className == this.javaClass.name) {
                    startActivity(Intent(this, MainMenuActivity::class.java))
                }
                finish()
            }
    }

    private fun backToLobby() {
        databaseMyLobby!!.child("drawerID").removeValue()
        databaseMyLobby!!.child("endTurnTrigger").removeValue()
        databaseMyLobby!!.child("turnTimeLeft").removeValue()

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
    override fun onBackPressed() {
        exitGame()
    }
}