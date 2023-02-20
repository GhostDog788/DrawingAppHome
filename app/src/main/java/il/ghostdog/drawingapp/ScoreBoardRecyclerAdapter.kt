package il.ghostdog.drawingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class ScoreBoardRecyclerAdapter(private var data: ArrayList<ScoreBoardRViewData>) : RecyclerView.Adapter<ScoreBoardRecyclerAdapter.ItemViewHolder>() {
    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view){
        val name: TextView = view.findViewById(R.id.tvName)
        val points: TextView = view.findViewById(R.id.tvPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreBoardRecyclerAdapter.ItemViewHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.score_board_item, parent, false)

        return  ItemViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ScoreBoardRecyclerAdapter.ItemViewHolder, position: Int) {
        val scoreBoardData : ScoreBoardRViewData = data[position]

        holder.name.text = scoreBoardData.name
        holder.points.text = scoreBoardData.points.toString()
        when (position) {
            0 -> {
                holder.itemView.findViewById<LinearLayout>(R.id.clMain).setBackgroundResource(R.drawable.score_board_gold)
            }
            1 -> {
                holder.itemView.findViewById<LinearLayout>(R.id.clMain).setBackgroundResource(R.drawable.score_board_silver)
            }
            2 -> {
                holder.itemView.findViewById<LinearLayout>(R.id.clMain).setBackgroundResource(R.drawable.score_board_bronze)
            }
            else -> {
                holder.itemView.findViewById<LinearLayout>(R.id.clMain).setBackgroundResource(R.drawable.score_board_normal)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}