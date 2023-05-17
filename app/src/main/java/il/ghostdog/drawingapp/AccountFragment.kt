package il.ghostdog.drawingapp

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class AccountFragment : Fragment(R.layout.fragment_account), IProgressDialogUser, IAudioUser {
    private lateinit var photoMakerFragment: PhotoMakerFragment
    private lateinit var etNickName: EditText
    override var customProgressDialog: Dialog? = null
    override lateinit var soundPool: SoundPool
    override var clickSoundId: Int = -1
    override var errorSoundId: Int = -1
    override var softClickSoundId: Int = -1


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoMakerFragment = childFragmentManager.findFragmentById(R.id.my_fragment) as PhotoMakerFragment
        val btnApplyChanges: Button = view.findViewById(R.id.btnApplyChanges)
        btnApplyChanges.setOnClickListener{ checkChanges()}

        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)
        btnSignOut.setOnClickListener{ onSignOut()}

        val btnCopyMyFriendCode = view.findViewById<Button>(R.id.btnCopyMyCode)
        btnCopyMyFriendCode.setOnClickListener{ copyFriendCode()}

        etNickName = view.findViewById(R.id.etNickname)
        updateUIFromDB()

        //create sound player
        setUpSoundPool(context!!)
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
            soundPool.play(errorSoundId, 1F, 1F,0,0, 1F)
            return
        }
        soundPool.play(clickSoundId, 1F, 1F,0,0, 1F)
        applyChanges(etNickName.text.toString())
    }

    private fun applyChanges(nickName: String) {
        showProgressDialog(activity!!)
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

    private fun copyFriendCode() {
        soundPool.play(clickSoundId, 1F, 1F,0,0, 1F)
        val myFriendCode = FirebaseAuth.getInstance().currentUser!!.uid
        val clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("label", "My friend code is: $myFriendCode")
        clipboardManager?.setPrimaryClip(clip)
        Toast.makeText(activity, "Friend code copied", Toast.LENGTH_SHORT).show()
    }

    private fun onSignOut(){
        soundPool.play(clickSoundId, 1F, 1F,0,0, 1F)
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(activity, LoginActivity::class.java))
        activity!!.finish()
    }
}