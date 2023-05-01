package il.ghostdog.drawingapp


import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment

class RegisterProfilePicFragment : Fragment(R.layout.fragment_register_profile_pic) {
    lateinit var photoMakerFragment: PhotoMakerFragment

    val mOnPhotoMakerCreated : Event<Unit> = Event()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoMakerFragment = childFragmentManager.findFragmentById(R.id.my_fragment) as PhotoMakerFragment
        mOnPhotoMakerCreated.invoke(Unit)
    }
}