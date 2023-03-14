package il.ghostdog.drawingapp

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class RegisterActivity : AppCompatActivity(), IProgressDialogUser {

    private var mAuth: FirebaseAuth? = null

    private lateinit var userTextFieldsFragment: UserTextFieldsFragment
    private lateinit var photoMakerFragment: PhotoMakerFragment
    private lateinit var currentFragment: Fragment
    override var customProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()

        userTextFieldsFragment = UserTextFieldsFragment()
        photoMakerFragment = PhotoMakerFragment()

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener { onRegisterClicked() }

        supportFragmentManager.beginTransaction()
            .add(R.id.flFragmentContainer, photoMakerFragment)
            .hide(photoMakerFragment)
            .add(R.id.flFragmentContainer, userTextFieldsFragment)
            .commit()
        currentFragment = userTextFieldsFragment

        val btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener{
            if (currentFragment == userTextFieldsFragment) {
                supportFragmentManager.beginTransaction()
                    .hide(userTextFieldsFragment)
                    .show(photoMakerFragment)
                    .commit()
                currentFragment = photoMakerFragment
            } else {
                supportFragmentManager.beginTransaction()
                    .hide(photoMakerFragment)
                    .show(userTextFieldsFragment)
                    .commit()
                currentFragment = userTextFieldsFragment
            }
        }
    }

    private fun onRegisterClicked() {
        val etNickname = userTextFieldsFragment.view!!.findViewById<EditText>(R.id.etNickname)
        val etEmail = userTextFieldsFragment.view!!.findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        if(etEmail.text.isEmpty() || etPassword.text.isEmpty() || etNickname.text.isEmpty())
        {
            Toast.makeText(this, "Nickname, Password or Email fields are empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (etPassword.text.length < 6)
        {
            Toast.makeText(this, "Password is shorter than required", Toast.LENGTH_SHORT).show()
            return
        }

        if(etNickname.text.length > 16){
            Toast.makeText(this, "Nickname is too long", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog(this@RegisterActivity)
        mAuth!!.createUserWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val databaseUsers = FirebaseDatabase.getInstance().getReference("users")
                        databaseUsers.child(mAuth!!.currentUser!!.uid).setValue(
                            UserData(etNickname.text.toString(), etEmail.text.toString())
                        )
                        val data = SerializationHelper.compressBitmap(photoMakerFragment.getBitmapFromView())
                        val reference = FirebaseStorage.getInstance().getReference("UsersData")
                            .child(mAuth!!.currentUser!!.uid).child("profilePic")
                        reference.putBytes(data).addOnCompleteListener{
                            cancelProgressDialog()
                            Toast.makeText(this, it.isSuccessful.toString(), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainMenuActivity::class.java))
                            finish()
                        }
                    }else{
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                }
    }
}