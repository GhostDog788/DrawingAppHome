package il.ghostdog.drawingapp

import android.graphics.BitmapFactory
import android.opengl.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class PlayerRecyclerAdapter(private var data: ArrayList<PlayerRViewData>, private var listener: RecyclerViewEvent) : RecyclerView.Adapter<PlayerRecyclerAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnLongClickListener, View.OnClickListener {
        val name: TextView = view.findViewById(R.id.tvName)
        val profilePic: ImageView = view.findViewById(R.id.ivProfilePic)
        val leaderIcon: ImageView = view.findViewById(R.id.ivLeaderIcon)
        val progressText: TextView = view.findViewById(R.id.tvProgress)
        val loadingBar: ProgressBar = view.findViewById(R.id.pbLoading)

        init {
            view.setOnLongClickListener(this)
            view.setOnClickListener(this)
        }

        override fun onLongClick(p0: View?): Boolean {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION){
                listener.onItemClickedLong(position)
            }
            return true
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION){
                listener.onItemClickedShort(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_raw_player, parent, false)

        return  ItemViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val playerRecycleView: PlayerRViewData = data[position]

        holder.name.text = playerRecycleView.name
        if(playerRecycleView.profilePic == null){
            holder.profilePic.setImageResource(0)
            getProfilePicAndUpdateView(playerRecycleView.userId, holder)
        }else{
            holder.profilePic.setImageBitmap(playerRecycleView.profilePic)
        }
        if(playerRecycleView.isLeader){
            holder.leaderIcon.visibility = View.VISIBLE
        }else{
            holder.leaderIcon.visibility = View.INVISIBLE
        }
    }
    private fun getProfilePicAndUpdateView(userId: String, holder: ItemViewHolder) = CoroutineScope(Dispatchers.IO).launch{
        withContext(Dispatchers.Main) {
            holder.profilePic.visibility = View.GONE
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
                holder.profilePic.visibility = View.VISIBLE
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
        fun onItemClickedShort(position: Int)
        fun onItemClickedLong(position: Int)
    }
}