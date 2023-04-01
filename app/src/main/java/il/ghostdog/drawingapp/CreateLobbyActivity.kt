package il.ghostdog.drawingapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class CreateLobbyActivity : AppCompatActivity(), ILobbyUser, IProgressDialogUser, PlayerRecyclerAdapter.RecyclerViewEvent {

    var lobbyId: String? = null

    override var customProgressDialog: Dialog? = null

    private var mAuth : FirebaseAuth? = null

    private var databaseUsers : DatabaseReference? = null

    override var pingTimerJob: Job? = null
    override var checkPingTimerJob: Job? = null
    override var mPingInterval: Int = Constants.PING_INTERVAL
    override var mCheckPingInterval: Int = Constants.PING_INTERVAL_CHECK
    override var sharedPref: SharedPreferences? = null
    override var databaseMyLobby : DatabaseReference? = null
    override var partyLeader : String? = null

    private lateinit var rvPlayers: RecyclerView
    private var playerRViewDataList : ArrayList<PlayerRViewData> = ArrayList()

    private lateinit var tvRounds: TextView
    private lateinit var tvTime: TextView
    private var minRounds: Int = 2
    private var maxRounds: Int = 9
    private var minTime: Int = 15
    private var maxTime: Int = 150
    private var timeJumps: Int = 15

    private var gamePreferences: GamePreferences = GamePreferences()

    var playersMap: LinkedHashMap<String, PlayerData> = LinkedHashMap()

    private val myFriendsRViewDataMap = mutableMapOf<String,FriendRViewData>()

    private val playersChildListener = object : ChildEventListener{
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            //some time called for some reason
        }
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val playerData = snapshot.getValue(PlayerData::class.java)!!
            playersMap[snapshot.key!!] = playerData
            playerRViewDataList.add(PlayerRViewData(snapshot.key!!, playerData,null, snapshot.key == partyLeader))
            val index: Int
            if(snapshot.key == partyLeader){
                index = 0
                val temp = playerRViewDataList[0]
                playerRViewDataList[0] = playerRViewDataList[playerRViewDataList.size - 1]
                playerRViewDataList[playerRViewDataList.size - 1] = temp
            }else{
                index = playerRViewDataList.size - 1
            }
            rvPlayers.adapter!!.notifyItemInserted(index)
        }
        override fun onChildRemoved(snapshot: DataSnapshot) {
            if(snapshot.key == mAuth!!.currentUser!!.uid){
                //player have been kicked or exit
                removeAllListeners()
                startActivity(Intent(this@CreateLobbyActivity, MainMenuActivity::class.java))
                finish()
            }

            playersMap.remove(snapshot.key)
            var index = 0
            while(index < playerRViewDataList.size) {
                if(playerRViewDataList[index].userId == snapshot.key!!){
                    break
                }
                index++
            }
            playerRViewDataList.removeAt(index)
            rvPlayers.adapter!!.notifyItemRemoved(index)

            if(snapshot.key == partyLeader){
                onLeaderDisconnected()
            }
        }
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    private val leaderListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            partyLeader = snapshot.getValue(String::class.java)
            if(partyLeader == mAuth!!.currentUser!!.uid){
                setUpLeader()
            }
        }
        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }
    private val gamesPreferencesListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            val data = snapshot.getValue(GamePreferences::class.java) ?: return
            gamePreferences = data

            if (gamePreferences.status == GameStatus.active){
                onStartGame()
            }
        }
        override fun onCancelled(error: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_lobby)

        sharedPref = applicationContext.getSharedPreferences(Constants.SHARED_LOBBIES_NAME, Context.MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()
        lobbyId = intent.getStringExtra("lobbyId")

        val dataBaseInstance = FirebaseDatabase.getInstance()
        val databaseLobbies = dataBaseInstance.getReference("lobbies")
        databaseUsers = dataBaseInstance.getReference("users")
        databaseMyLobby = databaseLobbies.child(lobbyId!!)

        databaseUsers!!.child(mAuth!!.currentUser!!.uid).child("activeGame").setValue(lobbyId!!)

        //set lobby id display
        findViewById<TextView>(R.id.tvLobbyId).text = lobbyId

        tvRounds = findViewById(R.id.tvRounds)
        findViewById<Button>(R.id.btnMinusRounds)
            .setOnClickListener{ onAdditiveButtonClicked(minRounds, maxRounds, -1, tvRounds)}
        findViewById<Button>(R.id.btnPlusRounds)
            .setOnClickListener{ onAdditiveButtonClicked(minRounds, maxRounds, 1, tvRounds)}

        tvTime = findViewById(R.id.tvTime)
        findViewById<Button>(R.id.btnMinusTime)
            .setOnClickListener{ onAdditiveButtonClicked(minTime, maxTime, -timeJumps, tvTime)}
        findViewById<Button>(R.id.btnPlusTime)
            .setOnClickListener{ onAdditiveButtonClicked(minTime, maxTime, timeJumps, tvTime)}

        val btnExit = findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {exitLobby()}

        val btnInviteFriend = findViewById<Button>(R.id.btnInviteFriend)
        btnInviteFriend.setOnClickListener{ showFriendsDialog()}

        rvPlayers = findViewById(R.id.rvPlayers)
        rvPlayers.adapter = PlayerRecyclerAdapter(playerRViewDataList, this)
        rvPlayers.layoutManager = GridLayoutManager(this@CreateLobbyActivity, 2)

        lifecycleScope.launch {
            setUpLobby()
        }
    }

    private fun showFriendsDialog(){
        val myUid = FirebaseAuth.getInstance().currentUser!!.uid
        val myFriendsIdList = ArrayList<String>()

        FirebaseDatabase.getInstance().getReference("users")
            .child(myUid).child("friendsList").addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children) {
                        val potentialFriend = ds.getValue(String::class.java)
                        if (potentialFriend != null && !potentialFriend.startsWith("request-")) {
                            myFriendsIdList.add(potentialFriend)
                        }
                    }
                    for(friendId in myFriendsIdList){
                        if(myFriendsRViewDataMap.contains(friendId)) continue

                        FirebaseDatabase.getInstance().getReference("users")
                            .child(friendId).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.value == null) return
                                    val userData = snapshot.getValue(UserData::class.java)!!
                                    val friendRViewData = FriendRViewData(friendId, userData, null)
                                    myFriendsRViewDataMap[friendId] = friendRViewData
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                    CoroutineScope(Dispatchers.Default).launch {
                        while (myFriendsRViewDataMap.size < myFriendsIdList.size) {
                            delay(50)
                        }
                    }.invokeOnCompletion {
                        runOnUiThread {
                            val arrayList = ArrayList<FriendRViewData>(myFriendsRViewDataMap.values)
                            val dialogFragment = FriendsDialog(arrayList)
                            dialogFragment.show(supportFragmentManager, "FriendsDialogFragment")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun onStartGame() {
        if(playersMap.size < 2){
            Toast.makeText(applicationContext, "Need at least two players", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, MainActivity::class.java)
        if(mAuth!!.currentUser!!.uid == partyLeader) {
            updateGamePreferences()
            databaseMyLobby!!.child("gamePreferences").setValue(gamePreferences)
        }
        intent.putExtra("lobbyId", lobbyId)
        intent.putExtra("language", gamePreferences.language)
        intent.putExtra("rounds", gamePreferences.rounds)
        intent.putExtra("turnTime", gamePreferences.turnTime)
        removeAllListeners()
        startActivity(intent)
        finish()
    }

    private fun updateGamePreferences() {
        gamePreferences.status = GameStatus.active
        when (findViewById<RadioGroup>(R.id.rgLanguage).checkedRadioButtonId) {
            R.id.rbEnglish -> {
                gamePreferences.language = "english"
            }
            R.id.rbHebrew -> {
                gamePreferences.language = "hebrew"
            }
        }
        gamePreferences.rounds = findViewById<TextView>(R.id.tvRounds).text.toString().toInt()
        gamePreferences.turnTime = findViewById<TextView>(R.id.tvTime).text.toString().toInt()
    }

    private suspend fun setUpLobby(){
        showProgressDialog(this@CreateLobbyActivity)
        addLeaderListener()
        withContext(Dispatchers.IO){
            while(partyLeader == null)
            {
                awaitFrame()
            }
        }
        if(partyLeader != mAuth!!.currentUser!!.uid){
            setUpPlayer()
        }
        databaseUsers!!.child(mAuth!!.currentUser!!.uid).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserData::class.java)
                databaseMyLobby!!.child("players").child(mAuth!!.currentUser!!.uid)
                    .setValue(PlayerData(user!!.nickname))
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        //calls to all players at start + new ones
        addPlayerChildListener()

        cancelProgressDialog()
    }

    private fun setUpPlayer() {
        databaseMyLobby!!.child("leader").addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                partyLeader = dataSnapshot.getValue(String::class.java)!!
            }

            override fun onCancelled(error: DatabaseError) {
                // An error occurred
                Toast.makeText(applicationContext, "Error in setting up party leader", Toast.LENGTH_SHORT).show()
            }
        })

        addGamePreferencesListener()
    }

    private fun setUpLeader() {
        gamePreferences.status = GameStatus.preparing
        databaseMyLobby!!.child("gamePreferences").setValue(gamePreferences)

        val myList = sharedPref?.getStringSet("lobbyIds", emptySet())!!.toMutableSet()
        myList.add(lobbyId)
        val editor = sharedPref?.edit()
        editor?.putStringSet("lobbyIds", myList)
        editor?.apply()

        findViewById<LinearLayout>(R.id.llGamePreferences).visibility = View.VISIBLE
        tvRounds.text = gamePreferences.rounds.toString()
        tvTime.text = gamePreferences.turnTime.toString()

        val btnCreate = findViewById<Button>(R.id.btnStartGame)
        btnCreate.visibility = View.VISIBLE
        btnCreate.setOnClickListener{ onStartGame()}
    }

    private fun addPlayerChildListener(){
        databaseMyLobby!!.child("players").addChildEventListener(playersChildListener)
    }
    private fun removePlayerChildListener(){
        databaseMyLobby!!.child("players").removeEventListener(playersChildListener)
    }
    private fun addGamePreferencesListener() {
        databaseMyLobby!!.child("gamePreferences")
            .addValueEventListener(gamesPreferencesListener)
    }
    private fun removeGamePreferencesListener() {
        databaseMyLobby!!.child("gamePreferences")
            .removeEventListener(gamesPreferencesListener)
    }
    private fun addLeaderListener() {
        databaseMyLobby!!.child("leader").addValueEventListener(leaderListener)
    }
    private fun removeLeaderListener() {
        databaseMyLobby!!.child("leader").removeEventListener(leaderListener)
    }
    private fun removeAllListeners(){
        removePlayerChildListener()
        removeLeaderListener()
        removeGamePreferencesListener()
    }

    override fun onItemClickedLong(position: Int) {
        val player = playerRViewDataList[position]

        if(player.isLeader || mAuth!!.currentUser!!.uid != partyLeader) return

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Kick player")
        alertDialogBuilder.setMessage("Do you want to kick ${player.name} from the lobby?")
        alertDialogBuilder.setPositiveButton("Kick") { dialog, _ ->
            kickPlayer(player)
            dialog.dismiss()
        }
        alertDialogBuilder.setNegativeButton("No"){ dialog, _ ->
            dialog.dismiss()
        }
        alertDialogBuilder.show()
    }

    override fun onItemClickedShort(position: Int) {
        val player = playerRViewDataList[position]
        if(player.userId == mAuth!!.currentUser!!.uid) return
        val viewHolder = rvPlayers.findViewHolderForAdapterPosition(position)!!
        val popupMenu = PopupMenu(this, viewHolder.itemView)
        popupMenu.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.action_request_friend ->{
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(player.userId).addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val userData = snapshot.getValue(UserData::class.java)!!
                                checkIfAllReadyAdded(player.userId, userData)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                    true
                }else ->{
                    false
                }
            }
        }
        popupMenu.inflate(R.menu.popup_friend_request_menu)
        popupMenu.show()
    }
    private fun checkIfAllReadyAdded(testId: String, userData: UserData){
        val myUid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("users")
            .child(testId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val friendList = snapshot.getValue(UserData::class.java)!!.friendsList
                    for (item in friendList){
                        if(item == myUid){
                            Toast.makeText(this@CreateLobbyActivity, "This user has all ready been added as a friend", Toast.LENGTH_SHORT).show()
                            return
                        }
                        else if(item.contains(myUid)){
                            Toast.makeText(this@CreateLobbyActivity, "You have all ready sent a friend request", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                    //doesn't contains
                    makeRequestDialog(testId, userData)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun makeRequestDialog(userId: String, userData: UserData) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.send_friend_request_dialog)
        dialog.setCancelable(true)
        dialog.findViewById<TextView>(R.id.tvName).text = userData.nickname
        dialog.findViewById<Button>(R.id.btnRequest).setOnClickListener{
            userData.friendsList.add("request-${FirebaseAuth.getInstance().currentUser!!.uid}")
            FirebaseDatabase.getInstance().getReference("users").child(userId).setValue(userData)
            Toast.makeText(this, "The request has been sent to ${userData.nickname}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener{
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun kickPlayer(player: PlayerRViewData) {
        ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, player.userId)
    }
    private fun exitLobby(){
        ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!, mAuth!!.currentUser!!.uid)
    }

    private fun onAdditiveButtonClicked(minRange: Int, maxRange: Int, amount: Int, display: TextView?) {
        var value = display!!.text.toString().toInt()
        if(value + amount > maxRange || value + amount < minRange) return

        value += amount
        display!!.text = value.toString()
    }

    override fun onLeaderDisconnected() {
        for(key in playersMap.keys){
            if(partyLeader != key){
                if(mAuth!!.currentUser!!.uid != key) return
                databaseMyLobby!!.child("leader")
                    .setValue(mAuth!!.currentUser!!.uid)
                return
            }
        }
    }

    override fun onStop() {
        pingTimerJob?.cancel()
        checkPingTimerJob?.cancel()
        super.onStop()
    }

    override fun onResume() {
        lifecycleScope.launch {
            while (databaseMyLobby == null || partyLeader == null) {
                delay(100)
            }
            pingTimerJob = startPingTimer(lifecycleScope)
            checkPingTimerJob = startPingCheckTimer(lifecycleScope, mAuth!!.currentUser!!.uid)
            updateMyStatus()
        }
        super.onResume()
    }
}