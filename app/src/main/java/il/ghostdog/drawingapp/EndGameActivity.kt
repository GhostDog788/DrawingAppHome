package il.ghostdog.drawingapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class EndGameActivity : AppCompatActivity() {

    private var mPlayersMap: HashMap<String, PlayerData> = HashMap()
    private lateinit var mPartyLeader: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_game)

        mPlayersMap = intent.getSerializableExtra("players") as HashMap<String, PlayerData>
        mPartyLeader = intent.getStringExtra("leaderId")!!

        findViewById<TextView>(R.id.tvLeader).text = mPartyLeader

    }
}