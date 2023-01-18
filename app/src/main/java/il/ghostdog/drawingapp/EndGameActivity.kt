package il.ghostdog.drawingapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class EndGameActivity : AppCompatActivity() {

    private var mPlayersMap: LinkedHashMap<String, PlayerData> = LinkedHashMap()
    private lateinit var mPartyLeader: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_game)

        mPlayersMap = intent.getSerializableExtra("players") as LinkedHashMap<String, PlayerData>
        mPartyLeader = intent.getStringExtra("leaderId")!!

        Toast.makeText(applicationContext, "khgop", Toast.LENGTH_SHORT).show()
    }
}