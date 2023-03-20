package il.ghostdog.drawingapp

import android.graphics.BitmapFactory
import android.opengl.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.OnProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

class FriendRequestsRecyclerAdapter(private var data: ArrayList<FriendRequestRViewData>, private var listener: RecyclerViewEvent) : RecyclerView.Adapter<FriendRequestsRecyclerAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val profilePic: ImageView = view.findViewById(R.id.ivProfilePic)
        val progressText: TextView = view.findViewById(R.id.tvProgress)
        val loadingBar: ProgressBar = view.findViewById(R.id.pbLoading)
        private val btnApprove: ImageButton = view.findViewById(R.id.btnApprove)
        private val btnDecline: ImageButton = view.findViewById(R.id.btnDecline)

        init {
            btnApprove.setOnClickListener{
                val position = adapterPosition
                if(position != RecyclerView.NO_POSITION){
                    listener.onApprovedClicked(position)
                }
            }
            btnDecline.setOnClickListener{
                val position = adapterPosition
                if(position != RecyclerView.NO_POSITION){
                    listener.onDeclinedClicked(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_raw_friend_request, parent, false)

        return  ItemViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val requestRViewData: FriendRequestRViewData = data[position]

        holder.name.text = requestRViewData.name
        if(requestRViewData.profilePic == null){
            holder.profilePic.setImageResource(0)
            getProfilePicAndUpdateView(requestRViewData.userId, holder)
        }else{
            holder.profilePic.setImageBitmap(requestRViewData.profilePic)
        }
    }
    private fun getProfilePicAndUpdateView(userId: String, holder: ItemViewHolder) = CoroutineScope(Dispatchers.IO).launch{
        withContext(Dispatchers.Main) {
            holder.loadingBar.visibility = View.VISIBLE
        }
        try {
            val reference = FirebaseStorage.getInstance().getReference("UsersData")
                .child(userId).child("profilePic")
            val localFile = File.createTempFile("image", "jpg")
            val task = reference.getFile(localFile)
            task.addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                holder.profilePic.setImageBitmap(bitmap)
                holder.loadingBar.visibility = View.GONE
            }.addOnFailureListener {
                //Handle failed download
            }.addOnProgressListener { taskSnapshot ->
                /*println(taskSnapshot.bytesTransferred)
                val progress =
                    (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                holder.progressText.text = "$progress%"
                holder.loadingBar.progress = progress*/
            }
        }catch (e : Exception){
            //Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface RecyclerViewEvent{
        fun onApprovedClicked(position: Int)
        fun onDeclinedClicked(position: Int)
    }
}