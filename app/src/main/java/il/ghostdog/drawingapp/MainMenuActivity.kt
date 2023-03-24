package il.ghostdog.drawingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainMenuActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private val homeFragment = HomeFragment()
    private val accountFragment = AccountFragment()
    private val friendsFragment = FriendsFragment()
    private lateinit var currentFragment: Fragment

    //home fragment
    var checkedPastLobbies = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        if(Constants.lastSeenServiceIntent == null){
            Constants.lastSeenServiceIntent = Intent(this, LastSeenService::class.java)
            Constants.lastSeenServiceIntent!!.putExtra("userId", FirebaseAuth.getInstance().currentUser!!.uid)
            startService(Constants.lastSeenServiceIntent)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_home
        supportFragmentManager.beginTransaction().apply {
            add(R.id.flFragment, homeFragment)
            add(R.id.flFragment, accountFragment)
            add(R.id.flFragment, friendsFragment)
            hide(accountFragment)
            hide(friendsFragment)
            commit()
        }
        currentFragment = homeFragment
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val transaction = supportFragmentManager.beginTransaction()
        when (item.itemId) {
            R.id.action_home -> {
                if (currentFragment == accountFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                } else if (currentFragment == friendsFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                transaction.hide(currentFragment)
                transaction.show(homeFragment)
                transaction.commit()
                currentFragment = homeFragment
            }
            R.id.action_account -> {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                transaction.hide(currentFragment)
                transaction.show(accountFragment)
                transaction.commit()
                currentFragment = accountFragment
            }
            R.id.action_friends -> {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                transaction.hide(currentFragment)
                transaction.show(friendsFragment)
                transaction.commit()
                currentFragment = friendsFragment
            }
        }
        return true
    }
}