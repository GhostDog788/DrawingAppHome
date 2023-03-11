package il.ghostdog.drawingapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.findFragment

class AccountFragment : Fragment(R.layout.fragment_account) {
    private lateinit var photoMakerFragment: PhotoMakerFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoMakerFragment = childFragmentManager.findFragmentById(R.id.my_fragment) as PhotoMakerFragment
    }
}