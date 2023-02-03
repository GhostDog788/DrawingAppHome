package il.ghostdog.drawingapp

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase

class LobbyTimeOutService : Service() {
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var isServiceStopped = false

    private var lobbyId: String? = null
    private var playerId: String? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LobbyTimeOutService = this@LobbyTimeOutService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        lobbyId = intent?.getStringExtra("lobbyId")
        playerId = intent?.getStringExtra("playerId")

        handler = Handler()
        runnable = Runnable {
            Toast.makeText(applicationContext, "Timer is finished! $lobbyId $playerId", Toast.LENGTH_SHORT).show()
            ConnectionHelper.disconnectPlayerFromLobby(lobbyId!!, playerId!!)
        }
        handler.postDelayed(runnable, 10000)

        return binder
    }

    fun stopService() {
        isServiceStopped = true
        stopSelf()
    }

    override fun onDestroy() {
        if(isServiceStopped){
            //stopped by app
            handler.removeCallbacks(runnable)
        }else{
            //stopped by system
            Toast.makeText(applicationContext, "Destroyed by system", Toast.LENGTH_SHORT).show()
        }
        //Thread.sleep(3000)
        Toast.makeText(applicationContext, "Now destroying", Toast.LENGTH_SHORT).show()
        super.onDestroy()
    }
}