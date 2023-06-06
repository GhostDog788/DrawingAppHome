package il.ghostdog.drawingapp

import android.content.Context
import android.content.Intent
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.scaleMatrix
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LaunchActivity : AppCompatActivity(), IAudioUser {
    companion object{
        var lastExtras: Bundle? = null
    }

    private var mAuth: FirebaseAuth? = null
    private val splashScreenTime = 2200L

    override lateinit var soundPool: SoundPool
    override var clickSoundId: Int = -1
    override var errorSoundId: Int = -1
    override var softClickSoundId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.splashScreenTheme)
        setContentView(R.layout.activity_launch)

        mAuth = FirebaseAuth.getInstance()

        setUpSoundPool(this)
        val woof1ID = soundPool.load(this, R.raw.woof1, 1)
        //animation
        lifecycleScope.launch {
            delay(200)
            soundPool.play(woof1ID, 1F,1F,0,0,1F)
        }
        val iv = findViewById<ImageView>(R.id.ivLogo)
        iv.animate()
            .scaleX(1.25f)
            .scaleY(1.25f)
            .translationY(-100f)
            .setDuration(1000)
            .withEndAction {
                iv.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(1000)
                    .start()
            }
            .start()

        if (mAuth!!.currentUser == null) {
            lastExtras = intent.extras
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
            //sends the user to login and once finished it will return here and pass the conditional
        }

        if(Constants.lastSeenServiceIntent == null){
            Constants.lastSeenServiceIntent = Intent(this, LastSeenService::class.java)
            Constants.lastSeenServiceIntent!!.putExtra("userId", FirebaseAuth.getInstance().currentUser!!.uid)
            startService(Constants.lastSeenServiceIntent)
        }

        if(Constants.GUESS_WORDS_MAP.isEmpty()) {
            fillGuessWordMap()
        }
        setToken()
        setUserNameInConstants()

        //handle dynamic link
        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
            .addOnSuccessListener { pendingDynamicLinkData ->
                // Get the deep link from the result
                val deepLink = pendingDynamicLinkData?.link
                if (deepLink != null) {
                    val lobbyId = deepLink.getQueryParameter("lobbyId")
                    if (lobbyId != null) {
                        Log.i("Dynamic", "find id $lobbyId")
                        val toSendIntent = Intent(this, JoinLobbyActivity::class.java)
                        toSendIntent.putExtra("lobbyId", lobbyId)
                        toSendIntent.putExtra("startAuto", true)
                        startActivity(toSendIntent)
                        finish()
                    }
                }
            }
            .addOnFailureListener { e -> Log.e("Dynamic link", "Error getting dynamic link: $e") }

        var myExtras = lastExtras
        if(lastExtras == null) { //no extras from original intent
            myExtras = intent.extras
        }
        if(myExtras == null) { //no extras from original AND current
            Handler().postDelayed({
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            }, splashScreenTime)
            return
        }
        lastExtras = null//empty last cause the data is in myExtras
        //use extras
        if(myExtras.containsKey("targetName")) {
            if (myExtras.getString("targetName") == "JoinLobbyActivity"){
                val toSendIntent = Intent(this, JoinLobbyActivity::class.java)
                toSendIntent.putExtra("lobbyId", myExtras.getString("lobbyId"))
                toSendIntent.putExtra("startAuto", true)
                startActivity(toSendIntent)
                finish()
                return
            }
        }

        //to ensure there will always be a main activity in the background
        Handler().postDelayed({
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }, splashScreenTime)
    }

    private fun setUserNameInConstants() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("users").child(uid)
        ref.child("nickname").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                Constants.myUserName = snapshot.getValue(String::class.java)!!
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setToken() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("users").child(uid)
        FBMessagingService.sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            FBMessagingService.token = it
            ref.child("token").setValue(it)
        }
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
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}