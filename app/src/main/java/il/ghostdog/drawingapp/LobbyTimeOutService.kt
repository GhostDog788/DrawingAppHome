package il.ghostdog.drawingapp

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase

class LobbyTimeOutService : Service() {
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private var lobbyId: String? = null
    private var playerId: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lobbyId = intent?.getStringExtra("lobbyId")
        playerId = intent?.getStringExtra("playerId")

        handler = Handler()
        runnable = Runnable {
            Toast.makeText(applicationContext, "Timer is finished! $lobbyId $playerId", Toast.LENGTH_SHORT).show()
            ConnectionHelper().disconnectPlayerFromLobby(lobbyId!!, playerId!!)
        }
        handler.postDelayed(runnable, 10000)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }
}