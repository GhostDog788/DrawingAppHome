package il.ghostdog.drawingapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.random.Random


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), ILobbyUser {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog? = null

    private var lobbyId: String? = null

    override var pingTimerJob: Job? = null
    override var checkPingTimerJob: Job? = null
    override var mPingInterval: Int = Constants.PING_INTERVAL
    override var mCheckPingInterval: Int = Constants.PING_INTERVAL_CHECK
    override var sharedPref: SharedPreferences? = null
    override var databaseMyLobby : DatabaseReference? = null
    override var partyLeader : String? = null

    private var mDatabaseInstance: FirebaseDatabase? = null
    private var mPathDatabase: DatabaseReference? = null
    private var mChatDatabase: DatabaseReference? = null
    private var mflDrawingView: FrameLayout? = null
    private var count : Int = 0
    private var dbPathsCount : Int = 0
    private var mAuth : FirebaseAuth? = null
    private var mDrawerUid: String? = null
    private var mGuessWord: String? = null
    private lateinit var mLanguage: String
    private var mPlayersMap: LinkedHashMap<String, PlayerData> = LinkedHashMap()
    private var mPlayerRDataList: ArrayList<PlayerRGameViewData> = ArrayList()
    private var mChatRDataList: ArrayList<GuessMessageRData> = ArrayList()
    private var mHaveNotDrawnList: ArrayList<String> = ArrayList()
    private var mHaveNotGuessedList: ArrayList<String> = ArrayList()
    private lateinit var rvPlayers: RecyclerView
    private lateinit var rvChat: RecyclerView
    private lateinit var llGuessField: LinearLayout
    private lateinit var vgDrawersTools: Group
    private lateinit var tvGuessWord: TextView
    private lateinit var tvRounds: TextView
    private lateinit var tvTurnTimer: TextView
    private lateinit var etGuessField: EditText
    private var mRounds: Int = -1
    private var mCurrentRound: Int = 1
    private var mTurnTime: Int = -1
    private var mTimeLeft: Int = -1
    private var mTurnTimerJob: Job? = null
    private var mCanGuess: Boolean = false

    private val pathsChildListener = object  : ChildEventListener{
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            if(dbPathsCount > drawingView!!.mPaths.size) {
                val encoded = snapshot.getValue(String::class.java) ?: return
                val data = unGzip(encoded)
                val path: DrawingView.StandardPath = Json.decodeFromString(data)
                val customPath = DrawingView.CustomPath(path)
                drawingView!!.mPaths.add(customPath)

                drawingView!!.invalidate()
            }
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            if(drawingView!!.mPaths.isEmpty() || snapshot.key!!.toInt() >= drawingView!!.mPaths.size) return

            val encoded = snapshot.getValue(String::class.java) ?: return
            val data = unGzip(encoded)
            val path: DrawingView.StandardPath = Json.decodeFromString(data)
            val customPath = DrawingView.CustomPath(path)
            drawingView!!.mPaths[snapshot.key!!.toInt()] = customPath

            drawingView!!.invalidate()
        }
        override fun onChildRemoved(snapshot: DataSnapshot) {
            if(drawingView!!.mPaths.isEmpty() || snapshot.key!!.toInt() >= drawingView!!.mPaths.size) return

            if(dbPathsCount < drawingView!!.mPaths.size) {
                drawingView!!.mPaths.removeAt(snapshot.key!!.toInt())

                drawingView!!.invalidate()
            }
        }
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }
    private val drawerIdListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            mDrawerUid = snapshot.getValue(String::class.java)
            if(mDrawerUid == null) return

            mHaveNotDrawnList.remove(mDrawerUid)
            mHaveNotGuessedList.remove(mDrawerUid)

            var i = 0
            while(i < mPlayerRDataList.size){
                mPlayerRDataList[i].isDrawer = mPlayerRDataList[i].userId == mDrawerUid
                rvPlayers.adapter!!.notifyItemChanged(i)
                i++
            }

            if(mAuth!!.currentUser!!.uid == mDrawerUid){
                setUpDrawer()
            }else{
                setUpGuesser()
            }
        }

        override fun onCancelled(error: DatabaseError) {}
    }
    private val playersListener = object : ChildEventListener{
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            val playerData = snapshot.getValue(PlayerData::class.java)!!
            mPlayersMap[snapshot.key!!] = playerData

            if(playerData.answeredCorrectly){
                mHaveNotGuessedList.remove(snapshot.key)
                if(mHaveNotGuessedList.isEmpty()){
                    nextTurn()
                }
            }

            val index = mPlayerRDataList.indexOf(mPlayerRDataList.find
            { playerRGameViewData ->  playerRGameViewData.userId == snapshot.key!!})
            mPlayerRDataList[index] = PlayerRGameViewData(snapshot.key!!, playerData, snapshot.key == mDrawerUid)
            rvPlayers.adapter!!.notifyItemChanged(index)
        }

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val playerData = snapshot.getValue(PlayerData::class.java)!!
            mPlayersMap[snapshot.key!!] = playerData
            mHaveNotDrawnList.add(snapshot.key!!)
            mHaveNotGuessedList.add(snapshot.key!!)

            databaseMyLobby!!.child("currentRound").addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Int::class.java) == null) return
                    mCurrentRound = snapshot.getValue(Int::class.java)!!
                    updateRoundsDisplay()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            mPlayerRDataList.add(PlayerRGameViewData(snapshot.key!!, playerData, snapshot.key == mDrawerUid))
            val index: Int
            if(snapshot.key == partyLeader){
                index = 0
                val temp = mPlayerRDataList[0]
                mPlayerRDataList[0] = mPlayerRDataList[mPlayerRDataList.size - 1]
                mPlayerRDataList[mPlayerRDataList.size - 1] = temp
            }else{
                index = mPlayerRDataList.size - 1
            }
            rvPlayers.adapter!!.notifyItemInserted(index)
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            mPlayersMap.remove(snapshot.key)
            mHaveNotDrawnList.remove(snapshot.key!!)
            mHaveNotGuessedList.remove(snapshot.key!!)

            if (snapshot.key == partyLeader){
                val newLeader = mPlayersMap.keys.first()
                databaseMyLobby!!.child("leader")
                    .setValue(newLeader)
                if(mAuth!!.currentUser!!.uid == newLeader) {
                    databaseMyLobby!!.child("turnTimeLeft")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                mTurnTimerJob =
                                    startDrawingTimer(snapshot.getValue(Int::class.java)!!)
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            var index = 0
            while(index < mPlayerRDataList.size) {
                if(mPlayerRDataList[index].userId == snapshot.key!!){
                    break
                }
                index++
            }
            mPlayerRDataList.removeAt(index)
            rvPlayers.adapter!!.notifyItemRemoved(index)

            if(mPlayersMap.keys.count() < 2){
                mTurnTimerJob?.cancel()
                val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
                alertDialogBuilder.setCancelable(false)
                alertDialogBuilder.setTitle("All players had exit")
                alertDialogBuilder.setMessage("You are the only one left")
                alertDialogBuilder.setPositiveButton("Exit") { dialog, _ ->
                    ConnectionHelper.disconnectPlayerFromLobby(databaseMyLobby!!
                        ,mAuth!!.currentUser!!.uid)
                    removeAllListeners()
                    val intent = Intent()
                    intent.setClass(this@MainActivity, MainMenuActivity::class.java)
                    startActivity(intent)
                    finish()
                    dialog.dismiss()
                }
                alertDialogBuilder.show()
                return
            }

            if(snapshot.key == mDrawerUid || mHaveNotGuessedList.isEmpty()) {
                nextTurn()
            }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            TODO("Not yet implemented")
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }
    private val pathCountListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            dbPathsCount = snapshot.getValue(Long::class.java)!!.toInt()

            if(dbPathsCount == 0){
                drawingView!!.mPaths.clear()
                drawingView!!.invalidate()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }
    private val turnTimerListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.getValue(Int::class.java) == null) return
            mTimeLeft = snapshot.getValue(Int::class.java)!!

            tvTurnTimer.text = mTimeLeft.toString()

            if(mTimeLeft == 0){
                nextTurn()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }
    private val guessWordListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            mGuessWord = snapshot.getValue(String::class.java)
            if(mGuessWord != null){ // means set drawerId
                if(mDrawerUid == mAuth!!.currentUser!!.uid){
                    tvGuessWord.text = mGuessWord
                }else{
                    var str = ""
                    for (char in mGuessWord!!){
                        if(char != ' '){
                            str += "_ "
                        }else{
                            str += " "
                        }
                    }
                    tvGuessWord.text = str
                }
                clearChat()
                drawingView!!.clear()
                if(mAuth!!.currentUser!!.uid == mDrawerUid){
                    drawingView!!.canDraw = true
                }
                mCanGuess = true
                if(mAuth!!.currentUser!!.uid == partyLeader){
                    mTurnTimerJob = startDrawingTimer()
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }
    private val guessChatListener = object : ChildEventListener{
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val messageData = snapshot.getValue(GuessMessageRData::class.java)!!

            mChatRDataList.add(messageData)
            rvChat.adapter!!.notifyItemInserted(mChatRDataList.size)
            rvChat.scrollToPosition(mChatRDataList.size - 1)
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            //no need
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            TODO("Not yet implemented")
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            TODO("Not yet implemented")
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
    }
    private val partyLeaderListener = object: ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            partyLeader = dataSnapshot.getValue(String::class.java)!! //had to be initialized
            if(mAuth!!.currentUser!!.uid == partyLeader){
                val editor = sharedPref?.edit()
                editor?.putString("lobbyId", lobbyId)
                editor?.apply()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            // An error occurred
            Toast.makeText(applicationContext, "Error in setting up party leader", Toast.LENGTH_SHORT).show()
        }
    }

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
                if (result.resultCode == RESULT_OK && result.data != null){
                    val imageBackground: ImageView = findViewById(R.id.ivBackground)
                    imageBackground.setImageURI(result.data?.data)
                }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
                permissions.entries.forEach{
                    val permissionName = it.key
                    val isGranted =it.value

                    if(isGranted){
                        if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                            Toast.makeText(
                                this,
                                "Permission granted to read files",
                                Toast.LENGTH_SHORT
                            ).show()
                            val pickIntent = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            openGalleryLauncher.launch(pickIntent)
                        }
                    }else{
                        if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = applicationContext.getSharedPreferences(Constants.SHARED_LOBBIES_NAME, Context.MODE_PRIVATE)

        lobbyId = intent.getStringExtra("lobbyId")
        val reEntering = intent.getBooleanExtra("reEntering", false)
        mLanguage = intent.getStringExtra("language")!!
        mRounds = intent.getIntExtra("rounds", GamePreferences().rounds)
        mTurnTime = intent.getIntExtra("turnTime", GamePreferences().turnTime)

        drawingView = findViewById(R.id.dvDrawingView)
        vgDrawersTools = findViewById(R.id.drawersTools)
        llGuessField = findViewById(R.id.llGuessField)
        tvGuessWord = findViewById(R.id.tvGuessWord)
        tvRounds = findViewById(R.id.tvRounds)
        tvTurnTimer = findViewById(R.id.tvTimer)
        etGuessField = findViewById(R.id.etGuessField)
        rvPlayers = findViewById(R.id.rvPlayers)
        rvPlayers.adapter = PlayerGameHUDAdapter(mPlayerRDataList)
        rvPlayers.layoutManager = GridLayoutManager(this, 1)

        rvChat = findViewById(R.id.rvChat)
        rvChat.adapter = GuessChatAdapter(mChatRDataList)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        rvChat.layoutManager = linearLayoutManager


        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val widthR = displayMetrics.widthPixels
        (drawingView!!.parent as View).updateLayoutParams{
            height = (widthR * 1.2).toInt()
            width = (widthR - widthR * 0.02).toInt()
            Constants.viewWidth = width
        }

        drawingView?.setSizeForBrush(12.toFloat())

        mAuth = FirebaseAuth.getInstance()
        drawingView?.mOnDrawChange!!.plusAssign(::handleDrawChange)

        mDatabaseInstance = FirebaseDatabase.getInstance()
        databaseMyLobby = mDatabaseInstance!!.getReference("lobbies").child(lobbyId!!)
        mPathDatabase = databaseMyLobby!!.child("paths")
        mChatDatabase = databaseMyLobby!!.child("guessChat")
        mflDrawingView = findViewById(R.id.flDrawingViewContainer)

        lifecycleScope.launch {
            addPlayersListener()
            if(reEntering){
                delay(500L)
            }
            addDrawerIdListener()
            addGuessWordListener()
            addGuessChatListener()
            addTurnTimerListener()
            addPartyLeaderListener()
            if(!reEntering) {
                setupGame() //have to be before listeners
            }else{
                rejoinGame()
            }
            addPathsValueListener()
            addPathsCountListener()
        }


        etGuessField.setOnEditorActionListener {view, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                keyEvent == null ||
                keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                //User finished typing
                onSubmitGuess()
                true
            }
            false
        }

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.llPaintColors)
        mImageButtonCurrentPaint = linearLayoutPaintColors.findViewWithTag("#ff000000") as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))

        val ibBrush: ImageButton = findViewById(R.id.ibBrush)
        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }
        val ibGallery: ImageButton = findViewById(R.id.ibGallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }
        val ibUndo: ImageButton = findViewById(R.id.ibUndo)
        ibUndo.setOnClickListener {
            if (mAuth!!.currentUser!!.uid == mDrawerUid){
                drawingView?.onClickUndo()
            }
        }
        val ibSave: ImageButton = findViewById(R.id.ibSave)
        ibSave.setOnClickListener{
            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    saveBitmapFile(getBitmapFromView(mflDrawingView!!))
                }
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

    private fun onSubmitGuess() {
        val guess = etGuessField.text.toString()
        etGuessField.text.clear()

        if (guess == mGuessWord){
            val playerData = mPlayersMap[mAuth!!.currentUser!!.uid]
            if(playerData!!.answeredCorrectly || !mCanGuess) return

            playerData.answeredCorrectly = true
            playerData.points += 100
            updatePlayerData(mAuth!!.currentUser!!.uid, playerData)
        }else{
            val name = mPlayersMap[mAuth!!.currentUser!!.uid]!!.name
            val guessMessageRData = GuessMessageRData(name, guess)
            mChatDatabase!!.push().setValue(guessMessageRData) }
    }

    private suspend fun setupGame() {
        mPathDatabase!!.setValue("")
        databaseMyLobby!!.child("pathsCount").setValue(0)
        clearChat()
        databaseMyLobby!!.child("currentRound").setValue(1)
        tvTurnTimer.text = mTurnTime.toString()

        withContext(Dispatchers.IO){
            while(partyLeader == null)
            {
                awaitFrame()
            }
        }

        //set name in tvUserName
        findViewById<TextView>(R.id.tvUserName).text = mPlayersMap[mAuth!!.currentUser!!.uid]!!.name

        nextTurn()
    }

    private suspend fun rejoinGame() {
        withContext(Dispatchers.IO){
            while(!mPlayersMap.contains(mAuth!!.currentUser!!.uid))
            {
                awaitFrame()
            }
        }
        //set name in tvUserName
        findViewById<TextView>(R.id.tvUserName).text = mPlayersMap[mAuth!!.currentUser!!.uid]!!.name

        //get past paths
        //databaseMyLobby!!.child(p)
    }

    private fun nextTurn() {
        mCanGuess = false
        drawingView!!.canDraw = false //takes the ability from all players to draw until new drawer is set
        drawingView!!.clear()

        mTurnTimerJob?.cancel() //cancels the timer if exists
        if (mHaveNotDrawnList.isEmpty()){
            mCurrentRound++
            databaseMyLobby!!.child("currentRound").setValue(1)
            updateRoundsDisplay()
            if(mCurrentRound > mRounds){
                if(mAuth!!.currentUser!!.uid == partyLeader) {
                    databaseMyLobby!!.child("gamePreferences")
                        .child("status").setValue(GameStatus.ended)
                }
                endGame()
                return
            }
            for (key in mPlayersMap.keys){
                mHaveNotDrawnList.add(key)
            }
        }

        if(mAuth!!.currentUser!!.uid == partyLeader) {
            mPathDatabase!!.setValue("")
            databaseMyLobby!!.child("pathsCount").setValue(0)

            val selectedPlayerKey = mHaveNotDrawnList[0]
            databaseMyLobby!!.child("drawerID").setValue(selectedPlayerKey)
        }

        mHaveNotGuessedList.clear()
        for (playerId in mPlayersMap.keys) {
            mHaveNotGuessedList.add(playerId)
        }
    }

    private fun endGame() {
        val intent = Intent()
        intent.putExtra("players", mPlayersMap)
        intent.putExtra("lobbyId", lobbyId)
        intent.putExtra("partyLeader", partyLeader)

        removeAllListeners()

        intent.setClass(this@MainActivity, EndGameActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun removeAllListeners() {
        removeDrawerIdListener()
        removePathsValueListener()
        removePlayersListener()
        removeGuessChatListener()
        removeGuessWordListener()
        removeTurnTimerListener()
        removePathsCountListener()
        removePartyLeaderListener()
    }

    private fun chooseWord() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.word_chooser_dialog)
        dialog.setTitle("Custom Dialog")
        dialog.setCancelable(false)
        val firstButton = dialog.findViewById<Button>(R.id.btnOne)
        val secondButton = dialog.findViewById<Button>(R.id.btnTwo)
        val thirdButton = dialog.findViewById<Button>(R.id.btnThree)

        val usedIndexes = ArrayList<Int>()
        usedIndexes.add(Constants.GUESS_WORDS_MAP[mLanguage]!!.indexOf(mGuessWord))
        //prevents from getting the same word
        var index = 0
        do {
            index = Random.nextInt(0, Constants.GUESS_WORDS_MAP[mLanguage]!!.size)
        }while(usedIndexes.contains(index))
        firstButton.text = Constants.GUESS_WORDS_MAP[mLanguage]!![index]
        usedIndexes.add(index)
        do {
            index = Random.nextInt(0, Constants.GUESS_WORDS_MAP[mLanguage]!!.size)
        }while(usedIndexes.contains(index))
        secondButton.text = Constants.GUESS_WORDS_MAP[mLanguage]!![index]
        usedIndexes.add(index)
        do {
            index = Random.nextInt(0, Constants.GUESS_WORDS_MAP[mLanguage]!!.size)
        }while(usedIndexes.contains(index))
        thirdButton.text = Constants.GUESS_WORDS_MAP[mLanguage]!![index]
        usedIndexes.add(index)

        firstButton.setOnClickListener {
            setGuessWord(((it as Button).text.toString()), dialog)
        }
        secondButton.setOnClickListener {
            setGuessWord(((it as Button).text.toString()), dialog)
        }
        thirdButton.setOnClickListener {
            setGuessWord(((it as Button).text.toString()), dialog)
        }
        if ((this as Activity).isFinishing) {
            Toast.makeText(applicationContext, "Application finished", Toast.LENGTH_SHORT).show()
        }else{
            dialog.show()
        }
    }

    private fun setGuessWord(word: String, dialog: Dialog) {
        databaseMyLobby!!.child("guessWord").setValue(word).addOnCompleteListener{
            dialog.dismiss()
        }
    }

    private fun setUpGuesser() {
        drawingView!!.canDraw = false
        addPathsValueListener()
        vgDrawersTools.visibility = View.GONE
        llGuessField.visibility = View.VISIBLE
        updateRoundsDisplay()


        val playerData = mPlayersMap[mAuth!!.currentUser!!.uid]
        playerData!!.answeredCorrectly = false
        updatePlayerData(mAuth!!.currentUser!!.uid, playerData)
    }

    private fun setUpDrawer() {
        removePathsValueListener()
        vgDrawersTools.visibility = View.VISIBLE
        llGuessField.visibility = View.GONE
        updateRoundsDisplay()

        val playerData = mPlayersMap[mAuth!!.currentUser!!.uid]
        playerData!!.answeredCorrectly = false
        updatePlayerData(mAuth!!.currentUser!!.uid, playerData)

        chooseWord()
    }

    override fun onLeaderDisconnected() {}

    private fun addPathsValueListener() {
        mPathDatabase!!.addChildEventListener(pathsChildListener)
    }
    private fun removePathsValueListener() {
        mPathDatabase!!.removeEventListener(pathsChildListener)
    }

    private fun addGuessWordListener() {
        databaseMyLobby!!.child("guessWord").addValueEventListener(guessWordListener)
    }
    private fun removeGuessWordListener() {
        databaseMyLobby!!.child("guessWord").removeEventListener(guessWordListener)
    }

    private fun addDrawerIdListener() {
        databaseMyLobby!!.child("drawerID").addValueEventListener(drawerIdListener)
    }
    private fun removeDrawerIdListener() {
        databaseMyLobby!!.child("drawerID").removeEventListener(drawerIdListener)
    }

    private fun addPathsCountListener() {
        databaseMyLobby!!.child("pathsCount").addValueEventListener(pathCountListener)
    }
    private fun removePathsCountListener() {
        databaseMyLobby!!.child("pathsCount").removeEventListener(pathCountListener)
    }

    private fun addTurnTimerListener() {
        databaseMyLobby!!.child("turnTimeLeft").addValueEventListener(turnTimerListener)
    }
    private fun removeTurnTimerListener() {
        databaseMyLobby!!.child("turnTimeLeft").removeEventListener(turnTimerListener)
    }

    private fun addPlayersListener() {
        databaseMyLobby!!.child("players").addChildEventListener(playersListener)
    }
    private fun removePlayersListener() {
        databaseMyLobby!!.child("players").removeEventListener(playersListener)
    }
    private fun addGuessChatListener() {
        mChatDatabase!!.addChildEventListener(guessChatListener)
    }
    private fun removeGuessChatListener() {
        mChatDatabase!!.removeEventListener(guessChatListener)
    }
    private fun addPartyLeaderListener() {
        databaseMyLobby!!.child("leader").addValueEventListener(partyLeaderListener)
    }
    private fun removePartyLeaderListener() {
        databaseMyLobby!!.child("leader").removeEventListener(partyLeaderListener)
    }


    private fun updatePlayerData(playerId: String, newData: PlayerData){
        databaseMyLobby!!.child("players").child(playerId)
            .setValue(newData)
    }

    private fun updateRoundsDisplay(){
        tvRounds.text = "$mCurrentRound/$mRounds"
    }

    private fun clearChat(){
        mChatDatabase!!.removeValue()
        mChatRDataList.clear()
        rvChat.adapter!!.notifyDataSetChanged()
    }

    private fun startDrawingTimer(time: Int = mTurnTime): Job {
        mTimeLeft = time
        tvTurnTimer.text = mTimeLeft.toString()
        databaseMyLobby!!.child("turnTimeLeft").setValue(mTimeLeft)
        return lifecycleScope.launch {
            while (isActive && mTimeLeft > 0){ //if 0 than time out
                delay(1000L)
                mTimeLeft--
                databaseMyLobby!!.child("turnTimeLeft").setValue(mTimeLeft)
            }
        }
    }

    private fun handleDrawChange(myPathsCount : Int){
        count++
        databaseMyLobby!!.child("count").setValue(count)


        if(dbPathsCount != myPathsCount){
            databaseMyLobby!!.child("pathsCount").setValue(myPathsCount)
        }

        if(dbPathsCount < myPathsCount){
            var counter = dbPathsCount
            while(counter < myPathsCount){
                val path = drawingView!!.mPaths[counter]
                val standardPath = path.toStandardPath()
                val data = Json.encodeToString(standardPath)
                val compressedData = gzip(data)

                mPathDatabase!!.child(counter.toString()).setValue(compressedData)
                counter++
            }
        }else if(dbPathsCount > myPathsCount){
            var counter = myPathsCount
            while(counter < dbPathsCount) {
                if(myPathsCount == 0){
                    mPathDatabase!!.setValue("")
                }else {
                    mPathDatabase!!.child(counter.toString()).removeValue()
                }
                counter++
            }
        }else{
            val path = drawingView!!.mPaths[myPathsCount - 1]
            val standardPath = path.toStandardPath()
            val data = Json.encodeToString(standardPath)
            val compressedData = gzip(data)


            mPathDatabase!!.child((myPathsCount - 1).toString()).setValue(compressedData)
        }
    }

    private fun gzip(content: String): String {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        val b = bos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    private fun unGzip(content: String): String =
        GZIPInputStream(Base64.decode(content, Base64.DEFAULT).inputStream()).bufferedReader(UTF_8).use { it.readText() }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationalDialog("Permission denied - access storage"
                ,"Can not access storage of device for functions of the device for the application" )
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val button1: ImageView = brushDialog.findViewById(R.id.ibBrush1)
        button1.setOnClickListener{
            drawingView?.setSizeForBrush(3.toFloat())
            brushDialog.dismiss()
        }
        val button2: ImageView = brushDialog.findViewById(R.id.ibBrush2)
        button2.setOnClickListener{
            drawingView?.setSizeForBrush(7.toFloat())
            brushDialog.dismiss()
        }
        val button3: ImageView = brushDialog.findViewById(R.id.ibBrush3)
        button3.setOnClickListener{
            drawingView?.setSizeForBrush(12.toFloat())
            brushDialog.dismiss()
        }
        val button4: ImageView = brushDialog.findViewById(R.id.ibBrush4)
        button4.setOnClickListener{
            drawingView?.setSizeForBrush(21.toFloat())
            brushDialog.dismiss()
        }
        val button5: ImageView = brushDialog.findViewById(R.id.ibBrush5)
        button5.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view != mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_selected))
            mImageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            mImageButtonCurrentPaint = view
        }
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private fun showRationalDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancel"){ dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawingApp" + System.currentTimeMillis() / 1000 + ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath
                    
                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved successfully: $result", Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
                        }
                    }
                }catch(e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

}