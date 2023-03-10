package il.ghostdog.drawingapp

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.InputStream

class AccountFragment : Fragment(R.layout.fragment_account) {
    private lateinit var photoMakerFragment: PhotoMakerFragment
    private lateinit var etNickName: EditText
    private var customProgressDialog: Dialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoMakerFragment = childFragmentManager.findFragmentById(R.id.my_fragment) as PhotoMakerFragment
        val btnApplyChanges: Button = view.findViewById(R.id.btnApplyChanges)
        btnApplyChanges.setOnClickListener{ checkChanges()}

        etNickName = view.findViewById(R.id.etNickname)
        updateUIFromDB()
    }

    private fun updateUIFromDB()  = CoroutineScope(Dispatchers.IO).launch{
        try {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            val dbUserReference = FirebaseDatabase.getInstance().getReference("users").child(userId)
            dbUserReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.getValue(UserData::class.java) ?: return
                    etNickName.setText(userData.nickname)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
            val reference = FirebaseStorage.getInstance().getReference("UsersData")
                .child(userId).child("profilePic")

            val maxDownloadSize = 5L * 1024 * 1024
            val bytes = reference.getBytes(maxDownloadSize).await()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0 , bytes.size)
            withContext(Dispatchers.Main){
                photoMakerFragment.getBackgroundImageView().setImageBitmap(bitmap)
            }
        }
        catch (e : Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(activity!!, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkChanges() {
        if(etNickName.text.length !in 2..15){
            Toast.makeText(activity!!, "Name size is too long or too short", Toast.LENGTH_SHORT).show()
            return
        }
        applyChanges(etNickName.text.toString())
    }

    private fun applyChanges(nickName: String) {
        showProgressDialog()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val dbUserReference = FirebaseDatabase.getInstance().getReference("users").child(userId)
        dbUserReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(UserData::class.java) ?: return
                userData.nickname = nickName


                dbUserReference.setValue(userData).addOnCompleteListener{
                    if(it.isSuccessful){
                        val data = SerializationHelper.compressBitmap(photoMakerFragment.getBitmapFromView())
                        val reference = FirebaseStorage.getInstance().getReference("UsersData")
                            .child(userId).child("profilePic")
                        reference.putBytes(data).addOnCompleteListener{ it2 ->
                            if (it2.isSuccessful){
                                Toast.makeText(activity!!, "Data updated successfully", Toast.LENGTH_SHORT).show()
                            }else{
                                Toast.makeText(activity!!, "Photo could not be updated please try again", Toast.LENGTH_SHORT).show()
                            }
                            cancelProgressDialog()
                        }
                    }else{
                        Toast.makeText(activity!!, "Data could not be updated please try again", Toast.LENGTH_SHORT).show()
                        cancelProgressDialog()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun showProgressDialog(){
        customProgressDialog = Dialog(activity!!)
        customProgressDialog?.setCancelable(false)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
}