package il.ghostdog.drawingapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class LaunchActivity : AppCompatActivity() {
    companion object{
        var lastExtras: Bundle? = null
    }

    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        mAuth = FirebaseAuth.getInstance()

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
        var myExtras = lastExtras
        if(lastExtras == null) { //no extras from original intent
            myExtras = intent.extras
        }
        if(myExtras == null) { //no extras from original AND current
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
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

        //will change when intent is introduced
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
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