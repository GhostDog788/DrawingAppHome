package il.ghostdog.drawingapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class MainMenuActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private val homeFragment = HomeFragment()
    private val accountFragment = AccountFragment()
    private lateinit var currentFragment: Fragment

    //home fragment
    var checkedPastLobbies = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_home
        supportFragmentManager.beginTransaction().apply {
            add(R.id.flFragment, homeFragment)
            add(R.id.flFragment, accountFragment)
            hide(accountFragment)
            commit()
        }
        currentFragment = homeFragment
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_home -> {
                setCurrentFragment(homeFragment)
            }
            R.id.action_account -> {
                setCurrentFragment(accountFragment)
            }
            R.id.action_shop -> {

            }
        }
        return true
    }
    private fun setCurrentFragment(fragment: Fragment){
        if(fragment == currentFragment) return
        supportFragmentManager.beginTransaction().apply {
            if(fragment is HomeFragment) {
                show(homeFragment)
            }else {
                hide(homeFragment)
            }
            if(fragment is AccountFragment) {
                show(accountFragment)
            }
            else {
                hide(accountFragment)
            }
            currentFragment = fragment
            commit()
        }
    }
}