package il.ghostdog.drawingapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.findFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AccountFragment : Fragment(R.layout.fragment_account) {
    private lateinit var photoMakerFragment: PhotoMakerFragment
    private lateinit var etNickName: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoMakerFragment = childFragmentManager.findFragmentById(R.id.my_fragment) as PhotoMakerFragment
        val btnApplyChanges: Button = view.findViewById(R.id.btnApplyChanges)
        btnApplyChanges.setOnClickListener{ checkChanges()}

        etNickName = view.findViewById(R.id.etNickname)
        updateUIFromDB()
    }

    private fun updateUIFromDB() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val dbUserReference = FirebaseDatabase.getInstance().getReference("users").child(userId)
        dbUserReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(UserData::class.java) ?: return
                etNickName.setText(userData.nickname)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkChanges() {
        if(etNickName.text.length !in 2..15){
            Toast.makeText(activity!!, "Name size is too long or too short", Toast.LENGTH_SHORT).show()
            return
        }

        applyChanges(etNickName.text.toString())
    }

    private fun applyChanges(nickName: String) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val dbUserReference = FirebaseDatabase.getInstance().getReference("users").child(userId)
        dbUserReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(UserData::class.java) ?: return
                userData.nickname = nickName
                
                
                dbUserReference.setValue(userData).addOnCompleteListener{
                    if(it.isSuccessful){
                        Toast.makeText(activity!!, "Data updated successfully", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(activity!!, "Data could not be updated please try again", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}