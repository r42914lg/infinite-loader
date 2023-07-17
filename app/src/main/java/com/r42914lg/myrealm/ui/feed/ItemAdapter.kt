package com.r42914lg.myrealm.ui.feed

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.r42914lg.myrealm.R
import com.r42914lg.myrealm.domain.Item

class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    private val differCallback = object : DiffUtil.ItemCallback<Item>(){
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return  oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this,differCallback)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idTv: TextView = view.findViewById(R.id.item_id)
        val prop1Tv: TextView = view.findViewById(R.id.prop1)
        val prop2Tv: TextView = view.findViewById(R.id.prop2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.idTv.text = differ.currentList[position].id.toString()
        holder.prop1Tv.text = differ.currentList[position].prop1
        holder.prop2Tv.text = differ.currentList[position].prop2
    }

    override fun getItemCount(): Int =
        differ.currentList.size
}
