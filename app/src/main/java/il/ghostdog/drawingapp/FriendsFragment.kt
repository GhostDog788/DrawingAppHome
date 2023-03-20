package il.ghostdog.drawingapp

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class FriendsFragment : Fragment(R.layout.fragment_friends), BottomNavigationView.OnNavigationItemSelectedListener {
    private val myFriendFragment = MyFriendsFragment()
    private val friendRequestsFragment = FriendRequestsFragment()
    private val addFriendFragment = AddFriendFragment()
    private lateinit var currentFragment: Fragment

    private val friendListListener = object : ChildEventListener{
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val potentialFriend = snapshot.getValue(String::class.java)!!
            if(potentialFriend.startsWith("request-")){
                //request
                val start = potentialFriend.lastIndexOf('-') + 1
                val end = potentialFriend.lastIndex + 1
                val uid = potentialFriend.subSequence(start,end)
                updateRequestToRView(uid.toString(), UpdateAction.Add)
            }else{
                //friend
                updateFriendToRView(potentialFriend, UpdateAction.Add)
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            val potentialFriend = snapshot.getValue(String::class.java)!!
            if(potentialFriend.startsWith("request-")){
                //request
                val start = potentialFriend.lastIndexOf('-') + 1
                val end = potentialFriend.lastIndex + 1
                val uid = potentialFriend.subSequence(start,end)
                updateRequestToRView(uid.toString(), UpdateAction.Update)
            }else{
                //friend
                updateFriendToRView(potentialFriend, UpdateAction.Update)
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val potentialFriend = snapshot.getValue(String::class.java)!!
            if(potentialFriend.startsWith("request-")){
                //request
                val start = potentialFriend.lastIndexOf('-') + 1
                val end = potentialFriend.lastIndex + 1
                val uid = potentialFriend.subSequence(start,end)
                updateRequestToRView(uid.toString(), UpdateAction.Remove)
            }else{
                //friend
                updateFriendToRView(potentialFriend, UpdateAction.Remove)
            }
        }
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigationView = view.findViewById<BottomNavigationView>(R.id.bottom_navigation_friends)
        bottomNavigationView.selectedItemId = R.id.action_friends
        childFragmentManager.beginTransaction().apply {
            add(R.id.flFriendsFragment, friendRequestsFragment)
            add(R.id.flFriendsFragment, addFriendFragment)
            add(R.id.flFriendsFragment, myFriendFragment)
            hide(addFriendFragment)
            hide(friendRequestsFragment)
            commit()
        }
        currentFragment = myFriendFragment
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        addFriendsListListener()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val transaction = childFragmentManager.beginTransaction()
        when (item.itemId) {
            R.id.action_requests -> {
                if (currentFragment == addFriendFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                } else if (currentFragment == myFriendFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                transaction.hide(currentFragment)
                transaction.show(friendRequestsFragment)
                transaction.commit()
                currentFragment = friendRequestsFragment
            }
            R.id.action_add_friend -> {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                transaction.hide(currentFragment)
                transaction.show(addFriendFragment)
                transaction.commit()
                currentFragment = addFriendFragment
            }
            R.id.action_friends -> {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                transaction.hide(currentFragment)
                transaction.show(myFriendFragment)
                transaction.commit()
                currentFragment = myFriendFragment
            }
        }
        return true
    }

    private fun addFriendsListListener() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid).child("friendsList").addChildEventListener(friendListListener)
    }
    private fun removeFriendsListListener() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid).child("friendsList").removeEventListener(friendListListener)
    }

    private fun updateFriendToRView(uid: String, action : UpdateAction) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value == null) return
                    val userData = snapshot.getValue(UserData::class.java)!!
                    val friendRViewData = FriendRViewData(uid, userData, null)
                    when(action){
                        UpdateAction.Add -> myFriendFragment.addRView(friendRViewData)
                        UpdateAction.Update -> myFriendFragment.updateRView(friendRViewData)
                        UpdateAction.Remove -> {}
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateRequestToRView(uid: String, action : UpdateAction) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value == null) return
                    val userData = snapshot.getValue(UserData::class.java)!!
                    val friendRequestRViewData = FriendRequestRViewData(uid, userData, null)
                    when(action){
                        UpdateAction.Add -> friendRequestsFragment.addRView(friendRequestRViewData)
                        UpdateAction.Update -> friendRequestsFragment.updateRView(friendRequestRViewData)
                        UpdateAction.Remove -> {}
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeFriendsListListener()
    }

    private enum class UpdateAction {
        Add,
        Update,
        Remove
    }
}