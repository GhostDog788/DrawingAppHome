package il.ghostdog.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val btnJoinLobby = findViewById<Button>(R.id.btnJoinLobby)
        btnJoinLobby.setOnClickListener{ onJoinLobbyClicked()}

        val btnCreateLobby = findViewById<Button>(R.id.btnCreateLobby)
        btnCreateLobby.setOnClickListener{ onCreateLobbyClicked()}

        val btnSignOut = findViewById<Button>(R.id.btnSignOut)
        btnSignOut.setOnClickListener{ onSignOut()}
    }

    private fun onJoinLobbyClicked() {
        startActivity(Intent(this, JoinLobbyActivity::class.java))
        finish()
    }

    private fun onCreateLobbyClicked() {
        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("lobbies")
        val lobbyId = UUID.randomUUID().toString().substring(0,4) //unique id of the lobby and join code

        Toast.makeText(applicationContext, lobbyId, Toast.LENGTH_LONG).show()

        databaseLobbies.child(lobbyId)
        val intent = Intent(this, CreateLobbyActivity::class.java)
        intent.putExtra("lobbyId", lobbyId)
        startActivity(intent)
        finish()
    }

    private fun onSignOut(){
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}