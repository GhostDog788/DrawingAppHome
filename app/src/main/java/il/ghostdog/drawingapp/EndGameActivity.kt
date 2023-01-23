package il.ghostdog.drawingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class EndGameActivity : AppCompatActivity() {

    private var mPlayersMap: HashMap<String, PlayerData> = HashMap()
    private var lobbyId: String? = null

    private var mDatabaseInstance: FirebaseDatabase? = null
    private var mDatabaseLobby: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_game)

        mPlayersMap = intent.getSerializableExtra("players") as HashMap<String, PlayerData>
        lobbyId = intent.getStringExtra("lobbyId")!!
        mDatabaseInstance = FirebaseDatabase.getInstance()
        mDatabaseLobby = mDatabaseInstance!!.getReference("lobbies").child(lobbyId!!)

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
        mDatabaseLobby!!.child("players").child(uid).removeValue()
        for(key in mPlayersMap.keys){
            if(key != uid){
                mDatabaseLobby!!.child("leader").setValue(key)
                break
            }
        }

        val intent = Intent()
        intent.setClass(this@EndGameActivity, MainMenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun backToLobby() {
        mDatabaseLobby!!.child("drawerID").removeValue()

        val intent = Intent()
        intent.putExtra("lobbyId", lobbyId)

        intent.setClass(this@EndGameActivity, CreateLobbyActivity::class.java)
        startActivity(intent)
        finish()
    }
}