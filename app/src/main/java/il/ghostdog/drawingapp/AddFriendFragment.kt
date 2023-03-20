package il.ghostdog.drawingapp

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class AddFriendFragment : Fragment(R.layout.fragment_add_friend) {
    private lateinit var etSearch: EditText
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.etIdSearch)
        val btnSearch = view.findViewById<Button>(R.id.btnSearch)
        btnSearch.setOnClickListener{
            if(etSearch.text.length > 6 && etSearch.text.toString() != FirebaseAuth.getInstance().currentUser!!.uid){
            searchForUser(etSearch.text.toString())
            }else{
                Toast.makeText(activity!!, "User id is not correct", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchForUser(testId: String) {
        FirebaseDatabase.getInstance().getReference("users")
                .child(testId).addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                            val userData = snapshot.getValue(UserData::class.java)!!
                            checkIfAllReadyAdded(testId, userData)
                        }else{
                            Toast.makeText(activity!!, "User doesn't exists", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
    }

    private fun checkIfAllReadyAdded(testId: String, userData: UserData){
        val myUid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseDatabase.getInstance().getReference("users")
            .child(testId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val friendList = snapshot.getValue(UserData::class.java)!!.friendsList
                    for (item in friendList){
                        if(item == myUid){
                            Toast.makeText(activity, "This user has all ready been added as a friend", Toast.LENGTH_SHORT).show()
                            return
                        }
                        else if(item.contains(myUid)){
                            Toast.makeText(activity, "You have all ready sent a friend request", Toast.LENGTH_SHORT).show()
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
        val dialog = Dialog(activity!!)
        dialog.setContentView(R.layout.send_friend_request_dialog)
        dialog.setCancelable(true)
        dialog.findViewById<TextView>(R.id.tvName).text = userData.nickname
        dialog.findViewById<Button>(R.id.btnRequest).setOnClickListener{
            userData.friendsList.add("request-${FirebaseAuth.getInstance().currentUser!!.uid}")
            FirebaseDatabase.getInstance().getReference("users").child(userId).setValue(userData)
            Toast.makeText(activity!!, "The request has been sent to ${userData.nickname}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener{
            dialog.dismiss()
        }
        dialog.show()
    }
}