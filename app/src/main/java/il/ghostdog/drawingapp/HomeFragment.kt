package il.ghostdog.drawingapp

import android.content.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


class HomeFragment : Fragment(R.layout.fragment_home), IAudioUser {
    override lateinit var soundPool: SoundPool
    override var clickSoundId: Int = -1
    override var errorSoundId: Int = -1
    override var softClickSoundId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!(activity as MainMenuActivity).checkedPastLobbies) {
            cleanPastLobbies()
            (activity as MainMenuActivity).checkedPastLobbies = true
        }

        val btnJoinLobby = view.findViewById<Button>(R.id.btnJoinLobby)
        btnJoinLobby.setOnClickListener{ onJoinLobbyClicked()}

        val btnSearchLobbyActivity = view.findViewById<Button>(R.id.btnSearchLobby)
        btnSearchLobbyActivity.setOnClickListener { onSearchLobbyClicked()}

        val btnCreateLobby = view.findViewById<Button>(R.id.btnCreateLobby)
        btnCreateLobby.setOnClickListener{ onCreateLobbyClicked()}

        val filter = IntentFilter(JoinLobbyActivity.JOINING_LOBBY_BROADCAST_FILTER)
        activity?.registerReceiver(joinedLobbyReceiver, filter)

        //create sound player
        setUpSoundPool(context!!)
    }

    private fun cleanPastLobbies() {
        val sharedPref = activity!!.getSharedPreferences(Constants.SHARED_LOBBIES_NAME, Context.MODE_PRIVATE)
        val lobbyIdToDeleteSet = sharedPref.getStringSet("lobbyIds", emptySet())
        /*for (id in lobbyIdToDeleteSet!!){
            Toast.makeText(activity!!, id, Toast.LENGTH_SHORT).show()
        }*/
        if(lobbyIdToDeleteSet != null && lobbyIdToDeleteSet.isNotEmpty()) {
            for (lobbyIdToDelete in lobbyIdToDeleteSet) {
                val dbLobby = FirebaseDatabase.getInstance().getReference("lobbies").child(lobbyIdToDelete)
                dbLobby.child("leader").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.value == FirebaseAuth.getInstance().currentUser!!.uid) {
                            checkIfOverPingTime(dbLobby, sharedPref, lobbyIdToDelete)
                        }else{
                            val myList = sharedPref.getStringSet("lobbyIds", emptySet())!!.toMutableSet()
                            myList.remove(lobbyIdToDelete)
                            val editor = sharedPref.edit()
                            editor.putStringSet("lobbyIds", myList)
                            editor.apply()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun checkIfOverPingTime(dbLobby: DatabaseReference, sharedPref: SharedPreferences, lobbyId:String) {
        dbLobby.child("playersStatus")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value is Long){ //very uncommon
                        dbLobby.removeValue()
                        val myList = sharedPref.getStringSet("lobbyIds", emptySet())!!.toMutableSet()
                        myList.remove(lobbyId)
                        val editor = sharedPref.edit()
                        editor.putStringSet("lobbyIds", myList)
                        editor.apply()
                        return
                    }
                    val timeString = snapshot.getValue(String::class.java) ?: return
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val dateTimeLastLeader = LocalDateTime.parse(timeString, formatter)
                    GlobalScope.launch(Dispatchers.Default){
                        val date = ConnectionHelper.getCurrentTimeFromFirebase()
                        val israelZone: ZoneId = ZoneId.of("Asia/Jerusalem")
                        val localDateTime = LocalDateTime.ofInstant(date.toInstant(), israelZone)
                        val difference = (localDateTime.toEpochSecond(ZoneOffset.UTC) - dateTimeLastLeader.toEpochSecond(
                            ZoneOffset.UTC))
                        if(difference > Constants.PING_INTERVAL_CHECK + 5) {
                            dbLobby.removeValue()
                            val myList = sharedPref.getStringSet("lobbyIds", emptySet())!!.toMutableSet()
                            myList.remove(lobbyId)
                            val editor = sharedPref.edit()
                            editor.putStringSet("lobbyIds", myList)
                            editor.apply()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun onJoinLobbyClicked() {
        startActivity(Intent(activity, JoinLobbyActivity::class.java))
        soundPool.play(clickSoundId!!, 1F, 1F,0,0, 1F)
    }
    private val joinedLobbyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // joined lobby
            activity?.finish()
        }
    }

    private fun onSearchLobbyClicked(){
        startActivity(Intent(activity, SearchLobbyActivity::class.java))
        soundPool.play(clickSoundId!!, 1F, 1F,0,0, 1F)
    }

    private fun onCreateLobbyClicked() {
        soundPool.play(clickSoundId!!, 1F, 1F,0,0, 1F)
        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("lobbies")
        val lobbyId = UUID.randomUUID().toString().substring(0,4) //unique id of the lobby and join code

        //creates new lobby in db and sets leaderId
        databaseLobbies.child(lobbyId).child("leader")
            .setValue(FirebaseAuth.getInstance()!!.currentUser!!.uid)
        val intent = Intent(activity, CreateLobbyActivity::class.java)
        intent.putExtra("lobbyId", lobbyId)
        startActivity(intent)
        activity!!.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.unregisterReceiver(joinedLobbyReceiver)
    }
}