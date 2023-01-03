package il.ghostdog.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class CreateLobbyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_lobby)

        val btnCreate = findViewById<Button>(R.id.btnCreateLobby)
        btnCreate.setOnClickListener{ onCreateLobby()}
    }

    private fun onCreateLobby() {
        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("Lobbies")
        val lobbyId = UUID.randomUUID().toString().substring(0,8) //unique id of the lobby and join code

        Toast.makeText(applicationContext, lobbyId, Toast.LENGTH_LONG).show()

        databaseLobbies.child(lobbyId)
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("lobbyId", lobbyId)
        startActivity(intent)
        finish()
    }
}