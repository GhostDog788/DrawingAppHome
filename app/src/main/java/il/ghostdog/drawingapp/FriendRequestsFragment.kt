package il.ghostdog.drawingapp

import android.os.Bundle
import android.view.View
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


class FriendRequestsFragment : Fragment(R.layout.fragment_friend_requests), FriendRequestsRecyclerAdapter.RecyclerViewEvent {
    private lateinit var rvRequests: RecyclerView
    private var requestsRDataList : ArrayList<FriendRequestRViewData> = ArrayList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvRequests = view.findViewById(R.id.rvRequests)
        rvRequests.adapter = FriendRequestsRecyclerAdapter(requestsRDataList, this)
        rvRequests.layoutManager = GridLayoutManager(activity!!, 2)
    }
    fun updateRView(friendRequestRViewData: FriendRequestRViewData){
        val tempRView = requestsRDataList.find { v -> v.userId == friendRequestRViewData.userId }
        val position = requestsRDataList.indexOf(tempRView)
        requestsRDataList[position] = friendRequestRViewData
        rvRequests.adapter!!.notifyItemChanged(position)
    }
    fun updateRView(requestsRDataList : ArrayList<FriendRequestRViewData>){
        this.requestsRDataList = requestsRDataList
        rvRequests.adapter!!.notifyDataSetChanged()
    }
    fun addRView(friendRequestRViewData: FriendRequestRViewData){
        requestsRDataList.add(friendRequestRViewData)
        rvRequests.adapter!!.notifyItemInserted(requestsRDataList.size)
    }

    override fun onApprovedClicked(position: Int) {
        val requestRViewData = requestsRDataList[position]
        val deleteUid = "request-" + requestRViewData.userId
        val myUid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("users")
            .child(myUid).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val myUserData = snapshot.getValue(UserData::class.java)!!
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(requestRViewData.userId).addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val theirUserData = snapshot.getValue(UserData::class.java)!!
                                myUserData.friendsList.remove(deleteUid)
                                myUserData.friendsList.add(requestRViewData.userId)
                                FirebaseDatabase.getInstance().getReference("users")
                                    .child(myUid).setValue(myUserData)
                                theirUserData.friendsList.add(myUid)
                                FirebaseDatabase.getInstance().getReference("users")
                                    .child(requestRViewData.userId).setValue(theirUserData)
                                requestsRDataList.removeAt(position)
                                rvRequests.adapter!!.notifyItemRemoved(position)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDeclinedClicked(position: Int) {
        val requestRViewData = requestsRDataList[position]
        val deleteUid = "request-" + requestRViewData.userId
        val myUid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("users")
            .child(myUid).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.getValue(UserData::class.java)!!
                    userData.friendsList.remove(deleteUid)
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(myUid).setValue(userData)
                    requestsRDataList.removeAt(position)
                    rvRequests.adapter!!.notifyItemRemoved(position)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}