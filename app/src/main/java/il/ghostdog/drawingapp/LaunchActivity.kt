package il.ghostdog.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LaunchActivity : AppCompatActivity() {
    companion object{
        lateinit var launchIntent: Intent
        var firstTime = true
    }

    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        if (firstTime) {
            launchIntent = intent
            firstTime = false
        }

        mAuth = FirebaseAuth.getInstance()
        
        if (mAuth!!.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            //sends the user to login and once finished it will return here and pass the conditional
        }

        if(Constants.lastSeenServiceIntent == null){
            Constants.lastSeenServiceIntent = Intent(this, LastSeenService::class.java)
            Constants.lastSeenServiceIntent!!.putExtra("userId", FirebaseAuth.getInstance().currentUser!!.uid)
            startService(Constants.lastSeenServiceIntent)
        }

        fillGuessWordMap()

        //will change when intent is introduced
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
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