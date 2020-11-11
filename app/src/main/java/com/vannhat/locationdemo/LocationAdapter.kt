package com.vannhat.locationdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LocationAdapter(private val listData: MutableList<String>) :
    RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

    fun addToTop(newLocation:String){
        listData.add(0, newLocation)
        notifyItemInserted(0)
    }

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        return LocationViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_location, parent, false)
        )
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.tvLocation.text = listData[position]
    }

    override fun getItemCount(): Int {
        return listData.size
    }

}
