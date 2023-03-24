package il.ghostdog.drawingapp

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.timerTask

class LastSeenService : Service() {
    private lateinit var userId: String
    private var timer: Timer? = null
    private lateinit var myRef: DatabaseReference

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Stop the timer when the screen is turned off
            stopTimer()
        }
    }

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Start the timer when the screen is turned on
            startTimer()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getStringExtra("userId") ?: ""
        myRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        registerScreenOffReceiver()
        registerScreenOnReceiver()
        startTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterScreenOffReceiver()
        unregisterScreenOnReceiver()
        stopTimer()
    }

    private fun registerScreenOffReceiver() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    private fun registerScreenOnReceiver() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(screenOnReceiver, filter)
    }

    private fun unregisterScreenOffReceiver() {
        unregisterReceiver(screenOffReceiver)
    }

    private fun unregisterScreenOnReceiver() {
        unregisterReceiver(screenOnReceiver)
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Update the current time on the user node in the realtime database every 60 seconds
                updateLastSeen()
            }
        }, 0, 6000)
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private suspend fun getCurrentTimeFromFirebase() : Date{
        var date: Date? = null

        val database = FirebaseDatabase.getInstance().reference
        database.child(".info/serverTimeOffset").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val offset = dataSnapshot.getValue(Long::class.java) ?: 0L
                val estimatedServerTimeMs = System.currentTimeMillis() + offset
                val currentTime = Date(estimatedServerTimeMs)
                // use the current time here
                date = currentTime
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        withContext(Dispatchers.IO){
            while(date == null)
            {
                awaitFrame()
            }
        }
        return date!!
    }
    private fun updateLastSeen() = GlobalScope.launch(Dispatchers.IO) {
        val date: Date = getCurrentTimeFromFirebase()
        val israelZone: ZoneId = ZoneId.of("Asia/Jerusalem")
        val localDateTime = LocalDateTime.ofInstant(date.toInstant(), israelZone)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val currentTime = localDateTime.format(formatter)
        myRef.child("lastSeen").setValue(currentTime)
    }
}