package il.ghostdog.drawingapp

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList


class MyFriendsFragment : Fragment(R.layout.fragment_my_friends), FriendsRecyclerAdapter.RecyclerViewEvent {
    private lateinit var rvFriends: RecyclerView
    private var friendsRDataList : ArrayList<FriendRViewData> = ArrayList()
    private var timer: Timer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFriends = view.findViewById(R.id.rvFriends)
        rvFriends.adapter = FriendsRecyclerAdapter(friendsRDataList, this)
        rvFriends.layoutManager = LinearLayoutManager(activity!!)

        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                for (i in 0 until rvFriends.childCount) {
                    val viewHolder = rvFriends.findViewHolderForAdapterPosition(i) as FriendsRecyclerAdapter.ItemViewHolder
                    val myFriendData = friendsRDataList[i]
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(myFriendData.userId).child("lastSeen")
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val timeString = snapshot.getValue(String::class.java)!!
                                viewHolder.calculateTimeDifAndUpdate(timeString)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
        }, 0, 6000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        timer = null
    }

    fun addRView(friendRViewData: FriendRViewData){
        friendsRDataList.add(friendRViewData)
        rvFriends.adapter!!.notifyItemInserted(friendsRDataList.size)
    }
    fun updateRView(friendRViewData: FriendRViewData){
        val tempRView = friendsRDataList.find { v -> v.userId == friendRViewData.userId }
        val position = friendsRDataList.indexOf(tempRView)
        if(position < 0){
            addRView(friendRViewData)
            return
        }
        friendsRDataList[position] = friendRViewData
        rvFriends.adapter!!.notifyItemChanged(position)
    }
    fun removeRView(friendRViewData: FriendRViewData){
        val tempRView = friendsRDataList.find { v -> v.userId == friendRViewData.userId }
        val position = friendsRDataList.indexOf(tempRView)
        friendsRDataList.remove(tempRView)
        rvFriends.adapter!!.notifyItemRemoved(position)
    }

    override fun onItemClicked(position: Int) {
        val popupMenu = PopupMenu(activity!!, rvFriends.findViewHolderForAdapterPosition(position)!!.itemView)
        popupMenu.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.action_remove_friend ->{
                    val ref = FirebaseDatabase.getInstance().getReference("users")
                    ref.child(FirebaseAuth.getInstance().currentUser!!.uid).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val userData = snapshot.getValue(UserData::class.java)
                            userData!!.friendsList.remove(friendsRDataList[position].userId)
                            ref.child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(userData)
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                    ref.child(friendsRDataList[position].userId).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val userData = snapshot.getValue(UserData::class.java)
                            userData!!.friendsList.remove(FirebaseAuth.getInstance().currentUser!!.uid)
                            ref.child(friendsRDataList[position].userId).setValue(userData)
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                    true
                }else ->{
                false
            }
            }
        }
        popupMenu.inflate(R.menu.popup_friend_menu)
        popupMenu.show()
    }
}