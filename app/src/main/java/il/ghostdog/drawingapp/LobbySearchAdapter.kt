package il.ghostdog.drawingapp

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class LobbySearchAdapter(private var data: ArrayList<LobbySearchRViewData>, private var listener: RecyclerViewEvent) : RecyclerView.Adapter<LobbySearchAdapter.ItemViewHolder>() {
    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view), View.OnClickListener{
        val leaderName = view.findViewById<TextView>(R.id.tvLeaderName)
        val playersCount = view.findViewById<TextView>(R.id.tvPlayersCount)
        val gameStatus = view.findViewById<ImageView>(R.id.ivGameStatus)
        val currntRound = view.findViewById<TextView>(R.id.tvCurrentRounds)
        val rounds = view.findViewById<TextView>(R.id.tvRounds)
        val roundsContainer = view.findViewById<LinearLayout>(R.id.llRounds)

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if(position != RecyclerView.NO_POSITION){
                listener.onItemClicked(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbySearchAdapter.ItemViewHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_lobby_search, parent, false)

        return  ItemViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: LobbySearchAdapter.ItemViewHolder, position: Int) {
        val lobbySearchRViewData = data[position]

        holder.leaderName.text = lobbySearchRViewData.leaderName
        holder.playersCount.text = lobbySearchRViewData.playersCount.toString()
        when(lobbySearchRViewData.status){
            GameStatus.active -> {
                holder.gameStatus.setImageResource(R.drawable.green_circle)
                holder.currntRound.text = lobbySearchRViewData.currentRound.toString()
                holder.rounds.text = lobbySearchRViewData.rounds.toString()
                holder.roundsContainer.visibility = View.VISIBLE
            }
            else -> {
                holder.gameStatus.setImageResource(R.drawable.gray_circle)
                holder.roundsContainer.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface RecyclerViewEvent{
        fun onItemClicked(position: Int)
    }
}