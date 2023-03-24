package il.ghostdog.drawingapp

import android.graphics.BitmapFactory
import android.opengl.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.OnProgressListener
import kotlinx.coroutines.*
import kotlinx.coroutines.android.awaitFrame
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class FriendsRecyclerAdapter(private var data: ArrayList<FriendRViewData>, private var listener: RecyclerViewEvent) : RecyclerView.Adapter<FriendsRecyclerAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener {
        val name: TextView = view.findViewById(R.id.tvName)
        val profilePic: ImageView = view.findViewById(R.id.ivProfilePic)
        val progressText: TextView = view.findViewById(R.id.tvProgress)
        val loadingBar: ProgressBar = view.findViewById(R.id.pbLoading)
        val llInactive: LinearLayout = view.findViewById(R.id.llInactive)
        val tvLastSeen: TextView = view.findViewById(R.id.tvLastSeen)
        val ivActive: ImageView = view.findViewById(R.id.ivActive)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION){
                listener.onItemClicked(position)
            }
        }
        fun calculateTimeDifAndUpdate(timeString: String){
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val dateTimeLastLeader = LocalDateTime.parse(timeString, formatter)
            GlobalScope.launch(Dispatchers.Default){
                val date = getCurrentTimeFromFirebase()
                val israelZone: ZoneId = ZoneId.of("Asia/Jerusalem")
                val localDateTime = LocalDateTime.ofInstant(date.toInstant(), israelZone)
                val difference = (localDateTime.toEpochSecond(ZoneOffset.UTC) - dateTimeLastLeader.toEpochSecond(
                    ZoneOffset.UTC))
                withContext(Dispatchers.Main){
                    if(difference > 90){
                        //not active
                        ivActive.visibility = View.GONE
                        llInactive.visibility = View.VISIBLE
                        val finalString = if((difference / (60*60*24)).toInt() > 0){
                            //days
                            "${(difference / (60*60*24)).toInt()} days"
                        }else if((difference / (60*60)).toInt() > 0){
                            //hours
                            "${(difference / (60*60)).toInt()} hours"
                        }else{
                            //minutes
                            "${(difference / 60).toInt()} minutes"
                        }
                        tvLastSeen.text = finalString
                    }else{
                        //active
                        ivActive.visibility = View.VISIBLE
                        llInactive.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_raw_friend, parent, false)

        return  ItemViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val friendRViewData: FriendRViewData = data[position]

        holder.name.text = friendRViewData.name
        if(friendRViewData.lastSeen != null){
            holder.calculateTimeDifAndUpdate(friendRViewData.lastSeen!!)
        }
        if(friendRViewData.profilePic == null){
            holder.profilePic.setImageResource(0)
            getProfilePicAndUpdateView(friendRViewData.userId, holder)
        }else{
            holder.profilePic.setImageBitmap(friendRViewData.profilePic)
        }
    }
    private fun getProfilePicAndUpdateView(userId: String, holder: ItemViewHolder) = CoroutineScope(Dispatchers.IO).launch{
        holder.profilePic.visibility = View.GONE
        holder.loadingBar.visibility = View.VISIBLE
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
    private suspend fun getCurrentTimeFromFirebase() : Date {
        var date: Date? = null

        val database = FirebaseDatabase.getInstance().reference
        database.child(".info/serverTimeOffset").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val offset = dataSnapshot.getValue(Long::class.java) ?: 0L
                val estimatedServerTimeMs = System.currentTimeMillis() + offset
                val currentTime = Date(estimatedServerTimeMs)
                // use the current time here
                date = currentTime
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
        withContext(Dispatchers.IO){
            while(date == null)
            {
                awaitFrame()
            }
        }
        return date!!
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface RecyclerViewEvent{
        fun onItemClicked(position: Int)
    }
}