package il.ghostdog.drawingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlayerRecyclerAdapter(private var data: ArrayList<PlayerRViewData>, private var listener: RecyclerViewEvent) : RecyclerView.Adapter<PlayerRecyclerAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnLongClickListener {
        val name: TextView = view.findViewById(R.id.tvName)
        val profilePic: ImageView = view.findViewById(R.id.ivProfilePic)
        val leaderIcon: ImageView = view.findViewById(R.id.ivLeaderIcon)

        init {
            view.setOnLongClickListener(this)
        }

        override fun onLongClick(p0: View?): Boolean {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION){
                listener.onItemClicked(position)
            }
            return true
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
        }else{
            holder.profilePic.setImageBitmap(playerRecycleView.profilePic)
        }
        if(playerRecycleView.isLeader){
            holder.leaderIcon.visibility = View.VISIBLE
        }else{
            holder.leaderIcon.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface RecyclerViewEvent{
        fun onItemClicked(position: Int)
    }
}