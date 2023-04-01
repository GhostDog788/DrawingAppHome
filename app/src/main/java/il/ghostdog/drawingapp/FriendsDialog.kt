package il.ghostdog.drawingapp

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Duration
import java.time.LocalTime
import java.util.*
import kotlin.collections.ArrayList

class FriendsDialog(friendsList: ArrayList<FriendRViewData>) : DialogFragment(), FriendsRecyclerAdapter.RecyclerViewEvent {
    private lateinit var rvFriends: RecyclerView
    private var friendsList: ArrayList<FriendRViewData>
    private var timer: Timer? = null
    private val lastSeenMap = mutableMapOf<String, LocalTime>()

    init {
        this.friendsList = friendsList
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity!!)
        dialog.setContentView(R.layout.friends_dialog)
        rvFriends = dialog.findViewById(R.id.rvFriends)
        rvFriends.adapter = FriendsRecyclerAdapter(friendsList, this)
        rvFriends.layoutManager = LinearLayoutManager(activity!!)

        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                for (i in 0 until rvFriends.childCount) {
                    val viewHolder = rvFriends.findViewHolderForAdapterPosition(i) as FriendsRecyclerAdapter.ItemViewHolder
                    val myFriendData = friendsList[i]
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(myFriendData.userId).child("lastSeen")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val timeString = snapshot.getValue(String::class.java)!!
                                myFriendData.lastSeen = timeString
                                viewHolder.calculateTimeDifAndUpdate(timeString)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
        }, 0, 6000)

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        timer = null
    }

    override fun onItemClicked(position: Int) {
        if(activity!! is CreateLobbyActivity){
            if(checkIfPlayer(friendsList[position])){
                Toast.makeText(activity, "${friendsList[position].name} is already in the lobby", Toast.LENGTH_SHORT).show()
            }else{
                if (lastSeenMap.contains(friendsList[position].userId)){
                    val timePassed = Duration.between(lastSeenMap[friendsList[position].userId], LocalTime.now()).seconds
                    if(timePassed <= 9){
                        Toast.makeText(activity,
                            "You have already invited this friend. Wait ${10 - timePassed} more seconds to invite again"
                            , Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                Toast.makeText(activity, "inviting ${friendsList[position].name}", Toast.LENGTH_SHORT).show()
                lastSeenMap[friendsList[position].userId] = LocalTime.now()
            }
        }
    }

    private fun checkIfPlayer(friendData: FriendRViewData) : Boolean {
        val myCreateLobbyActivity = activity!! as CreateLobbyActivity
        return myCreateLobbyActivity.playersMap.keys.contains(friendData.userId)
    }
}