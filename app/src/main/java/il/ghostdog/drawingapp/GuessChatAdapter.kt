package il.ghostdog.drawingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GuessChatAdapter(private var data: ArrayList<GuessMessageRData>) : RecyclerView.Adapter<GuessChatAdapter.ItemViewHolder>(){
    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view){
        val name: TextView = view.findViewById(R.id.tvName)
        val guess: TextView = view.findViewById(R.id.tvGuess)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycle_guess_message, parent, false)

        return  ItemViewHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val messageRData: GuessMessageRData = data[position]

        holder.name.text = messageRData.name
        holder.guess.text = messageRData.guess
    }

    override fun getItemCount(): Int {
        return data.size
    }
}