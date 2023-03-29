package il.ghostdog.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class  LoginActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        setContentView(R.layout.activity_login)
        //can start code

        val btnSignIn = findViewById<Button>(R.id.btnLogin)
        btnSignIn.setOnClickListener { onSignInClicked() }

        val tvRegisterButton = findViewById<TextView>(R.id.tvRegisterButton)
        tvRegisterButton.setOnClickListener { onRegisterButtonClicked() }
    }

    private fun onSignInClicked() {
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        if(etEmail.text.isEmpty() || etPassword.text.isEmpty())
        {
            Toast.makeText(this, "Password or Email fields are empty", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth!!.signInWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this, LaunchActivity::class.java))
                        finish()
                    }else{
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    }
                }

    }

    private fun onRegisterButtonClicked() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}