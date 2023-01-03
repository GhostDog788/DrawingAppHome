package il.ghostdog.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener { onRegisterClicked() }
    }

    private fun onRegisterClicked() {
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        if(etEmail.text.isEmpty() || etPassword.text.isEmpty())
        {
            Toast.makeText(this, "Password or Email fields are empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (etPassword.text.length < 6)
        {
            Toast.makeText(this, "Password is shorter than required", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth!!.createUserWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this, MainMenuActivity::class.java))
                        finish()
                    }else{
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                }
    }
}