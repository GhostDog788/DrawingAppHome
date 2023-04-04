package il.ghostdog.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class SearchLobbyActivity : AppCompatActivity(), LobbySearchAdapter.RecyclerViewEvent {

    private lateinit var rvLobbies: RecyclerView
    private var lobbySearchList = ArrayList<LobbySearchRViewData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_lobby)

        val spinnerAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.searchByOptions,
            R.layout.search_by_spinner_item
        )
        spinnerAdapter.setDropDownViewResource(R.layout.search_by_spinner_dropdown_item)
        val spSortBy = findViewById<Spinner>(R.id.spSortBy)
        spSortBy.adapter = spinnerAdapter


        rvLobbies = findViewById(R.id.rvLobbies)
        rvLobbies.adapter = LobbySearchAdapter(lobbySearchList, this)
        rvLobbies.layoutManager = LinearLayoutManager(this)
        setLobbySearchView()
    }

    private fun setLobbySearchView(){
        FirebaseDatabase.getInstance().getReference("lobbies").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (lobbySp in snapshot.children){
                    if(!lobbySp.hasChild("gamePreferences")) continue
                    val gamePreferences: GamePreferences = lobbySp.child("gamePreferences").getValue(GamePreferences::class.java)!!
                    if (!gamePreferences.public) continue
                    if(!lobbySp.hasChild("leader")) continue
                    if(!lobbySp.hasChild("players")) continue
                    if(lobbySp.child("players").childrenCount < 1) continue
                    if(!lobbySp.hasChild("playersStatus")) continue
                    val leader = lobbySp.child("leader").getValue(String::class.java)!!
                    val timeString = lobbySp.child("playersStatus").child(leader).getValue(String::class.java)!!
                    CoroutineScope(Dispatchers.Default).launch {
                        val difference = getDifferenceInSeconds(timeString)
                        if (difference > 180) {
                            cancel()
                            yield()
                        }
                        FirebaseDatabase.getInstance().getReference("users").child(leader)
                            .addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val userData = snapshot.getValue(UserData::class.java)
                                    val leaderName = userData!!.nickname
                                    makeLobbyViewFromSp(lobbySp!!, leaderName)
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun makeLobbyViewFromSp(lobbySp: DataSnapshot, leaderName: String) {
        val playerCount: Int = lobbySp.child("players").childrenCount.toInt()
        val gamePreferences: GamePreferences = lobbySp.child("gamePreferences").getValue(GamePreferences::class.java)!!
        var currentRound: Int = 0
        if(gamePreferences.status == GameStatus.active){
            currentRound = lobbySp.child("currentRound").getValue(Int::class.java)!!
        }
        val lobbySearchRViewData = LobbySearchRViewData(
            lobbySp.key!!,
            leaderName,
            playerCount, gamePreferences.status, currentRound, gamePreferences.rounds)
        lobbySearchList.add(lobbySearchRViewData)
        rvLobbies.adapter!!.notifyItemInserted(lobbySearchList.lastIndex)
        Toast.makeText(applicationContext, "added", Toast.LENGTH_SHORT).show()
    }

    private suspend fun getDifferenceInSeconds(timeString: String): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateTimeLastLeader = LocalDateTime.parse(timeString, formatter)
        val date = getCurrentTimeFromFirebase()
        val israelZone: ZoneId = ZoneId.of("Asia/Jerusalem")
        val localDateTime = LocalDateTime.ofInstant(date.toInstant(), israelZone)
        return (localDateTime.toEpochSecond(ZoneOffset.UTC) - dateTimeLastLeader.toEpochSecond(
            ZoneOffset.UTC
        ))
    }
    private suspend fun getCurrentTimeFromFirebase() : Date {
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

    override fun onItemClicked(position: Int) {
        val myLobbyData = lobbySearchList[position]
        val intent = Intent(this, JoinLobbyActivity::class.java)
        intent.putExtra("lobbyId", myLobbyData.lobbyId)
        startActivity(intent)
    }
}