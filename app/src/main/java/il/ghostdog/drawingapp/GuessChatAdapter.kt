package il.ghostdog.drawingapp

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class GuessChatAdapter(private var data: ArrayList<GuessMessageRData>) : RecyclerView.Adapter<GuessChatAdapter.ItemViewHolder>(){
    inner class ItemViewHolder(view: View): RecyclerView.ViewHolder(view){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }
}