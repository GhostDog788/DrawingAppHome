package il.ghostdog.drawingapp

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class PlayerGameHUDAdapter(private var data: ArrayList<PlayerRGameViewData>, private var listener: RecyclerViewEvent) : RecyclerView.Adapter<PlayerGameHUDAdapter.ItemViewHolder>() {
    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnLongClickListener{
        val name: TextView = view.findViewById(R.id.tvName)
        val points: TextView = view.findViewById(R.id.tvPoints)
        val drawerIcon: ImageView = view.findViewById(R.id.ivDrawerIcon)

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerGameHUDAdapter.ItemViewHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_game_player, parent, false)

        return  ItemViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: PlayerGameHUDAdapter.ItemViewHolder, position: Int) {
        val playerGameData: PlayerRGameViewData = data[position]

        holder.name.text = playerGameData.name
        holder.points.text = playerGameData.points.toString()
        if(playerGameData.isDrawer){
            holder.drawerIcon.visibility = View.VISIBLE
            holder.itemView.findViewById<ConstraintLayout>(R.id.clMain).setBackgroundResource(R.drawable.primary_variant_color_rec)
            return
        }else{
            holder.drawerIcon.visibility = View.INVISIBLE
        }
        if(playerGameData.answeredCorrectly){
            holder.itemView.findViewById<ConstraintLayout>(R.id.clMain).setBackgroundResource(R.drawable.player_game_card_correct)
        }else{
            holder.itemView.findViewById<ConstraintLayout>(R.id.clMain).setBackgroundResource(R.drawable.primary_color_rec)

        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface RecyclerViewEvent{
        fun onItemClicked(position: Int)
    }
}