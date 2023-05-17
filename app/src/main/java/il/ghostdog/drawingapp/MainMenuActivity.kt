package il.ghostdog.drawingapp


import android.app.AlertDialog
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainMenuActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private val homeFragment = HomeFragment()
    private val accountFragment = AccountFragment()
    private val friendsFragment = FriendsFragment()
    private lateinit var currentFragment: Fragment

    //home fragment
    var checkedPastLobbies = false

    private var musicPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        FirebaseDatabase.getInstance().getReference("users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).child("activeGame").removeValue()

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
        friendsFragment.friendRequestsFragment.rvMain = bottomNavigationView

        musicPlayer = MediaPlayer.create(this, R.raw.awesomeness)
        musicPlayer!!.isLooping = true
        musicPlayer!!.setVolume(0.5f,0.5f)

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
    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit game")
        builder.setMessage("Do you want to exit the game?")
        builder.setPositiveButton("Yes") { dialog, which ->
            moveTaskToBack(true)
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        musicPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        musicPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer?.release()
        Toast.makeText(applicationContext, "Main Menu Destroyed", Toast.LENGTH_SHORT).show()
    }
}