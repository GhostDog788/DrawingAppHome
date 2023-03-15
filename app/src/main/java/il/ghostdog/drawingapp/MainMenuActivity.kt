package il.ghostdog.drawingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainMenuActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private val homeFragment = HomeFragment()
    private val accountFragment = AccountFragment()
    private val shopFragment = ShopFragment()
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
            add(R.id.flFragment, shopFragment)
            hide(accountFragment)
            hide(shopFragment)
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
                } else if (currentFragment == shopFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                transaction.hide(currentFragment)
                transaction.show(homeFragment)
                transaction.commit()
                currentFragment = homeFragment
            }
            R.id.action_account -> {
                if (currentFragment == homeFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                } else if (currentFragment == shopFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                transaction.hide(currentFragment)
                transaction.show(accountFragment)
                transaction.commit()
                currentFragment = accountFragment
            }
            R.id.action_shop -> {
                if (currentFragment == homeFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                } else if (currentFragment == accountFragment) {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                transaction.hide(currentFragment)
                transaction.show(shopFragment)
                transaction.commit()
                currentFragment = shopFragment
            }
        }
        return true
    }
}