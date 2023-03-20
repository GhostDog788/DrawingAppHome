package il.ghostdog.drawingapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MyFriendsFragment : Fragment(R.layout.fragment_my_friends), FriendsRecyclerAdapter.RecyclerViewEvent {
    private lateinit var rvFriends: RecyclerView
    private var friendsRDataList : ArrayList<FriendRViewData> = ArrayList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFriends = view.findViewById(R.id.rvFriends)
        rvFriends.adapter = FriendsRecyclerAdapter(friendsRDataList, this)
        rvFriends.layoutManager = LinearLayoutManager(activity!!)
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

    override fun onItemClicked(position: Int) {
        Toast.makeText(activity!!, "Friend Clicked", Toast.LENGTH_SHORT).show()
    }
}