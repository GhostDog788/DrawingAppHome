package il.ghostdog.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val btnJoinLobby = findViewById<Button>(R.id.btnJoinLobby)
        btnJoinLobby.setOnClickListener{ onJoinLobbyClicked()}

        val btnCreateLobby = findViewById<Button>(R.id.btnCreateLobby)
        btnCreateLobby.setOnClickListener{ onCreateLobbyClicked()}
    }

    private fun onJoinLobbyClicked() {
        startActivity(Intent(this, JoinLobbyActivity::class.java))
        finish()
    }

    private fun onCreateLobbyClicked() {
        startActivity(Intent(this, CreateLobbyActivity::class.java))
        finish()
    }
}